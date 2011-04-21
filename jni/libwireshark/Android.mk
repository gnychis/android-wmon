LOCAL_PATH:= $(call my-dir)
MY_LOCAL_PATH := $(LOCAL_PATH)
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := wireshark_libs/libwireshark.a wireshark_libs/libwsutil.a wireshark_libs/libtshark.a wireshark_libs/libwiretap.a

include $(BUILD_MULTI_PREBUILT)

LOCAL_PATH := $(MY_LOCAL_PATH)
include $(CLEAR_VARS)
NDK_MODULE_PATH := $(LOCAL_PATH)
LOCAL_SRC_FILES:= wireshark_helper.c
LOCAL_MODULE := libwireshark_helper
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy jni/libpcap jni/libwireshark jni/libglib jni/libglib/glib jni/libglib/android jni/libwireshark/epan jni/libwireshark/epan/dissectors
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat libwispy libpcap libglib-2.0 libgmodule-2.0 libnl
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -L$(LOCAL_PATH) -L$(LOCAL_PATH)/wireshark_libs -llog -lwireshark -lgcc -lwsutil -lz -lwiretap -ltshark
include $(BUILD_SHARED_LIBRARY)

