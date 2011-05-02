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

include $(CLEAR_VARS)
LOCAL_CFLAGS += -g
LOCAL_SRC_FILES:= serial.c
LOCAL_MODULE := izattach
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android \
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../../libnl/include 
LOCAL_SHARED_LIBRARIES := libnl liblowpan_common liblowpan_addrdb
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_CFLAGS += -g -DPID_FILE=\"/data/local/tmp/izcoordinator.pid\" -DLEASE_FILE=\"/data/local/tmp/izcoordinator.leases\"
LOCAL_SRC_FILES:= coordinator.c
LOCAL_MODULE := izcoordinator
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android \
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../../libnl/include 
LOCAL_SHARED_LIBRARIES := libnl liblowpan_common liblowpan_addrdb
include $(BUILD_EXECUTABLE)
