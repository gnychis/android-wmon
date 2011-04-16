#!/bin/bash
#
# $Id: fuzz-test.sh 36461 2011-04-04 18:43:59Z morriss $

# Fuzz-testing script for TShark
#
# This script uses Editcap to add random errors ("fuzz") to a set of
# capture files specified on the command line.  It runs TShark on
# each fuzzed file and checks for errors.  The files are processed
# repeatedly until an error is found.

# This needs to point to a 'date' that supports %s.
DATE=/bin/date
BASE_NAME=fuzz-`$DATE +%Y-%m-%d`-$$

# Directory containing binaries.  Default current directory.
BIN_DIR=.

# Temporary file directory and names.
# (had problems with this on cygwin, tried TMP_DIR=./ which worked)
TMP_DIR=/tmp
if [ "$OSTYPE" == "cygwin" ] ; then
        TMP_DIR=`cygpath --windows "$TMP_DIR"`
fi
TMP_FILE=$BASE_NAME.pcap
ERR_FILE=$BASE_NAME.err

# Loop this many times (< 1 loops forever)
MAX_PASSES=0

# Perform a two pass analysis on the capture file?
TWO_PASS=

# Specific config profile ?
CONFIG_PROFILE=

# These may be set to your liking
# Stop the child process if it's running longer than x seconds
MAX_CPU_TIME=900
# Stop the child process if it's using more than y * 1024 bytes
MAX_VMEM=500000
# Stop the child process if its stack is larger than than z * 1024 bytes
# Windows XP:   2033
# Windows 7:    2034
# OS X 10.6:    8192
# Linux 2.6.24: 8192
# Solaris 10:   8192
MAX_STACK=2033
# Insert z times an error into the capture file (0.02 seems to be a good value to find errors)
ERR_PROB=0.02
# Trigger an abort if a dissector finds a bug.
# Uncomment to enable
# Note that if ABORT is enabled there will be no info
#  output to stderr about the DISSECTOR_BUG.
#  (There'll just be a core-dump).
###export WIRESHARK_ABORT_ON_DISSECTOR_BUG="True"


# To do: add options for file names and limits
while getopts ":b:d:e:C:Pp:" OPTCHAR ; do
    case $OPTCHAR in
        b) BIN_DIR=$OPTARG ;;
        C) CONFIG_PROFILE="-C $OPTARG " ;;
        d) TMP_DIR=$OPTARG ;;
        e) ERR_PROB=$OPTARG ;;
        p) MAX_PASSES=$OPTARG ;;
        P) TWO_PASS="-P " ;;
    esac
done
shift $(($OPTIND - 1))

# Tweak the following to your liking.  Editcap must support "-E".
TSHARK="$BIN_DIR/tshark"
EDITCAP="$BIN_DIR/editcap"
CAPINFOS="$BIN_DIR/capinfos"

if [ "$BIN_DIR" = "." ]; then
    export WIRESHARK_RUN_FROM_BUILD_DIRECTORY=1
fi

# set some limits to the child processes, e.g. stop it if it's running longer then MAX_CPU_TIME seconds
# (ulimit is not supported well on cygwin and probably other platforms, e.g. cygwin shows some warnings)
ulimit -S -t $MAX_CPU_TIME -v $MAX_VMEM -s $MAX_STACK
ulimit -c unlimited

### usually you won't have to change anything below this line ###

# TShark arguments (you won't have to change these)
# n Disable network object name resolution
# V Print a view of the details of the packet rather than a one-line summary of the packet
# x Cause TShark to print a hex and ASCII dump of the packet data after printing the summary or details
# r Read packet data from the following infile
TSHARK_ARGS="${CONFIG_PROFILE}${TWO_PASS}-nVxr"

NOTFOUND=0
for i in "$TSHARK" "$EDITCAP" "$CAPINFOS" "$DATE" "$TMP_DIR" ; do
    if [ ! -x $i ]; then
        echo "Couldn't find $i"
        NOTFOUND=1
    fi
done
if [ $NOTFOUND -eq 1 ]; then
    exit 1
fi

# Make sure we have a valid test set
FOUND=0
for CF in "$@" ; do
    if [ "$OSTYPE" == "cygwin" ] ; then
        CF=`cygpath --windows "$CF"`
    fi
    "$CAPINFOS" "$CF" > /dev/null 2>&1 && FOUND=1
    if [ $FOUND -eq 1 ] ; then break ; fi
done

if [ $FOUND -eq 0 ] ; then
    cat <<FIN
Error: No valid capture files found.

Usage: `basename $0` [-b bin_dir] [-C config_profile] [-p passes] [-d work_dir] [-P ] [-e error probability] capture file 1 [capture file 2]...
FIN
    exit 1
fi

HOWMANY="forever"
if [ $MAX_PASSES -gt 0 ]; then
        HOWMANY="$MAX_PASSES passes"
fi
echo "Running $TSHARK with args: $TSHARK_ARGS ($HOWMANY)"
echo ""

# Clean up on <ctrl>C, etc
trap "rm -f $TMP_DIR/$TMP_FILE $TMP_DIR/$ERR_FILE; echo ""; exit 0" HUP INT TERM

# Iterate over our capture files.
PASS=0
while [ $PASS -lt $MAX_PASSES -o $MAX_PASSES -lt 1 ] ; do
    PASS=`expr $PASS + 1`
    echo "Starting pass $PASS:"
    RUN=0

    for CF in "$@" ; do
        RUN=$(( $RUN + 1 ))
        if [ $(( $RUN % 50 )) -eq 0 ] ; then
            echo "    [Pass $PASS]"
        fi
        if [ "$OSTYPE" == "cygwin" ] ; then
            CF=`cygpath --windows "$CF"`
        fi
	echo -n "    $CF: "

	"$CAPINFOS" "$CF" > /dev/null 2> $TMP_DIR/$ERR_FILE
	RETVAL=$?
	if [ $RETVAL -eq 1 ] ; then
	    echo "Not a valid capture file"
	    rm -f $TMP_DIR/$ERR_FILE
	    continue
	elif [ $RETVAL -ne 0 ] ; then
	    # Some other error
	    echo ""
	    echo " ERROR"
	    echo -e "Processing failed.  Capture info follows:\n"
	    echo "  Input file: $CF"
	    echo -e "stderr follows:\n"
	    cat $TMP_DIR/$ERR_FILE
	    exit 1
	fi

	DISSECTOR_BUG=0

	"$EDITCAP" -E $ERR_PROB "$CF" $TMP_DIR/$TMP_FILE > /dev/null 2>&1
	if [ $? -ne 0 ] ; then
	    "$EDITCAP" -E $ERR_PROB -T ether "$CF" $TMP_DIR/$TMP_FILE \
	    > /dev/null 2>&1
	    if [ $? -ne 0 ] ; then
	    echo "Invalid format for editcap"
	    continue
	    fi
	fi

	export WIRESHARK_DEBUG_SCRUB_MEMORY=
	export WIRESHARK_DEBUG_SE_USE_CANARY=
	export WIRESHARK_EP_VERIFY_POINTERS=
	export WIRESHARK_SE_VERIFY_POINTERS=
	export G_SLICE=debug-blocks             # since GLib 2.13
	export MALLOC_CHECK_=3
	"$TSHARK" $TSHARK_ARGS $TMP_DIR/$TMP_FILE \
	    > /dev/null 2>> $TMP_DIR/$ERR_FILE
	RETVAL=$?
	# Uncomment the next two lines to enable dissector bug
	# checking.
	#grep -i "dissector bug" $TMP_DIR/$ERR_FILE \
	#    > /dev/null 2>&1 && DISSECTOR_BUG=1
	if [ $RETVAL -ne 0 -o $DISSECTOR_BUG -ne 0 ] ; then
		echo ""
	    echo " ERROR"
	    echo -e "Processing failed.  Capture info follows:\n"
	    echo "  Output file: $TMP_DIR/$TMP_FILE"
	    echo -e "stderr follows:\n"
	    cat $TMP_DIR/$ERR_FILE
	    exit 1
	fi
	echo " OK"
	    rm -f $TMP_DIR/$TMP_FILE $TMP_DIR/$ERR_FILE
    done
done

