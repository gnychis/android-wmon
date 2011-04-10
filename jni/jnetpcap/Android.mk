LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := jnetpcap

LOCAL_SRC_FILES :=\
    jnetpcap.cpp\
    packet_flow.cpp\
    packet_jheader.cpp\
    jnetpcap_pcap_header.cpp\
    nio_jbuffer.cpp\
    winpcap_stat_ex.cpp\
    winpcap_send_queue.cpp\
    winpcap_ext.cpp\
    jnetpcap_ids.cpp\
    jnetpcap_dumper.cpp\
    jnetpcap_utils.cpp\
    util_in_cksum.cpp\
    jnetpcap_beta.cpp\
    nio_jmemory.cpp\
    packet_jsmall_scanner.cpp\
    packet_protocol.cpp\
    nio_jnumber.cpp\
    packet_jheader_scanner.cpp\
    library.cpp\
    packet_jscan.cpp\
    jnetpcap_pcap100.cpp\
    util_checksum.cpp\
    packet_jpacket.cpp\
    winpcap_ids.cpp\
    jnetpcap_bpf.cpp

LOCAL_C_INCLUDES += jni/libusb-compat/libusb jni/wispy jni/libpcap jni/jnetpcap/include

LOCAL_SHARED_LIBRARIES := libpcap

include $(BUILD_SHARED_LIBRARY)
