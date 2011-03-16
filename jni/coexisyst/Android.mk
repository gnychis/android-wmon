LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= coexisyst.c
LOCAL_MODULE := coexisyst
LOCAL_C_INCLUDES += jni/libusb-compat/libusb
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat
include $(BUILD_SHARED_LIBRARY)



