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

#include <arpa/inet.h>
#include <linux/sockios.h>
#include <net/if.h>
#include <netpacket/packet.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>

#include "ieee802154.h"
#include "libcommon.h"

int main(int argc, char **argv) {
	int ret;
	unsigned char buf[256];
	struct sockaddr_ll sa = {};
	struct ifreq req = {};
	char *iface = argv[1] ?: "wpan0";

	int sd = socket(PF_PACKET, (argv[2] && argv[2][0] == 'd') ? SOCK_DGRAM : SOCK_RAW , htons(ETH_P_ALL));
	if (sd < 0) {
		perror("socket");
		return 1;
	}

	strncpy(req.ifr_name, iface, IF_NAMESIZE);
	ret = ioctl(sd, SIOCGIFINDEX, &req);
	if (ret < 0)
		perror("ioctl: SIOCGIFINDEX");

	sa.sll_family = AF_PACKET;
	sa.sll_ifindex = req.ifr_ifindex;
	ret = bind(sd, (struct sockaddr*)&sa, sizeof(sa));
	if (ret < 0)
		perror("bind");

	do {
		struct sockaddr_storage sas;
		socklen_t saslen = sizeof(sas);
		ret = recvfrom(sd, buf, sizeof(buf), 0, (struct sockaddr *)&sas, &saslen);
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

