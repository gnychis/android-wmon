#include <stdio.h>
#include <stdlib.h>
#include <pcap.h>
#include <android/log.h>
#include <jni.h>
#define LOG_TAG "PcapDriver" // text for log tag 

jint 
Java_com_gnychis_coexisyst_CoexiSyst_pcapGetInterfaces( JNIEnv* env, jobject thiz )
{
	pcap_if_t *alldevs;
	pcap_if_t *d;
	char errbuf[PCAP_ERRBUF_SIZE+1];

	if (pcap_findalldevs(&alldevs, errbuf) == -1) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "error finding pcap devices: %s",
			errbuf);
		return -1;
	}

	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Network interfaces:");
	for(d=alldevs;d;d=d->next) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "   %s", d->name);
	}		
	return 1;
}
