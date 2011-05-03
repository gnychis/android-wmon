LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=\
	addr.c \
  attr.c \
	cache.c\
  cache_mngr.c \
  cache_mngt.c \
  data.c \
  handlers.c \
  msg.c \
  nl.c \
  object.c\
  socket.c \
  utils.c \
	genl/ctrl.c \
	genl/family.c \
	genl/genl.c \
	genl/mngt.c



LOCAL_C_INCLUDES += jni/libnl/include

LOCAL_CFLAGS:=-O2 -g
LOCAL_CFLAGS+=-DHAVE_CONFIG_H -D_U_="__attribute__((unused))" -Dlinux -D__GLIBC__ -D_GNU_SOURCE

LOCAL_MODULE:= libnl

include $(BUILD_STATIC_LIBRARY)
