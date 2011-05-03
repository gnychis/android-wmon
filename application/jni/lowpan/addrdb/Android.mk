LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

#LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES:= \
	coord-config-parse.c \
	coord-config-lex.c \
	addrdb.c \


LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android \
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../../libnl/include 

LOCAL_CFLAGS +=  -g
LOCAL_CFLAGS += -fPIC -DPIC

LOCAL_STATIC_LIBRARIES := libnl liblowpan_common

ifeq ($(TARGET_BUILD_TYPE),release)
	LOCAL_CFLAGS += -g
endif

LOCAL_MODULE:= liblowpan_addrdb

LOCAL_PRELINK_MODULE := false 
include $(BUILD_STATIC_LIBRARY)

