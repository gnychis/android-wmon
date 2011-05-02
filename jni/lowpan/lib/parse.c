/*
 * Linux IEEE 802.15.4 userspace tools
 *
 * Copyright (C) 2008, 2009 Sismens AG
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

#include <libcommon.h>
#include <errno.h>
#include <stdio.h>

int parse_hw_addr(const char *hw, unsigned char *buf) {
	int i = 0;

	while (*hw) {
		unsigned char c = *(hw++);
		switch (c) {
			case '0'...'9':
				c -= '0';
				break;
			case 'a'...'f':
				c -= 'a' - 10;
				break;
			case 'A'...'F':
				c -= 'A' - 10;
				break;
			case ':':
			case '.':
				continue;
			default:
				fprintf(stderr, "Bad HW address encountered (%c)\n", c);
				return -EINVAL;
		}
		buf[i / 2] = (buf[i/2] & (0xf << (4 * (i % 2)))) | (c << 4 * (1 -i % 2));

		i++;
		if (i == 16)
			return 0;
	}

	return -EINVAL;
}

