LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

#LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES:= \
	iw.c \
	genl.c \
	event.c \
	info.c \
	phy.c \
	interface.c \
	ibss.c \
	station.c \
	survey.c \
	util.c \
	mesh.c \
	mpath.c \
	scan.c \
	reg.c \
	version.c \
	reason.c \
	status.c \
	connect.c \
	link.c \
	offch.c \
	ps.c \
	cqm.c \
	bitrate.c \
	wowlan.c \
	roc.c \
	sections.c



LOCAL_C_INCLUDES += $(LOCAL_PATH)/../lowpan/include \
	$(LOCAL_PATH)/../libnl/include
	

LOCAL_CFLAGS +=  -g
LOCAL_CFLAGS += -fPIC -DPIC
LOCAL_LDLIBS := -Wl,--no-gc-sections  

LOCAL_STATIC_LIBRARIES := libnls libnl

ifeq ($(TARGET_BUILD_TYPE),release)
	LOCAL_CFLAGS += -g
endif

LOCAL_MODULE:= iw

include $(BUILD_EXECUTABLE)
