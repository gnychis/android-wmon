LOCAL_PATH := $(call my-dir)
subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
    libbtbb \
    libglib \
    libgpg-error \
    libgcrypt \
    libusb \
    libusb-compat \
    wispy \
    spectools \
    standalone \
    libpcap \
		pcapd\
		jnetpcap\
		libnl \
    libtshark \
    libwireshark \
		lowpan \
    awmon \
		wireless-tools \
		iw \
    ubertooth \
))
include $(subdirs)
