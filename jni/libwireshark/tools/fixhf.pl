#!/usr/bin/env perl

# Copyright 2010, Jeff Morriss <jeff.morriss[AT]ulticom.com>
#
# A simple tool to remove bogus blurbs from hf entries.
# This has already been run so it may not be necessary any more, but
# may as well check it in in case it can serve as a base for other, future,
# global hf changes.
#
# Usage:
# fixhf.pl file1 [file2 file3 ...]
#
# $Id: fixhf.pl 34242 2010-09-24 20:40:03Z morriss $
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
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
#

use strict;

# Read through the files
while ($_ = $ARGV[0])
{
        shift;
        my $filename = $_;
        my $fileContents = '';
        my @foundAPIs = ();

        die "No such file: \"$filename\"" if (! -e $filename);

        # delete leading './'
        $filename =~ s{ ^ \. / } {}xo;

        # Read in the file (ouch, but it's easier that way)
        open(FC, $filename) || die("Couldn't open $filename");
        while (<FC>) { $fileContents .= $_; }
        close(FC);

	if ($fileContents =~ s{   
				  (\{
				  \s*
				  &\s*[A-Z0-9_\[\]-]+		# &hf
				  \s*,\s*
				  \{\s*
				  ("[A-Z0-9 '\./\(\)_:-]+")	# name
				  \s*,\s*
				  "[A-Z0-9_\.-]+"		# abbrev
				  \s*,\s*
				  FT_[A-Z0-9_]+			# field type
				  \s*,\s*
				  [A-Z0-9x|_]+			# display
				  \s*,\s*
				  [A-Z0-9&_\(\)' -]+		# convert
				  \s*,\s*
				  [A-Z0-9x_]+			# bitmask
				  \s*,\s*)
				  \2				# blurb
				} [$1NULL]xgios)
				 # \s*HFILL)
	{
		print STDERR "Warning: field with name==blurb found in " .$filename. " FIXING IT!\n";

		# Trim trailing white space while we're here
		$fileContents =~ s{[ \t]+$} []gom;

		open(FC, ">".$filename) || die("Couldn't open $filename");
		print FC $fileContents;
		close(FC);
	}

}
