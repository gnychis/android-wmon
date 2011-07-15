/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_jnetpcap_Pcap */

#ifndef _Included_org_jnetpcap_Pcap
#define _Included_org_jnetpcap_Pcap
#ifdef __cplusplus
extern "C" {
#endif
#undef org_jnetpcap_Pcap_DEFAULT_JPACKET_BUFFER_SIZE
#define org_jnetpcap_Pcap_DEFAULT_JPACKET_BUFFER_SIZE 1048576L
#undef org_jnetpcap_Pcap_DEFAULT_PROMISC
#define org_jnetpcap_Pcap_DEFAULT_PROMISC 1L
#undef org_jnetpcap_Pcap_DEFAULT_SNAPLEN
#define org_jnetpcap_Pcap_DEFAULT_SNAPLEN 65536L
#undef org_jnetpcap_Pcap_DEFAULT_TIMEOUT
#define org_jnetpcap_Pcap_DEFAULT_TIMEOUT 0L
#undef org_jnetpcap_Pcap_DISPATCH_BUFFER_FULL
#define org_jnetpcap_Pcap_DISPATCH_BUFFER_FULL -1L
#undef org_jnetpcap_Pcap_ERROR
#define org_jnetpcap_Pcap_ERROR -1L
#undef org_jnetpcap_Pcap_ERROR_ACTIVATED
#define org_jnetpcap_Pcap_ERROR_ACTIVATED -4L
#undef org_jnetpcap_Pcap_ERROR_BREAK
#define org_jnetpcap_Pcap_ERROR_BREAK -2L
#undef org_jnetpcap_Pcap_ERROR_IFACE_NOT_UP
#define org_jnetpcap_Pcap_ERROR_IFACE_NOT_UP -9L
#undef org_jnetpcap_Pcap_ERROR_NO_SUCH_DEVICE
#define org_jnetpcap_Pcap_ERROR_NO_SUCH_DEVICE -5L
#undef org_jnetpcap_Pcap_ERROR_NOT_ACTIVATED
#define org_jnetpcap_Pcap_ERROR_NOT_ACTIVATED -3L
#undef org_jnetpcap_Pcap_ERROR_NOT_RFMON
#define org_jnetpcap_Pcap_ERROR_NOT_RFMON -7L
#undef org_jnetpcap_Pcap_ERROR_PERM_DENIED
#define org_jnetpcap_Pcap_ERROR_PERM_DENIED -8L
#undef org_jnetpcap_Pcap_ERROR_RFMON_NOTSUP
#define org_jnetpcap_Pcap_ERROR_RFMON_NOTSUP -6L
#undef org_jnetpcap_Pcap_IN
#define org_jnetpcap_Pcap_IN 1L
#undef org_jnetpcap_Pcap_INOUT
#define org_jnetpcap_Pcap_INOUT 0L
/* Inaccessible static: LIBRARY_LOAD_STATUS */
#undef org_jnetpcap_Pcap_LOOP_INFINATE
#define org_jnetpcap_Pcap_LOOP_INFINATE -1L
#undef org_jnetpcap_Pcap_LOOP_INFINITE
#define org_jnetpcap_Pcap_LOOP_INFINITE -1L
#undef org_jnetpcap_Pcap_LOOP_INTERRUPTED
#define org_jnetpcap_Pcap_LOOP_INTERRUPTED -2L
#undef org_jnetpcap_Pcap_MODE_BLOCKING
#define org_jnetpcap_Pcap_MODE_BLOCKING 0L
#undef org_jnetpcap_Pcap_MODE_NON_BLOCKING
#define org_jnetpcap_Pcap_MODE_NON_BLOCKING 1L
#undef org_jnetpcap_Pcap_MODE_NON_PROMISCUOUS
#define org_jnetpcap_Pcap_MODE_NON_PROMISCUOUS 0L
#undef org_jnetpcap_Pcap_MODE_PROMISCUOUS
#define org_jnetpcap_Pcap_MODE_PROMISCUOUS 1L
#undef org_jnetpcap_Pcap_NEXT_EX_EOF
#define org_jnetpcap_Pcap_NEXT_EX_EOF -2L
#undef org_jnetpcap_Pcap_NEXT_EX_NOT_OK
#define org_jnetpcap_Pcap_NEXT_EX_NOT_OK -1L
#undef org_jnetpcap_Pcap_NEXT_EX_OK
#define org_jnetpcap_Pcap_NEXT_EX_OK 1L
#undef org_jnetpcap_Pcap_NEXT_EX_TIMEDOUT
#define org_jnetpcap_Pcap_NEXT_EX_TIMEDOUT 0L
#undef org_jnetpcap_Pcap_NOT_OK
#define org_jnetpcap_Pcap_NOT_OK -1L
#undef org_jnetpcap_Pcap_OK
#define org_jnetpcap_Pcap_OK 0L
#undef org_jnetpcap_Pcap_OUT
#define org_jnetpcap_Pcap_OUT 2L
/* Inaccessible static: PCAP100_LOAD_STATUS */
#undef org_jnetpcap_Pcap_WARNING
#define org_jnetpcap_Pcap_WARNING 1L
#undef org_jnetpcap_Pcap_WARNING_PROMISC_NOT_SUP
#define org_jnetpcap_Pcap_WARNING_PROMISC_NOT_SUP 2L
/*
 * Class:     org_jnetpcap_Pcap
 * Method:    compileNoPcap
 * Signature: (IILorg/jnetpcap/PcapBpfProgram;Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_compileNoPcap
  (JNIEnv *, jclass, jint, jint, jobject, jstring, jint, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    create
 * Signature: (Ljava/lang/String;Ljava/lang/StringBuilder;)Lorg/jnetpcap/Pcap;
 */
JNIEXPORT jobject JNICALL Java_org_jnetpcap_Pcap_create
  (JNIEnv *, jclass, jstring, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    datalinkNameToVal
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_datalinkNameToVal
  (JNIEnv *, jclass, jstring);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    datalinkValToDescription
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jnetpcap_Pcap_datalinkValToDescription
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    datalinkValToName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jnetpcap_Pcap_datalinkValToName
  (JNIEnv *, jclass, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    findAllDevs
 * Signature: (Ljava/util/List;Ljava/lang/StringBuilder;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_findAllDevs
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    freecode
 * Signature: (Lorg/jnetpcap/PcapBpfProgram;)V
 */
JNIEXPORT void JNICALL Java_org_jnetpcap_Pcap_freecode
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jnetpcap_Pcap_initIDs
  (JNIEnv *, jclass);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    isCreateSupported
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_jnetpcap_Pcap_isCreateSupported
  (JNIEnv *, jclass);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    isInjectSupported
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_jnetpcap_Pcap_isInjectSupported
  (JNIEnv *, jclass);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    isSendPacketSupported
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_org_jnetpcap_Pcap_isSendPacketSupported
  (JNIEnv *, jclass);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    libVersion
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jnetpcap_Pcap_libVersion
  (JNIEnv *, jclass);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    lookupDev
 * Signature: (Ljava/lang/StringBuilder;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jnetpcap_Pcap_lookupDev
  (JNIEnv *, jclass, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    lookupNet
 * Signature: (Ljava/lang/String;Lorg/jnetpcap/nio/JNumber;Lorg/jnetpcap/nio/JNumber;Ljava/lang/StringBuilder;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_lookupNet__Ljava_lang_String_2Lorg_jnetpcap_nio_JNumber_2Lorg_jnetpcap_nio_JNumber_2Ljava_lang_StringBuilder_2
  (JNIEnv *, jclass, jstring, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    lookupNet
 * Signature: (Ljava/lang/String;Lorg/jnetpcap/PcapInteger;Lorg/jnetpcap/PcapInteger;Ljava/lang/StringBuilder;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_lookupNet__Ljava_lang_String_2Lorg_jnetpcap_PcapInteger_2Lorg_jnetpcap_PcapInteger_2Ljava_lang_StringBuilder_2
  (JNIEnv *, jclass, jstring, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    openDead
 * Signature: (II)Lorg/jnetpcap/Pcap;
 */
JNIEXPORT jobject JNICALL Java_org_jnetpcap_Pcap_openDead
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    openLive
 * Signature: (Ljava/lang/String;IIILjava/lang/StringBuilder;)Lorg/jnetpcap/Pcap;
 */
JNIEXPORT jobject JNICALL Java_org_jnetpcap_Pcap_openLive
  (JNIEnv *, jclass, jstring, jint, jint, jint, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    openOffline
 * Signature: (Ljava/lang/String;Ljava/lang/StringBuilder;)Lorg/jnetpcap/Pcap;
 */
JNIEXPORT jobject JNICALL Java_org_jnetpcap_Pcap_openOffline
  (JNIEnv *, jclass, jstring, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    activate
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_activate
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    breakloop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jnetpcap_Pcap_breakloop
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    canSetRfmon
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_canSetRfmon
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    checkIsActive
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jnetpcap_Pcap_checkIsActive
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_jnetpcap_Pcap_close
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    compile
 * Signature: (Lorg/jnetpcap/PcapBpfProgram;Ljava/lang/String;II)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_compile
  (JNIEnv *, jobject, jobject, jstring, jint, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    datalink
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_datalink
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    dispatch
 * Signature: (ILorg/jnetpcap/ByteBufferHandler;Ljava/lang/Object;Lorg/jnetpcap/PcapHeader;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_dispatch__ILorg_jnetpcap_ByteBufferHandler_2Ljava_lang_Object_2Lorg_jnetpcap_PcapHeader_2
  (JNIEnv *, jobject, jint, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    dispatch
 * Signature: (IILorg/jnetpcap/packet/JPacketHandler;Ljava/lang/Object;Lorg/jnetpcap/packet/JPacket;Lorg/jnetpcap/packet/JPacket$State;Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/packet/JScanner;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_dispatch__IILorg_jnetpcap_packet_JPacketHandler_2Ljava_lang_Object_2Lorg_jnetpcap_packet_JPacket_2Lorg_jnetpcap_packet_JPacket_00024State_2Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_packet_JScanner_2
  (JNIEnv *, jobject, jint, jint, jobject, jobject, jobject, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    dispatch
 * Signature: (IILorg/jnetpcap/packet/PcapPacketHandler;Ljava/lang/Object;Lorg/jnetpcap/packet/JPacket;Lorg/jnetpcap/packet/JPacket$State;Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/packet/JScanner;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_dispatch__IILorg_jnetpcap_packet_PcapPacketHandler_2Ljava_lang_Object_2Lorg_jnetpcap_packet_JPacket_2Lorg_jnetpcap_packet_JPacket_00024State_2Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_packet_JScanner_2
  (JNIEnv *, jobject, jint, jint, jobject, jobject, jobject, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    dispatch
 * Signature: (ILorg/jnetpcap/JBufferHandler;Ljava/lang/Object;Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/nio/JBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_dispatch__ILorg_jnetpcap_JBufferHandler_2Ljava_lang_Object_2Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_nio_JBuffer_2
  (JNIEnv *, jobject, jint, jobject, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    dispatch
 * Signature: (ILorg/jnetpcap/PcapDumper;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_dispatch__ILorg_jnetpcap_PcapDumper_2
  (JNIEnv *, jobject, jint, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    dispatch
 * Signature: (ILorg/jnetpcap/PcapHandler;Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_dispatch__ILorg_jnetpcap_PcapHandler_2Ljava_lang_Object_2
  (JNIEnv *, jobject, jint, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    dumpOpen
 * Signature: (Ljava/lang/String;)Lorg/jnetpcap/PcapDumper;
 */
JNIEXPORT jobject JNICALL Java_org_jnetpcap_Pcap_dumpOpen
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    getErr
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_jnetpcap_Pcap_getErr
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    getNonBlock
 * Signature: (Ljava/lang/StringBuilder;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_getNonBlock
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    inject
 * Signature: (Lorg/jnetpcap/nio/JBuffer;II)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_inject
  (JNIEnv *, jobject, jobject, jint, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    injectPrivate
 * Signature: (Ljava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_injectPrivate
  (JNIEnv *, jobject, jobject, jint, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    isSwapped
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_isSwapped
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    loop
 * Signature: (ILorg/jnetpcap/ByteBufferHandler;Ljava/lang/Object;Lorg/jnetpcap/PcapHeader;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_loop__ILorg_jnetpcap_ByteBufferHandler_2Ljava_lang_Object_2Lorg_jnetpcap_PcapHeader_2
  (JNIEnv *, jobject, jint, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    loop
 * Signature: (IILorg/jnetpcap/packet/JPacketHandler;Ljava/lang/Object;Lorg/jnetpcap/packet/JPacket;Lorg/jnetpcap/packet/JPacket$State;Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/packet/JScanner;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_loop__IILorg_jnetpcap_packet_JPacketHandler_2Ljava_lang_Object_2Lorg_jnetpcap_packet_JPacket_2Lorg_jnetpcap_packet_JPacket_00024State_2Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_packet_JScanner_2
  (JNIEnv *, jobject, jint, jint, jobject, jobject, jobject, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    loop
 * Signature: (IILorg/jnetpcap/packet/PcapPacketHandler;Ljava/lang/Object;Lorg/jnetpcap/packet/JPacket;Lorg/jnetpcap/packet/JPacket$State;Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/packet/JScanner;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_loop__IILorg_jnetpcap_packet_PcapPacketHandler_2Ljava_lang_Object_2Lorg_jnetpcap_packet_JPacket_2Lorg_jnetpcap_packet_JPacket_00024State_2Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_packet_JScanner_2
  (JNIEnv *, jobject, jint, jint, jobject, jobject, jobject, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    loop
 * Signature: (ILorg/jnetpcap/JBufferHandler;Ljava/lang/Object;Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/nio/JBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_loop__ILorg_jnetpcap_JBufferHandler_2Ljava_lang_Object_2Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_nio_JBuffer_2
  (JNIEnv *, jobject, jint, jobject, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    loop
 * Signature: (ILorg/jnetpcap/PcapDumper;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_loop__ILorg_jnetpcap_PcapDumper_2
  (JNIEnv *, jobject, jint, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    loop
 * Signature: (ILorg/jnetpcap/PcapHandler;Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_loop__ILorg_jnetpcap_PcapHandler_2Ljava_lang_Object_2
  (JNIEnv *, jobject, jint, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    majorVersion
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_majorVersion
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    minorVersion
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_minorVersion
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    next
 * Signature: (Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/nio/JBuffer;)Lorg/jnetpcap/nio/JBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_jnetpcap_Pcap_next__Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_nio_JBuffer_2
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    next
 * Signature: (Lorg/jnetpcap/PcapPktHdr;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_org_jnetpcap_Pcap_next__Lorg_jnetpcap_PcapPktHdr_2
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    nextEx
 * Signature: (Lorg/jnetpcap/PcapHeader;Lorg/jnetpcap/nio/JBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_nextEx__Lorg_jnetpcap_PcapHeader_2Lorg_jnetpcap_nio_JBuffer_2
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    nextEx
 * Signature: (Lorg/jnetpcap/PcapPktHdr;Lorg/jnetpcap/PcapPktBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_nextEx__Lorg_jnetpcap_PcapPktHdr_2Lorg_jnetpcap_PcapPktBuffer_2
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    sendPacket
 * Signature: (Lorg/jnetpcap/nio/JBuffer;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_sendPacket
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    sendPacketPrivate
 * Signature: (Ljava/nio/ByteBuffer;II)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_sendPacketPrivate
  (JNIEnv *, jobject, jobject, jint, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setBufferSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setBufferSize
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setDatalink
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setDatalink
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setDirection
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setDirection
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setFilter
 * Signature: (Lorg/jnetpcap/PcapBpfProgram;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setFilter
  (JNIEnv *, jobject, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setNonBlock
 * Signature: (ILjava/lang/StringBuilder;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setNonBlock
  (JNIEnv *, jobject, jint, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setPromisc
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setPromisc
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setRfmon
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setRfmon
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setSnaplen
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setSnaplen
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    setTimeout
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_setTimeout
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    snapshot
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_snapshot
  (JNIEnv *, jobject);

/*
 * Class:     org_jnetpcap_Pcap
 * Method:    stats
 * Signature: (Lorg/jnetpcap/PcapStat;)I
 */
JNIEXPORT jint JNICALL Java_org_jnetpcap_Pcap_stats
  (JNIEnv *, jobject, jobject);

#ifdef __cplusplus
}
#endif
#endif