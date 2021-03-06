LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
NDK_MODULE_PATH := $(LOCAL_PATH)
LOCAL_SRC_FILES:= wireshark_helper.c
LOCAL_MODULE := libwireshark_helper
LOCAL_CFLAGS := -DHAVE_LIBPCAP
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy jni/libpcap jni/libwireshark jni/libglib jni/libglib/glib jni/libglib/android jni/libwireshark/epan jni/libwireshark/epan/dissectors
LOCAL_SHARED_LIBRARIES := libc libusb libusb-compat libwispy libpcap libglib-2.0 libgmodule-2.0 libnl libtshark
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -L$(LOCAL_PATH) -L$(LOCAL_PATH)/wireshark_libs -llog -lgcc -lz
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
NDK_MODULE_PATH := $(LOCAL_PATH)
LOCAL_SRC_FILES:= tshark.c
LOCAL_MODULE := tshark_exec
LOCAL_CFLAGS := -DHAVE_LIBPCAP
LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy jni/libpcap jni/libwireshark jni/libglib/gmodule jni/libglib jni/libglib/glib jni/libglib/android jni/libwireshark/epan jni/libwireshark/epan/dissectors
LOCAL_SHARED_LIBRARIES := libc libgpg-error libgcrypt libusb libusb-compat libwispy libpcap libglib-2.0 libgmodule-2.0 libnl libtshark
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -L$(LOCAL_PATH) -L$(LOCAL_PATH)/wireshark_libs -llog -lgcc -lz
include $(BUILD_EXECUTABLE)

