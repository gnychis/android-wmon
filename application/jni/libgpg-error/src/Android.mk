LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	code-from-errno.c \
	strsource-sym.c \
	gpg-error.c \
	strerror.c \
	code-to-errno.c \
	versioninfo.rc \
	strerror-sym.c \
	init.c \
	strsource.c


LOCAL_SHARED_LIBRARIES := \
			libc \

PACKAGE_NAME := libgpg-error

LOCAL_CFLAGS += -DHAVE_CONFIG_H 

LOCAL_C_INCLUDES:= \
        $(LOCAL_PATH)/.. 


LOCAL_MODULE:= libgpg-error


####################################
# These .h files are generated.
err-sources-sym.h:
	cd $(LOCAL_PATH) && $(MAKE) $@

code-from-errno.h: 
	cd $(LOCAL_PATH) && $(MAKE) $@

errnos-sym.h: 
	cd $(LOCAL_PATH) && $(MAKE) $@

gpg-error.h:
	cd $(LOCAL_PATH) && $(MAKE) $@

err-codes.h:
	cd $(LOCAL_PATH) && $(MAKE) $@

code-to-errno.h:
	cd $(LOCAL_PATH) && $(MAKE) $@

err-codes-sym.h:
	cd $(LOCAL_PATH) && $(MAKE) $@

err-sources.h:
	cd $(LOCAL_PATH) && $(MAKE) $@

# override files target so the header files get generated	
# files: err-sources-sym.h code-from-errno.h errnos-sym.h gpg-error.h err-codes.h code-to-errno.h err-codes-sym.h err-sources.h 

include $(BUILD_SHARED_LIBRARY)
