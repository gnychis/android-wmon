#include <errno.h>
#include <stdlib.h>
#include <termios.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <stdint.h>
#include <limits.h>
#include "serial.h"

#define CHANGE_CHAN 0x0000
#define TRANSMIT_PACKET 0x0001
#define RECEIVED_PACKET 0x0002

int set_channel(int channel);
void init_econotag();
	
#define portname "/dev/ttyUSB1"
int fd;

char block_read1() {
	int n;
	char c;

	while(1) {
		if((n = read(fd, &c, 1))==1)
			return c;
	}
}

uint32_t block_read_uint32() {
	int i;
	uint32_t v = 0;

	for(i=0;i<4;i++) {
		uint32_t t = ((uint32_t)block_read1()) & 0xff;
		printf("t(%d): 0x%x\n", i, t);
		v = v | (t << i*CHAR_BIT);
	}

	printf("v: 0x%x\n", v);

	return v;
}

void block_read_nbytes(char *buf, int nbytes) {
	int nread=0;
	while(nread<nbytes) {
		int n = read(fd, buf+nread, nbytes-nread);
		nread += n;
	}
}

int main() {
	int n;
  char cmd;

	init_econotag();

	set_channel(1);

	// Keep reading in for commands
	while(1) {

		if((n = read(fd, &cmd, 1))==1) {
			
			if(cmd==RECEIVED_PACKET) {
				uint8_t lqi = (uint8_t)block_read1();
				uint32_t rxtime = block_read_uint32();
				uint8_t length = (uint8_t)block_read1();
				char *buf = malloc(length);
				block_read_nbytes(buf, length);
				printf("rxtime: %d, LQI: %d, length: %d\n", rxtime, lqi, length);
			}

		}
	}

	return 1;

}

int set_channel(int channel) {
	char cmd = CHANGE_CHAN;
	char chan = (char) channel;
	//char rval;

	write (fd, &cmd, 1); 
	write (fd, &chan, 1);
	
	// read back value for testing
	/*read (fd, &rval, 1); 

	if(rval==chan)
		return 1;
	else
		return 0;*/
	return 1;
}

void init_econotag() {
	fd = open (portname, O_RDWR | O_NOCTTY | O_SYNC);

	if (fd < 0)
	{
		fprintf (stderr, "error %d opening %s", errno, portname);
		return;
	}

	set_interface_attribs (fd, B115200, 0);  // set speed to 115,200 bps, 8n1 (no parity)
	set_blocking (fd, 0);                // set no blocking
}

