LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= pcapd.c
LOCAL_MODULE := pcapd
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/libpcap
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat libpcap
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_EXECUTABLE)
