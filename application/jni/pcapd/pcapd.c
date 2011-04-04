#include <stdio.h>
#include <stdlib.h>
#include <pcap.h>
#include <jni.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/types.h>
#define LOG_TAG "PcapDriver" // text for log tag 

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

	if(argc<3)
		return -1;

	if (pcap_findalldevs(&alldevs, errbuf) == -1) {
		printf("error finding pcap devices: %s\n", errbuf);
		return -1;
	}

	printf("Network interfaces:\n");
	for(d=alldevs;d;d=d->next) {
		printf("   %s\n", d->name);
	}

	handle = pcap_open_live(argv[1], 0, 1, 1000, errbuf);
	if(handle == NULL) {
		fprintf(stderr, "Couldn't open device %s: %s\n", argv[1], errbuf);
		return -1;
	}

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

	while(1) {
		packet = pcap_next(handle, &header);
		if(packet == NULL) {
			fprintf(stderr, "Error trying to read packet");
			break;
		}
		printf("Got a packet with length [%d]\n", header.len);
	}
	
	pcap_close(handle);

	return 1;
}
