LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	md4.c \
	sha512.c \
	ecc.c \
	md.c \
	whirlpool.c \
	rfc2268.c \
	tiger.c \
	rmd160.c \
	twofish.c \
	rijndael.c \
	rsa.c \
	hmac-tests.c \
	ac.c \
	cast5.c \
	serpent.c \
	blowfish.c \
	sha256.c \
	seed.c \
	camellia.c \
	cipher.c \
	md5.c \
	elgamal.c \
	pubkey.c \
	camellia-glue.c \
	primegen.c \
	arcfour.c \
	hash-common.c \
	sha1.c \
	crc.c \
	des.c

LOCAL_SHARED_LIBRARIES := \
                        libc \
                        libcutils \
                        bionic

# PACKAGE_NAME := libcypher  

LOCAL_CFLAGS += -DHAVE_CONFIG_H

LOCAL_C_INCLUDES:= \
        $(LOCAL_PATH)/..  \
        $(LOCAL_PATH)/../src/  \
	external/libgpg-error/src

LOCAL_MODULE:= libgcrypt-cipher

include $(BUILD_STATIC_LIBRARY)

