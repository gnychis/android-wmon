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

#include <sys/socket.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <net/if.h>

#include "ieee802154.h"
#include "libcommon.h"

int main(int argc, char **argv) {
	int ret;
	unsigned char buf[256];
	struct sockaddr_ieee802154 sa = {};
	struct ifreq req = {};
	char *iface = argv[1] ?: "wpan0";

	int sd = socket(PF_IEEE802154, SOCK_RAW, 0);
	if (sd < 0) {
		perror("socket");
		return 1;
	}

	strncpy(req.ifr_name, iface, IFNAMSIZ);
	ret = ioctl(sd, SIOCGIFHWADDR, &req);
	if (ret < 0)
		perror("ioctl: SIOCGIFHWADDR");

	sa.family = AF_IEEE802154;
	memcpy(&sa.addr.hwaddr, req.ifr_hwaddr.sa_data, sizeof(sa.addr.hwaddr));
	ret = bind(sd, (struct sockaddr*)&sa, sizeof(sa));
	if (ret < 0)
		perror("bind");

	ret = ioctl(sd, SIOCGIFADDR, &req);
	do {
		ret = recv(sd, buf, sizeof(buf), 0);
		if (ret < 0)
			perror("recv");
		else {
			printf("packet len %d (%x)\n", ret, ret);
			printbuf(buf, ret);
		}
	} while (ret >= 0);

	ret = shutdown(sd, SHUT_RDWR);
	if (ret < 0)
		perror("shutdown");

	ret = close(sd);
	if (ret < 0)
		perror("close");

	return 0;

}

