LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	./sexp.c \
	./hwfeatures.c \
	./ath.c \
	./stdmem.c \
	./versioninfo.rc \
	./module.c \
	./missing-string.c \
	./getrandom.c \
	./hmac256.c \
	./visibility.c \
	./global.c \
	./fips.c \
	./misc.c \
	./dumpsexp.c \
	./secmem.c

LOCAL_SHARED_LIBRARIES := \
                        libc \
                        libcutils \
                        bionic

PACKAGE_NAME := libgcrypt

LOCAL_CFLAGS += -DHAVE_CONFIG_H

LOCAL_C_INCLUDES:= \
        $(LOCAL_PATH)/..  \
        $(LOCAL_PATH)/../src/  \
	external/libgpg-error/src

LOCAL_MODULE:= libgcrypt  

include $(BUILD_STATIC_LIBRARY)

