LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= coexisyst.c coexisyst_helper.c
LOCAL_MODULE := coexisyst
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat libwispy
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_SHARED_LIBRARY)



