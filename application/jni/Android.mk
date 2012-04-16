LOCAL_PATH := $(call my-dir)
subdirs := $(addprefix $(LOCAL_PATH)/,$(addsuffix /Android.mk, \
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
    coexisyst \
		wireless-tools \
		iw \
))
include $(subdirs)
