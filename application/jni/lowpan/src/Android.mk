LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS += -g
LOCAL_SRC_FILES:= iz-common.c iz-mac.c iz-phy.c iz.c 
LOCAL_MODULE := iz
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android \
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../../libnl/include 
LOCAL_SHARED_LIBRARIES := libnl liblowpan_common liblowpan_addrdb
include $(BUILD_EXECUTABLE)
