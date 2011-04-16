#
# makenmake.pl - create a nmake file from a generic manifest file that will create the appropriate U3 structure
# $Id: makenmake.pl 32287 2010-03-25 22:22:45Z wmeier $
#

print "
include ../../../config.nmake
include <win32.mak>

DEVICE 	= device
HOST 	= host
MANIFEST= manifest
DATA	= data

TOPDIR	= ..\\..\\..
COPY	= xcopy
MOVE    = mv
MKDIR	= mkdir
COPY_FLAGS	= /d /y 

distribution:
";

while($line = <>) {
    
    if($line =~ /^\#/) { # comment
	next;
    } elsif($line =~ /^\[(\S+)/) { # new directory
	$dir = $1;
	undef $u3loc;

	if($line =~ /u3loc=(\w+)/) {
	    $u3loc = "\$(" . uc $1 . ")";
	}

	$dir =~ s/\$INSTDIR?//; # remove $INSTDIR
	
	$dir =~ s/\{/\(/g; $dir =~ s/\}/\)/g; # convert curlies to round brackets

	if($dir ne '') { 
	    print "\tif not exist $u3loc$dir \$(MKDIR) $u3loc$dir\n";
	}
	
    } else { # this is a file

	$line =~ /^\s+(\S+)/;
	$file = $1;

	$file =~ s/\{/\(/g; $file =~ s/\}/\)/g; # convert curlies to round brackets

	if($file =~ /^[^\$]/) {
	    $file = "\$(TOPDIR)\\" . $file;
	}

	if($line =~ /ifdef=(\w+)/) { # dependency
	    if($define ne $1) {
		if(defined $define) {
		    print "!ENDIF\n";
		}
		$define = $1;
		print "!IF DEFINED($define)\n";
	    }
	} else {
	    
	    if(defined $define) {
		print "!ENDIF\n";
	    }
	    undef $define;
	}

	$u3fileloc = $u3loc; # default location
	$oname = "";

	if($line =~ /u3loc=(\S+)/) { # override this location
	    $u3fileloc = "\$(" . uc $1 . ")";
	}

	print "\t\$(COPY) \"$file\" \"$u3fileloc$dir\" \$(COPY_FLAGS)\n";

	if($line =~ /oname=(\S+)/) { # override this filename
	    $oname = $1;
	    $file =~ /\\(.*)$/;
	    $name = $1;

	    print "\t\$(MOVE) \"$u3fileloc$dir\\$name\" \"$u3fileloc$dir\\$oname\"\n";

	}

    }
}
