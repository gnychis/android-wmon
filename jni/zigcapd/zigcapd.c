#include <errno.h>
#include <termios.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include "serial.h"

#define CHANGE_CHAN 0x0000
#define TRANSMIT_PACKET 0x0001
#define RECEIVED_PACKET 0x0002

int set_channel(int channel);
void init_econotag();
	
#define portname "/dev/ttyUSB1"
int fd;

int main() {

	init_econotag();

	if(set_channel(3))
		printf("Success!\n");
	else
		printf("Fail!\n");

	return 1;

}

int set_channel(int channel) {
	char cmd = CHANGE_CHAN;
	char chan = (char) channel;
	char rval;

	write (fd, &cmd, 1); 
	write (fd, &chan, 1);
	
	read (fd, &rval, 1);  // read up to 100 characters if ready to read

	if(rval==chan)
		return 1;
	else
		return 0;
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

