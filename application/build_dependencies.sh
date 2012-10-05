#!/bin/bash
cp jni/libpcap/scanner_good.c jni/libpcap/scanner.c
cp jni/libpcap/grammar_good.c jni/libpcap/grammar.c
../core/android-ndk-r8b-linux/ndk-build NDK_DEBUG=1
cp jni/libtshark/libtshark.so libs/armeabi/
cp libs/armeabi/iwlist res/raw
cp libs/armeabi/iw res/raw
cp libs/armeabi/testlibusb res/raw
cp libs/armeabi/lsusb_core res/raw
