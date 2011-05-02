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

#ifndef IPLINK_H
#define IPLINK_H

extern const char *ll_addr_n2a(unsigned char *addr, int alen, int type, char *buf, int blen);
extern int matches(const char *arg, const char *pattern);

extern int addattr32(struct nlmsghdr *n, int maxlen, int type, __u32 data);
extern int addattr_l(struct nlmsghdr *n, int maxlen, int type, const void *data, int alen);

extern void incomplete_command(void) __attribute__((noreturn));

#define NEXT_ARG() do { argv++; if (--argc <= 0) incomplete_command(); } while(0)
#define NEXT_ARG_OK() (argc - 1 > 0)
#define PREV_ARG() do { argv--; argc++; } while(0)


struct link_util {
	struct link_util *next;
	const char *id;
	int maxattr;
	int (*parse_opt)(struct link_util *, int, char **,
			struct nlmsghdr *);
	void (*print_opt)(struct link_util *, FILE *,
			struct rtattr *[]);
	void (*print_xstats)(struct link_util *, FILE *,
			struct rtattr *);
};

#endif
