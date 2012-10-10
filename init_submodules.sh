#!/bin/bash
git submodule init
git submodule update

# Get the master on the root helper
cd android-root-helper
git checkout master
cd ../

# Get the master on the core
cd core
git pull
git checkout master
git pull

# Initialize and update all of the submodules in core/
git submodule init
git submodule update

# Get the relative branch for each submodule
cd galaxynexus-cm10-kernel
  git checkout jb
  git pull
  cd ../
cd linaro-android-toolchain
  git checkout master
  git pull
  cd ../
cd sgs2-cm9-kernel
  git checkout ics
  git pull
  cd ../
cd sgs2-skyrocket-kernel
  git checkout master
  git pull
  cd ../
cd sgs3-cm10-kernel
  git checkout jellybean
  git pull
  cd ../
cd usbhost-kernel
  git checkout coexisyst
  git pull
  cd ../
