#include <stdio.h>
#include <stdlib.h>
#include <pcap.h>
#include <jni.h>
#include <unistd.h>
#define LOG_TAG "PcapDriver" // text for log tag 

int main ()
{
	pcap_if_t *alldevs;
	pcap_if_t *d;
	char errbuf[PCAP_ERRBUF_SIZE+1];
	int status;


	if (pcap_findalldevs(&alldevs, errbuf) == -1) {
		printf("error finding pcap devices: %s\n", errbuf);
		return -1;
	}

	printf("Network interfaces:\n");
	for(d=alldevs;d;d=d->next) {
		printf("   %s\n", d->name);
	}		
	return 1;
}
