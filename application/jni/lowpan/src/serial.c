/*
 * Linux IEEE 802.15.4 userspace tools
 *
 * Copyright (C) 2008, 2009 Siemens AG
 *
 * Written-by: Dmitry Eremin-Solenikov
 * Written-by: Sergey Lapin
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <fcntl.h>
#include <termios.h>
#include <unistd.h>
#include <errno.h>

#include <net/if.h>

#include "ieee802154.h"
#define N_ZB 19

int main(int argc, char **argv) {
	int fd, ret, s;

	if (argc == 1 || !strcmp(argv[1], "--version")) {
		printf(	"izattach " VERSION "\n"
			"Copyright (C) 2008, 2009 by Siemens AG\n"
			"License GPLv2 GNU GPL version 2 <http://gnu.org/licenses/gpl.html>.\n"
			"This is free software: you are free to change and redistribute it.\n"
			"There is NO WARRANTY, to the extent permitted by law.\n"
			"\n"
			"Written by Dmitry Eremin-Solenikov, Sergey Lapin and Maxim Osipov\n");
		return 0;
	}

	if (argc != 2 || (argc >= 1 && !strcmp(argv[1], "--help"))) {
		printf("Usage: %s SERIAL_DEV\n", argv[0]);
		printf("Attach serial devices via UART to IEEE 802.15.4/ZigBee stack\n\n");
		printf("  SERIAL_DEV  This specifies the serial device to attach.\n");
		printf("Report bugs to " PACKAGE_BUGREPORT "\n\n");
		printf(PACKAGE_NAME " homepage <" PACKAGE_URL ">\n");
		return 1;
	}

	fd = open(argv[1], O_RDWR | O_NOCTTY);
	if (fd < 0) {
		perror("open");
		return 2;
	}

	struct termios tbuf;

	memset(&tbuf, 0, sizeof(tbuf));

	tbuf.c_iflag |= IGNBRK;		/* Ignores break condition */
	tbuf.c_cc[VMIN] = 1;

	tbuf.c_cflag &= ~(CSIZE | PARODD | CSTOPB | CRTSCTS | PARENB);
	tbuf.c_cflag |= CLOCAL | CREAD | CS8;
	tbuf.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);
	tbuf.c_iflag &= ~(INPCK | ISTRIP);
	tbuf.c_oflag &= ~OPOST;
	tbuf.c_cc[VTIME] = 5; 
                                                                                  
	/*
	tbuf.c_cflag |= CLOCAL;
	tbuf.c_lflag = 0;
	tbuf.c_cc[0] = 0;
	tbuf.c_cc[1] = 0;
	tbuf.c_cc[2] = 0;
	tbuf.c_cc[3] = 0;
	tbuf.c_cc[4] = 0;
	tbuf.c_cc[5] = 0;
	tbuf.c_cc[6] = 0;
	tbuf.c_cc[7] = 0;
	tbuf.c_cc[VMIN] = 1;
	*/
	cfsetospeed(&tbuf, B115200);
	cfsetispeed(&tbuf, B115200);

	if (tcsetattr(fd, TCSANOW, &tbuf) < 0) {
		perror("tcsetattr");
		return 3;
	}
	memset(&tbuf, 0, sizeof(tbuf));
	if (tcgetattr(fd, &tbuf) < 0) {
		perror("tcgetattr");
		return 4;
	}
#if 0
	/* WTF?? */
	if (0 == (tbuf.c_cflag & CRTSCTS)) {
		fprintf(stderr, "failed to set CRTSCTS\n");
		return 5;
	}
#endif
	int arg;
	arg = N_IEEE802154;
	ret = ioctl(fd, TIOCSETD, &arg);
#ifdef ENABLE_KERNEL_COMPAT
	if (ret < 0 && errno == EINVAL) {
		arg = N_IEEE802154_OLD;
		ret = ioctl(fd, TIOCSETD, &arg);
	}
#endif
	if (ret < 0) {
		perror("ioctl: TIOCSETD");
		return 6;
	}

	s = socket(PF_IEEE802154, SOCK_RAW, 0);
	if (ret < 0) {
		perror("socket");
		return 7;
	}

	if (daemon(0, 0) < 0) {
		perror("daemon");
		exit(255);
	}

	while (1)
		select(0, NULL, NULL, NULL, NULL);

	close(fd);

	return 0;
}

