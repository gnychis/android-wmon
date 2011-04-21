LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libtshark
LOCAL_SRC_FILES := libtshark.so
include $(PREBUILT_SHARED_LIBRARY)
