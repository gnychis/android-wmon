#!/bin/bash
#
# $Id: win32-setup.sh 36586 2011-04-12 15:44:05Z etxrab $

# 32-bit wrapper for win-setup.sh.

export DOWNLOAD_TAG="2011-04-12B"
export WIRESHARK_TARGET_PLATFORM="win32"

WIN_SETUP=`echo $0 | sed -e s/win32/win/`

exec $WIN_SETUP $@
