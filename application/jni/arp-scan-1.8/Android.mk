LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS += -g -DDATADIR=\"/usr/local/share/arp-scan\" -DHAVE_CONFIG_H -Wshadow -Wwrite-strings -Wextra -fstack-protector -D_FORTIFY_SOURCE=2 -Wformat -Wformat-security
LOCAL_SRC_FILES:= arp-scan.c error.c wrappers.c utils.c hash.c obstack.c mt19937ar.c link-packet-socket.c strlcat.c strlcpy.c
LOCAL_MODULE := arp_scan
LOCAL_C_INCLUDES += jni/libpcap
LOCAL_SHARED_LIBRARIES := libc libpcap
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
include $(BUILD_EXECUTABLE)
