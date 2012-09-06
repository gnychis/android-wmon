LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= awmon.c awmon_helper.c serial.c
LOCAL_MODULE := awmon
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy jni/libpcap
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat libwispy libpcap libnls libnl
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_SHARED_LIBRARY)



