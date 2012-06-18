LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= spectool_container.c wispy_hw_gen1.c wispy_hw_24x.c wispy_hw_dbx.c spectool_net_client.c
LOCAL_MODULE := wispy
LOCAL_C_INCLUDES += jni/libusb-compat/libusb 
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_SHARED_LIBRARY)
