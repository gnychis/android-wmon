LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)


LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES:= \
	mpih-mul.c \
	mpi-add.c \
	mpih-div.c \
	mpih-sub1.c \
	generic \
	generic/mpih-sub1.c \
	generic/mpih-mul1.c \
	generic/udiv-w-sdiv.c \
	generic/mpih-mul3.c \
	generic/mpih-add1.c \
	generic/mpih-mul2.c \
	generic/mpih-rshift.c \
	generic/mpih-lshift.c \
	ec.c \
	mpi-mpow.c \
	mpi-pow.c \
	mpi-gcd.c \
	mpi-inv.c \
	mpi-scan.c \
	mpih-mul1.c \
	mpih-mul3.c \
	mpi-div.c \
	mpi-inline.c \
	mpih-add1.c \
	mpi-cmp.c \
	mpih-mul2.c \
	mpih-rshift.c \
	mpih-lshift.c \
	mpi-mod.c \
	mpiutil.c \
	mpi-bit.c \
	supersparc \
	mpicoder.c \
	mpi-mul.c 
 
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

LOCAL_MODULE:= libgcrypt-mpi 

include $(BUILD_STATIC_LIBRARY)

