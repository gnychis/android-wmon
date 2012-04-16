LOCAL_PATH := $(call my-dir)

ifneq ($(TARGET_SIMULATOR),true)
  include $(call all-subdir-makefiles)
endif
