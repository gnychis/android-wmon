LOCAL_PATH := $(call my-dir)
subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
    libglib \
    libusb \
    libusb-compat \
    wispy \
    spectools \
    standalone \
    libpcap \
		pcapd\
		jnetpcap\
    coexisyst \
))
include $(subdirs)
