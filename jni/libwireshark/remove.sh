#!/bin/bash
find . -name "*.c" -exec rm -fr {} \;
find . -name "*.lo" -exec rm -fr {} \;
find . -name "*.la" -exec rm -fr {} \;
find . -name "*.o" -exec rm -fr {} \;
find . -name "*.a" -exec rm -fr {} \;
find . -name "*.libs" -exec rm -fr {} \;
find . -name "*.deps" -exec rm -fr {} \;
find . -name "*.svn" -exec rm -fr {} \;
find . -name "*.xml" -exec rm -fr {} \;
find . -name "Makefile*" -exec rm -fr {} \;
find . -name "*.txt" -exec rm -fr {} \;
find . -name "*.vcproj" -exec rm -fr {} \;
find . -name "*.cmake" -exec rm -fr {} \;
find . -name "AUTHORS" -exec rm -fr {} \;
find . -name "README*" -exec rm -fr {} \;
