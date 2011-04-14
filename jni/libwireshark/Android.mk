LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= wireshark_helper.c 
LOCAL_MODULE := libwireshark
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy jni/libpcap jni/libwireshark
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat libwispy libpcap
LOCAL_STATIC_LIBRARIES := libwireshark_r36615
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)
#$(call import-module, libwireshark_r36615)
