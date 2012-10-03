#!/bin/bash
git submodule init
git submodule update

# Get the master on the root helper
cd android-root-helper
git checkout master
cd ../

# Get the master on the core
cd core
git checkout master

# Initialize and update all of the submodules in core/
git submodule init
git submodule update

# Get the relative branch for each submodule
cd galaxynexus-cm10-kernel
  git checkout jb
  cd ../
cd linaro-android-toolchain
  git checkout master
  cd ../
cd sgs2-cm9-kernel
  git checkout ics
  cd ../
cd sgs2-skyrocket-kernel
  git checkout master
  cd ../
cd sgs3-cm10-kernel
  git checkout jellybean
  cd ../
cd usbhost-kernel
  git checkout 
  cd ../
