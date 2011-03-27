LOCAL_PATH := $(call my-dir)
subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
    libusb \
    libusb-compat \
    wispy \
    spectools \
    coexisyst \
))
include $(subdirs)
