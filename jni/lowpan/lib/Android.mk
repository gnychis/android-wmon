LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

#LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES:= \
	printbuf.c \
	genl.c \
	parse.c \
	shash.c \
	logging.c \
	nl_policy.c \


LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android \
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../../libnl/include 

LOCAL_CFLAGS +=  -g
LOCAL_CFLAGS += -fPIC -DPIC

LOCAL_SHARED_LIBRARIES := libnl

ifeq ($(TARGET_BUILD_TYPE),release)
	LOCAL_CFLAGS += -g
endif

LOCAL_MODULE:= liblowpan_common

LOCAL_PRELINK_MODULE := false 
include $(BUILD_SHARED_LIBRARY)

