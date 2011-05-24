#include <stdio.h>
#include <stdlib.h>
#include <pcap.h>
#include <jni.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <android/log.h>
#include <sys/types.h>
#define LOG_TAG "PcapDriver" // text for log tag 
#define PCAP_DUMP 1
//#define VERBOSE 1

struct ieee80211_radiotap_header {
        u_int8_t        it_version;     /* set to 0 */
        u_int8_t        it_pad;
        u_int16_t       it_len;         /* entire length */
        u_int32_t       it_present;     /* fields present */
} __attribute__((__packed__));

int main (int argc, char *argv[])
{
	// TCP Server related variables
	int sd, sd_current, cc, fromlen, tolen;
	int addrlen;
	struct sockaddr_in sin, pin;

	// Pcap related variables
	pcap_if_t *alldevs;
	pcap_if_t *d;
	char errbuf[PCAP_ERRBUF_SIZE+1];
	int status;
	pcap_t *handle;
	struct pcap_pkthdr header;
	const u_char *packet;
	pcap_dumper_t *pDump;

#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Size of header: %d\n", sizeof(struct pcap_pkthdr));
#endif

	if(argc<3)
		return -1;

	if (pcap_findalldevs(&alldevs, errbuf) == -1) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "error finding pcap devices: %s\n", errbuf);
		return -1;
	}

#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Network interfaces:\n");
	for(d=alldevs;d;d=d->next) {
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "   %s\n", d->name);
	}
#endif

	handle = pcap_open_live(argv[1], 1500, 1, 1000, errbuf);
	if(handle == NULL) {
		fprintf(stderr, "Couldn't open device %s: %s\n", argv[1], errbuf);
		return -1;
	}

#ifdef PCAP_DUMP
	// Create a dump file
	pDump = pcap_dump_open(handle, "/sdcard/pcapd.pcap");
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Opened pcap dump");
#endif

	// Only open a server if we get this far.
	if ((sd = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
		perror("socket error");
		return -1;
	}

	// Listen on the user specified port
	memset(&sin, 0, sizeof(sin));
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = INADDR_ANY;
	sin.sin_port = htons(atoi(argv[2]));

	// Bind to the port number
	if(bind(sd, (struct sockaddr *) &sin, sizeof(sin)) == -1) {
		perror("error trying to bind");
		return -1;
	}

	// Open a port on the user specified port
	if(listen(sd, 0) ==-1) {
		perror("error trying to listen on socket");
		return -1;
	}
	
	addrlen = sizeof(pin);
	if ((sd_current = accept(sd, (struct sockaddr *) &pin, &addrlen)) == -1) {
		perror("error trying to accept client");
		return -1;
	}

#ifdef VERBOSE
	__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Accepted connection\n");
#endif

	while(1) {
		int k,wrote,total,z;
		struct ieee80211_radiotap_header *rth;
		packet = pcap_next(handle, &header);
#ifdef PCAP_DUMP
		pcap_dump((u_char *) pDump, &header, packet);
#endif
		if(packet == NULL) {
			fprintf(stderr, "Error trying to read packet");
			break;
		}

		// First write the pcap header
		total = 0;
		while(total < sizeof(struct pcap_pkthdr)) {
			if((wrote = send(sd_current, (char *) &header + total, sizeof(struct pcap_pkthdr)-total, 0)) == -1) {
				perror("error trying to send header over");
				return -1;
			}
			total += wrote;
		}

#ifdef VERBOSE
		// print out some information
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Got packet size of: %d, caplen: %d", header.len, header.caplen);
		rth = (struct ieee80211_radiotap_header *) packet;
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "RadioTap... Version: %d, Pad: %d, Len: %d, Fields: %d", 
																rth->it_version, rth->it_pad, rth->it_len, rth->it_present);
		__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "Datalink Type: %d, Name: %s", pcap_datalink(handle), pcap_datalink_val_to_name(pcap_datalink(handle)));
		for(z = 0; z<10; z++)
			__android_log_print(ANDROID_LOG_INFO, LOG_TAG, "data[%d]: 0x%x", z, packet[z]);
#endif

		// Now write the pcap packet
		total = 0;
		while(total < header.len) {
			if((wrote = send(sd_current, packet + total, header.len-total, 0)) == -1) {
				perror("error trying to send packet over");
				return -1;
			}
			total += wrote;
		}
	}
	
	pcap_close(handle);

	return 1;
}
