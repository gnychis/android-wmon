LOCAL_PATH:= $(call my-dir)
MY_LOCAL_PATH := $(LOCAL_PATH)
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libwireshark.a

include $(BUILD_MULTI_PREBUILT)

LOCAL_PATH := $(MY_LOCAL_PATH)
include $(CLEAR_VARS)
NDK_MODULE_PATH := $(LOCAL_PATH)
LOCAL_SRC_FILES:= wireshark_helper.c 
LOCAL_MODULE := libwireshark_helper
LOCAL_PREBUILT_LIBS := libwireshark.a
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy jni/libpcap jni/libwireshark jni/libglib jni/libglib/glib jni/libglib/android
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat libwispy libpcap libglib-2.0
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -L$(LOCAL_PATH) -llog -lwireshark
include $(BUILD_SHARED_LIBRARY)

