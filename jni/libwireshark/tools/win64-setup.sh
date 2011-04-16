#!/bin/bash
#
# $Id: win64-setup.sh 36611 2011-04-12 21:09:34Z etxrab $

# 64-bit wrapper for win-setup.sh.

export DOWNLOAD_TAG="2011-04-12"
export WIRESHARK_TARGET_PLATFORM="win64"

WIN_SETUP=`echo $0 | sed -e s/win64/win/`

exec $WIN_SETUP $@
