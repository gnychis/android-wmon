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

#include <stdio.h>

#include <libcommon.h>

void printbuf(const unsigned char *buf, int len) {
	char outbuf[1024], outbuf2[1024];
	int olen = 0, olen2 = 0;
	int i;
	for (i = 0; i < len; ) {
		if (i % 8 == 0) {
			olen = olen2 = 0;
			outbuf[olen] = outbuf2[olen2] = '\0';
			olen += snprintf(outbuf + olen, sizeof(outbuf) - olen, "%03x: ", i);
			if (i > len - 8) {
				int j;
				for (j = len % 8; j < 8; j++)
					olen2 += snprintf(outbuf2 + olen2, sizeof(outbuf2) - olen2, "   ");
			}
			olen2 += snprintf(outbuf2 + olen2, sizeof(outbuf2) - olen2, "| ");
		}

		outbuf2[olen2++] = (buf[i] > ' ' && buf[i] < 0x7f) ? buf[i] : '.';
		olen += snprintf(outbuf + olen, sizeof(outbuf) - olen, "%02x ", buf[i++]);

		if ((i % 8 == 0) || (i == len)) {
			outbuf2[olen2] = '\0';
			printf("%s%s\n", outbuf, outbuf2);
		}
	}
}



