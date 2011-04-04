LOCAL_PATH := $(call my-dir)
subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
    libusb \
    libusb-compat \
    wispy \
    spectools \
    standalone \
    libpcap \
		pcapd\
    coexisyst \
))
include $(subdirs)
