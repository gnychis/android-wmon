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

#include <sys/select.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <ieee802154.h>
#include <libcommon.h>

int main(int argc, char **argv) {
	int sd;
	int ret;
	struct sockaddr_ieee802154 a;

	if (argv[1] && !strcmp(argv[1], "--version")) {
		printf(	"izchat " VERSION "\n"
			"Copyright (C) 2008, 2009 by Siemens AG\n"
			"License GPLv2 GNU GPL version 2 <http://gnu.org/licenses/gpl.html>.\n"
			"This is free software: you are free to change and redistribute it.\n"
			"There is NO WARRANTY, to the extent permitted by law.\n"
			"\n"
			"Written by Dmitry Eremin-Solenikov, Sergey Lapin and Maxim Osipov\n");
		return 0;
	}

	if (!argv[1] || !strcmp(argv[1], "--help") || argc != 4) {
		printf("Usage: %s PANid sourceAddr destAddr\n", argv[0]);
		printf("  or:  %s --help\n", argv[0]);
		printf("  or:  %s --version\n", argv[0]);
		printf("\n\nStarts chat session on net PANid, using addr sourceAddr talking with destAddr.\n");
		printf("\nCurrently all addresses should be short (16-bit) and specified in hex form.\n");
		printf("Report bugs to " PACKAGE_BUGREPORT "\n\n");
		printf(PACKAGE_NAME " homepage <" PACKAGE_URL ">\n");
		return 0;
	}

	sd = socket(PF_IEEE802154, SOCK_DGRAM, 0);
	if (sd < 0) {
		perror("socket");
		return 1;
	}

	a.family = AF_IEEE802154;
	a.addr.addr_type = IEEE802154_ADDR_SHORT;

	a.addr.pan_id = strtol(argv[1], NULL, 16);

	a.addr.short_addr = (strtol(argv[2], NULL, 16));
	ret = bind(sd, (struct sockaddr *)&a, sizeof(a));
	if (ret) {
		perror("bind");
		return 1;
	}

	a.addr.short_addr = (strtol(argv[3], NULL, 16));
	ret = connect(sd, (struct sockaddr *)&a, sizeof(a));
	if (ret) {
		perror("connect");
		return 1;
	}

	while (1) {
		fd_set rs, ws, xs;
		char buf[80];
		int r, w;

		FD_ZERO(&rs);
		FD_ZERO(&ws);
		FD_ZERO(&xs);
		FD_SET(sd, &rs);
//		FD_SET(sd, &ws);
		FD_SET(sd, &xs);

		FD_SET(0, &rs);
//		FD_SET(1, &ws);

		ret = select(sd + 1, &rs, &ws, &xs, NULL);
		if (FD_ISSET(0, &rs)) {
			r = 0;
			w = sd;
		} else if (FD_ISSET(sd, &rs)) {
			r = sd;
			w = 1;
			ret = write(w, "> ", 2);
			if (ret < 0)
				perror("write");
		} else {
			printf("Something bad happened!\n");
			continue;
		}

		ret = read(r, &buf, sizeof(buf));
		if (ret < 0) {
			perror("read");
		} else if (ret > 0) {
			ret = write(w, &buf, ret);
			if (ret < 0)
				perror("write");
		}
	}
	return 0;
}
