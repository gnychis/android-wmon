LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	rndlinux.c \
	rndhw.c \
	rndegd.c \
	random-csprng.c \
	rndunix.c \
	random-daemon.c \
	random.c \
	random-fips.c 
 


LOCAL_SHARED_LIBRARIES := \
                        libc \
                        libcutils \
                        bionic

PACKAGE_NAME := libgcrypt

LOCAL_CFLAGS += -DHAVE_CONFIG_H -D__android__

LOCAL_C_INCLUDES:= \
        $(LOCAL_PATH)/..  \
        $(LOCAL_PATH)/../src/  \
	external/libgpg-error/src

LOCAL_MODULE:= libgcrypt-random

include $(BUILD_STATIC_LIBRARY)

