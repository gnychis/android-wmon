#!/bin/bash
#
# $Id: osx-dmg.sh 33222 2010-06-13 23:02:18Z gerald $
#
# USAGE
# osx-dmg [-s] -p /path/to/Wireshark.app
#
# The script creates a read-write disk image,
# copies Wireshark into it, customizes its appearance using a
# previously created .DS_Store file (wireshark.ds_store),
# and then compresses the disk image for distribution.
#
# Copied from Inkscape.
#
# AUTHORS
#	Jean-Olivier Irisson <jo.irisson@gmail.com>
#	Michael Wybrow <mjwybrow@users.sourceforge.net>
#
# Copyright (C) 2006-2007
# Released under GNU GPL, read the file 'COPYING' for more information
#
#
# How to update the disk image layout:
# ------------------------------------
#
# Modify the 'dmg_background.svg' file and generate a new
# 'dmg_background.png' file.
#
# Update the AppleScript file 'dmg_set_style.scpt'.
#
# Run this script with the '-s' option.  It will apply the
# 'dmg_set_style.scpt' AppleScript file, and then prompt the
# user to check the window size and position before writing
# a new 'wireshark.ds_store' file to work around a bug in Finder
# and AppleScript.  The updated 'wireshark.ds_store' will need
# to be commited to the repository when this is done.
#

# Defaults
set_ds_store=false
ds_store_root="root.ds_store"
ds_store_util="util.ds_store"
package="Wireshark.app"
rw_name="RWwireshark.dmg"
volume_name="Wireshark"
tmp_dir="/tmp/dmg-$$"
auto_open_opt=
utilities="Utilities"
ws_bin="$package/Contents/Resources/bin/wireshark-bin"

PATH=$PATH:/Developer/Tools

# Help message
#----------------------------------------------------------
help()
{
echo -e "
Create a custom dmg file to distribute Wireshark

USAGE
	$0 [-s] -p /path/to/Wireshark.app

OPTIONS
	-h,--help
		display this help message
	-s
		set a new apperance (do not actually creates a bundle)
	-p,--package
		set the path to the Wireshark.app that should be copied
		in the dmg
"
}

# Parse command line arguments
while [ "$1" != "" ]
do
	case $1 in
	  	-h|--help)
			help
			exit 0 ;;
	  	-s)
			set_ds_store=true ;;
	  	-p|--package)
			package="$2"
			shift 1 ;;
		*)
			echo "Invalid command line option"
			exit 2 ;;
	esac
	shift 1
done

# Safety checks
if [ ! -e "$package" ]; then
	echo "Cannot find package: $package"
	exit 1
fi

# Safety checks
if [ ! -e "$utilities" ]; then
	echo "Cannot find utilities: $utilities"
	exit 1
fi
echo -e "\nCREATE WIRESHARK DISK IMAGE\n"

# Get the architecture
case `file $ws_bin` in
	*Mach-O*64-bit*x86_64)
		architecture="Intel 64"
		;;
	*Mach-O*i386)
		architecture="Intel 32"
		;;
	*Mach-O*ppc64)
		architecture="PPC 64"
		;;
	*Mach-O*ppc)
		architecture="PPC 32"
		;;
	*)
		echo "Cannot determine architecture"
		exit 1
		;;
esac

# Set the version
version=`grep '^AC_INIT' ../../configure.in | sed -e 's/.*, //' -e 's/)//'`
if [ -z "$version" ] ; then
	echo "Cannot find VERSION in ../../configure.in"
	exit 1
fi
img_name="$volume_name $version $architecture.dmg"

# Create temp directory with desired contents of the release volume.
rm -rf "$tmp_dir"
mkdir "$tmp_dir"

echo -e "Copying files to temp directory"
# Wireshark itself
# Copy Wireshark.app
cp -rf "$package" "$tmp_dir"/
# Link to Applications in order to drag and drop wireshark onto it
ln -sf /Applications "$tmp_dir"/
# Copy the utilites
cp -rf "$utilities" "$tmp_dir"/
ln -sf /Library/StartupItems "$tmp_dir/$utilities"/
# Copy the readme
cp -rf  Read_me_first.rtf "$tmp_dir"/"Read me first.rtf"

# Copy a background images inside hidden directories so the image file itself won't be shown.
mkdir "$tmp_dir/.background"
cp dmg_background.png "$tmp_dir/.background/background.png"
mkdir "$tmp_dir/$utilities/.background"
cp util_background.png "$tmp_dir/$utilities/.background/background.png"

# If the appearance settings are not to be modified we just copy them
if [ ${set_ds_store} = "false" ]; then
	# Copy the .DS_Store file which contains information about
	# window size, appearance, etc.  Most of this can be set
	# with Apple script but involves user intervention so we
	# just keep a copy of the correct settings and use that instead.
	cp $ds_store_root "$tmp_dir/.DS_Store"
	cp $ds_store_util "$tmp_dir/$utilities/.DS_Store"
	auto_open_opt=-noautoopen
fi

# Create a new RW image from the temp directory.
echo -e "Creating a temporary disk image"
rm -f "$rw_name"
/usr/bin/hdiutil create -srcfolder "$tmp_dir" -volname "$volume_name" -fs HFS+ -fsargs "-c c=64,a=16,e=16" -format UDRW "$rw_name"

# We're finished with the temp directory, remove it.
rm -rf "$tmp_dir"

# Mount the created image.
MOUNT_DIR="/Volumes/$volume_name"
DEV_NAME=`/usr/bin/hdiutil attach -readwrite -noverify $auto_open_opt  "$rw_name" | egrep '^/dev/' | sed 1q | awk '{print $1}'`

# Have the disk image window open automatically when mounted.
bless -openfolder /Volumes/$volume_name

# In case the apperance has to be modified, mount the image and apply the base settings to it via Applescript
if [ ${set_ds_store} = "true" ]; then
	/usr/bin/osascript dmg_set_style.scpt

	open "/Volumes/$volume_name"
	# BUG: one needs to move and close the window manually for the
	# changes in appearance to be retained...
        echo "
        **************************************
        *  Please move the disk image window *
        *    to the center of the screen     *
        *   then close it and press enter    *
        **************************************
        "
        read -e DUMB

	# .DS_Store files aren't written till the disk is unmounted, or finder is restarted.
	hdiutil detach "$DEV_NAME"
	auto_open_opt=-noautoopen
	DEV_NAME=`/usr/bin/hdiutil attach -readwrite -noverify $auto_open_opt  "$rw_name" | egrep '^/dev/' | sed 1q | awk '{print $1}'`
	echo
	cp /Volumes/$volume_name/.DS_Store ./$ds_store_root
	SetFile -a v ./$ds_store_root
	cp /Volumes/$volume_name/$utilities/.DS_Store ./$ds_store_util
	SetFile -a v ./$ds_store_util
	echo "New $ds_store_root and $ds_store_util written. Re-run $0 without the -s option to use them"

	# Unmount the disk image.
	hdiutil detach "$DEV_NAME"
	rm -f "$rw_name"

	exit 0
fi

# Unmount the disk image.
hdiutil detach "$DEV_NAME"

# Create the offical release image by compressing the RW one.
echo -e "Compressing the final disk image"

# TODO make this a command line option
if [ -e "$img_name" ]; then
	echo "$img_name already exists."
	rm -i "$img_name"
fi
/usr/bin/hdiutil convert "$rw_name" -format UDZO -imagekey zlib-level=9 -o "$img_name"
rm -f "$rw_name"

exit 0
