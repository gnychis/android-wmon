#!/usr/bin/perl

my $debug = 0;
# 0: off
# 1: specific debug
# 2: full debug

#
# find unbalanced hf_ variables: Compare hf_ variable usage with the hf_ variables
#  declared in the hf_register_info array.
#
# Usage: checkhf.pl <file or files>

# $Id: checkhf.pl 34730 2010-10-31 09:05:11Z jmayer $

#
# Copyright 2005 Joerg Mayer (see AUTHORS file)
#
# Wireshark - Network traffic analyzer
# By Gerald Combs <gerald@wireshark.org>
# Copyright 1998 Gerald Combs
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

#
# Example:
# ~/work/wireshark/trunk/epan/dissectors> ../../tools/checkhf.pl packet-afs.c
# Unused entry: packet-afs.c, hf_afs_ubik_voteend
# Unused entry: packet-afs.c, hf_afs_ubik_errcode
# Unused entry: packet-afs.c, hf_afs_ubik_votetype
# NO ARRAY: packet-afs.c, hf_afs_fs_ipaddr
#
# or checkhf.pl packet-*.c, which will check all the dissector files.
#
# NOTE: This tool currently generates false positives!
#
# The "NO ARRAY" messages - if accurate - points to an error that will
# cause (t|wire)shark to terminate with an assertion when a packet containing
# this particular element is being dissected.
#
# The "Unused entry" message indicates the opposite: We define an entry but
# never use it in a proto_...add... function.
#

use warnings;
use strict;

my $D;

my %elements;
my $element;
my %skip;

my $state;
my $newstate;
# "s_unknown",
# "s_declared",
# "s_used",
# "s_array",
# "s_usedarray",
# "s_error"

my $type;
# "t_declaration";
# "t_usage";
# "t_array";

my $restofline;
my $currfile = "";

my $comment = 0;
my $brace = 0;

my $error = 0;

sub printprevfile {
	my $state;

	foreach $element (keys %elements) {
		$state = $elements{$element};
		$debug>=2 && print "$currfile, $element: PRINT $state\n";
		if ($state eq "s_usedarray") {
			# Everything is fine
		} elsif ($state eq "s_used") {
			# A value is declared and used but the hf_ array
			# entry is missing
			print "ERROR: NO ARRAY: $currfile, $element\n";
			$error = 1;
		} elsif ($state eq "s_array") {
			# A value is declared, has an hf_ array entry
			# but it is never used
			print "Unused entry: $currfile, $element\n";
		} elsif ($state eq "s_declared") {
			# A value is declared but no error value exists.
			# This doesn't matter as it isn't used either.
			print "Declared only entry: $currfile, $element\n"
		} elsif ($state eq "s_unknown") {
			print "UNKNOWN: $currfile, $element\n";
		} else {
			die "Impossible: State $state for $currfile, $element\n";
		}
	}
}

while (<>) {
	if ($currfile !~ /$ARGV/) {
		&printprevfile();
		# New file - reset array and state
		$currfile = $ARGV;
		%elements = ( );
		%skip = ( "hf_register_info" => 1 );
		$state = "s_unknown";
	}
	# opening then closing comment
	if (/(.*?)\/\*.*\*\/(.*)/) {
		$comment = 0;
		$_ = "$1$2";
	# closing then opening comment
	} elsif (/.*?\*\/(.*?)\/\*/) {
		$comment = 1;
		$_ = "$1";
	# opening comment
	} elsif (/(.*?)\/\*/) {
		$comment = 1;
		$_ = "$1";
	# closing comment
	} elsif (/\*\/(.*?)/) {
		$comment = 0;
		$_ = "$1";
	} elsif ($comment == 1) {
		next;
	}
	# unhandled: more than one complete comment per line

	chomp;
	if ($debug) {
		$D = " ($_)";
	} else {
		$D = "";
	}

	# Read input
	if (/static\s+.*int\s+(hf_\w*)\s*=\s*-1\s*;/) {
		$element = $1;
		$debug && print "t_declaration for $element$D\n";
		$type = "t_declaration";
		# ignore: declarations without any use are detected by the compiler
		next;
	# Skip function parameter declarations with hf_ names
	} elsif (/(int\s+?|int\s*?\*\s*?|header_field_info\s+?|header_field_info\s*?\*\s*?|hf_register_info\s+?|hf_register_info\s*?\*\s*?|->\s*?)(hf_\w*)\W(.*)/) {
		$element = $2;
		$restofline = $3;
		$debug && print "Setting skip for $element$D\n";
		$skip{$element} = 1;
		# Handle functions with multiple hf_ parameters
		while ($restofline =~ /(int\s+?|int\s*?\*\s*?|header_field_info\s+?|header_field_info\s*?\*\s*?|hf_register_info\s+?|hf_register_info\s*?\*\s*?|->\s*?)(hf_\w*)\W(.*)/) {
			$element = $2;
			$restofline = $3;
			$debug && print "Setting skip for $element$D\n";
			$skip{$element} = 1;
		}
		next;
	} elsif ($brace == 1 && /^\s*&\s*(hf_\w*)\W+/) {
		$element = $1;
		$debug && print "t_array1 for $element$D\n";
		$type = "t_array";
	} elsif (/^\s*\{\s*?&\s*?(hf_\w*)\W+/) {
		$element = $1;
		$debug && print "t_array2 for $element$D\n";
		$type = "t_array";
	# Order matters: catch all remaining hf_ lines
	} elsif (/\w*\s*=\s*(hf_\w*)\s*;/) {
		# Catches: variable = hf_*
		# But we don't check if the variable actually gets used...
		$element = $1;
		$debug && print "in variable usage for $element$D\n";
		next if ($skip{$element});
		$debug && print "set t_usage for $element$D\n";
		$type = "t_usage";
	} elsif (/\W(hf_\w*)\W*/) {
		$element = $1;
		$debug && print "in usage for $element$D\n";
		next if ($skip{$element});
		$debug && print "set t_usage for $element$D\n";
		$type = "t_usage";
	} else {
		# Line with only a {
		if (/^\s+\{\s*$/) {
			$brace = 1;
		} else {
			$brace = 0;
			$debug && print "else: $D\n";
		}
		next;
	}

	# Get current state
	if (!defined($elements{$element})) {
		$state = "s_unknown";
	} else {
		$state = $elements{$element};
	}

	# current state + input ==> new state
	# we currently ignore t_declaration
	if ($state eq "s_error") {
		$newstate = $state;
	} elsif ($state eq "s_unknown" && $type eq "t_usage") {
			$newstate = "s_used";
	} elsif ($state eq "s_unknown" && $type eq "t_array") {
			$newstate = "s_array";
	} elsif ($state eq "s_used" && $type eq "t_array") {
			$newstate = "s_usedarray";
	} elsif ($state eq "s_array" && $type eq "t_usage") {
			$newstate = "s_usedarray";
	} else {
		$newstate = $state;
	}
	$elements{$element} = $newstate;
	$debug>=2 && print "$currfile, $element: SET $state + $type => $newstate$D\n";
}
&printprevfile();

exit $error;

__END__
