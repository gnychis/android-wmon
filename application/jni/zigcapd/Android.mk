LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= zigcapd.c serial.c
LOCAL_MODULE := zigcapd
LOCAL_C_INCLUDES += jni/libpcap
LOCAL_SHARED_LIBRARIES := libc libpcap
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_EXECUTABLE)
