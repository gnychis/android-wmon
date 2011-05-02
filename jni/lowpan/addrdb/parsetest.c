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
#include <stdint.h>
#include <string.h>
#include <stdio.h>
#include <addrdb.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

extern int yydebug;

int main(int argc, char **argv)
{
	unsigned char gwa[8];
	const char *fname;
	int i, fd;
	if (argc == 2)
		fname = argv[1];
	else
		fname = LEASE_FILE;

//	yydebug = 1;
	fd = open("/dev/urandom", O_RDONLY);
	if(fd < 0)
		goto method2;
	if(read(fd, gwa, 8) < 0)
		goto method2;
	close(fd);
	goto testing;
method2:
	memcpy(gwa, "whack000", 8);
testing:
	addrdb_init(0, 0xfffd);
	addrdb_parse(fname);
	for (i = 0; i < 80; i++) {
		gwa[0] = i;
		printf("allocating %d\n", addrdb_alloc(gwa));
	}
	addrdb_dump_leases(fname);
	addrdb_parse(fname);
	return 0;
}

