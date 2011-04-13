LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    gthread-impl.c         
    
LOCAL_SHARED_LIBRARIES := libglib-2.0

LOCAL_MODULE:= libgthread-2.0

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)             \
    $(LOCAL_PATH)/..          \
    $(LOCAL_PATH)/../android  \
    $(LOCAL_PATH)/../glib

LOCAL_CFLAGS := \
    -DG_LOG_DOMAIN=\"GThread\"      \
    -D_POSIX4_DRAFT_SOURCE          \
    -D_POSIX4A_DRAFT10_SOURCE       \
    -U_OSF_SOURCE                   \
    -DG_DISABLE_DEPRECATED 

include $(BUILD_SHARED_LIBRARY)
