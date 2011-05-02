/*
 * Copyright (C) 2009 Siemens AG
 *
 * Written-by: Dmitry Eremin-Solenikov
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

#include <stdint.h>
#include <stdlib.h>
#include <ieee802154.h>

#include <netlink/netlink.h>
#include <linux/rtnetlink.h>
#include <linux/mac802154.h>
#include "iplink.h"

static void explain(void)
{
	fprintf(stderr,
		"Usage: ... wpan phy PHY\n"
	       );
}

static void wpan_print_opt(struct link_util *lu, FILE *f,
			struct rtattr *tb[])
{
	if (!tb)
		return;

	if (tb[IFLA_WPAN_PHY]) {
		char *phy = RTA_DATA(tb[IFLA_WPAN_PHY]);
		phy[RTA_PAYLOAD(tb[IFLA_WPAN_PHY]) - 1] = '\0';
		fprintf(f, "phy %s ", phy);
	}

	if (tb[IFLA_WPAN_PAGE]) {
		uint8_t *page = RTA_DATA(tb[IFLA_WPAN_PAGE]);
		fprintf(f, "page %d ", *page);
	}

	if (tb[IFLA_WPAN_CHANNEL]) {
		uint16_t *chan = RTA_DATA(tb[IFLA_WPAN_CHANNEL]);
		fprintf(f, "chan %d ", *chan);
	}

	if (tb[IFLA_WPAN_PAN_ID]) {
		uint16_t *panid = RTA_DATA(tb[IFLA_WPAN_PAN_ID]);
		fprintf(f, "pan %04x ", *panid);
	}

	if (tb[IFLA_WPAN_SHORT_ADDR]) {
		uint16_t *addr = RTA_DATA(tb[IFLA_WPAN_SHORT_ADDR]);
		fprintf(f, "addr %04x ", *addr);
	}

	if (tb[IFLA_WPAN_COORD_SHORT_ADDR]) {
		uint16_t *coord = RTA_DATA(tb[IFLA_WPAN_COORD_SHORT_ADDR]);
		fprintf(f, "coord %04x ", *coord);
	}

	if (tb[IFLA_WPAN_COORD_EXT_ADDR]) {
		char buf[24];
		fprintf(f, "%s\n", ll_addr_n2a(
				RTA_DATA(tb[IFLA_WPAN_COORD_EXT_ADDR]),
				RTA_PAYLOAD(tb[IFLA_WPAN_COORD_EXT_ADDR]),
				ARPHRD_IEEE802154, buf, sizeof(buf)));
	}
}

static int wpan_parse_opt(struct link_util *lu, int argc, char **argv,
			  struct nlmsghdr *n)
{
	while (argc > 0) {
		if (matches(*argv, "phy") == 0) {
			NEXT_ARG();
			addattr_l(n, 1024, IFLA_WPAN_PHY, *argv, strlen(*argv) + 1);
		} else if (matches(*argv, "help") == 0) {
			explain();
			return -1;
		} else {
			fprintf(stderr, "wpan: what is \"%s\"?\n", *argv);
			explain();
			return -1;
		}
		argc--; argv++;
	}

	return 0;
}

struct link_util wpan_link_util = {
	.id		= "wpan",
	.maxattr	= IFLA_WPAN_MAX,
	.print_opt	= wpan_print_opt,
	.parse_opt	= wpan_parse_opt,
};

