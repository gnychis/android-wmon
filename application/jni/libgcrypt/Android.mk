LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES:= \
	cipher/seed.c \
	cipher/rfc2268.c \
	cipher/md5.c \
	cipher/camellia.c \
	cipher/dsa.c \
	cipher/cipher.c \
	cipher/blowfish.c \
	cipher/rijndael.c \
	cipher/pubkey.c \
	cipher/md.c \
	cipher/rsa.c \
	cipher/camellia-glue.c \
	cipher/des.c \
	cipher/twofish.c \
	cipher/ecc.c \
	cipher/crc.c \
	cipher/tiger.c \
	cipher/whirlpool.c \
	cipher/sha1.c \
	cipher/hash-common.c  \
	cipher/sha512.c \
	cipher/ac.c \
	cipher/hmac-tests.c \
	cipher/cast5.c \
	cipher/elgamal.c \
	cipher/serpent.c \
	cipher/arcfour.c \
	cipher/sha256.c \
	cipher/rmd160.c \
	cipher/md4.c \
	cipher/primegen.c \
	src/stdmem.c \
	src/module.c \
	src/sexp.c \
	src/secmem.c \
	src/hwfeatures.c \
	src/misc.c \
	src/ath.c \
	src/global.c \
	src/fips.c \
	src/missing-string.c \
	src/visibility.c \
	src/hmac256.c \
	random/rndlinux.c \
	random/rndhw.c \
	random/rndegd.c \
	random/random-csprng.c \
	random/rndunix.c \
	random/random-daemon.c \
	random/random.c \
	random/random-fips.c \
	mpi/mpih-mul.c \
	mpi/mpi-add.c \
	mpi/mpih-div.c \
	mpi/mpih-sub1.c \
	mpi/ec.c \
	mpi/mpi-mpow.c \
	mpi/mpi-pow.c \
	mpi/mpi-gcd.c \
	mpi/mpi-inv.c \
	mpi/mpi-scan.c \
	mpi/mpih-mul1.c \
	mpi/mpih-mul3.c \
	mpi/mpi-div.c \
	mpi/mpi-inline.c \
	mpi/mpih-add1.c \
	mpi/mpi-cmp.c \
	mpi/mpih-mul2.c \
	mpi/mpih-rshift.c \
	mpi/mpih-lshift.c \
	mpi/mpi-mod.c \
	mpi/mpiutil.c \
	mpi/mpi-bit.c \
	mpi/mpicoder.c \
	mpi/mpi-mul.c

LOCAL_SHARED_LIBRARIES := \
                        libc \
												libgpg-error \

LOCAL_CFLAGS += -DHAVE_CONFIG_H -D__android__

PACKAGE_NAME := libgcrypt

LOCAL_CFLAGS += -DHAVE_CONFIG_H

LOCAL_C_INCLUDES:= \
        $(LOCAL_PATH)/../libgpg-error/src \
        $(LOCAL_PATH)/  \
        $(LOCAL_PATH)/src/ 

LOCAL_MODULE:= libgcrypt

include $(BUILD_SHARED_LIBRARY)
