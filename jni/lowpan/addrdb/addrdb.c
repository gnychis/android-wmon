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

#include <sys/types.h>
#include <sys/stat.h>

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include <fcntl.h>
#include <unistd.h>

#include <libcommon.h>
#include <ieee802154.h>
#include <logging.h>

struct lease {
	uint8_t hwaddr[IEEE802154_ADDR_LEN];
	uint16_t short_addr;
	time_t time;
};

static struct simple_hash *hwa_hash;
static struct simple_hash *shorta_hash;

static unsigned int hw_hash(const void *key)
{
	const uint8_t *hwa = key;
	int i;
	unsigned int val = 0;

	for (i = 0; i < IEEE802154_ADDR_LEN; i++) {
		val = (val * 31) | hwa[i];
	}

	return val;
}

static int hw_eq(const void *key1, const void *key2)
{
	return memcmp(key1, key2, IEEE802154_ADDR_LEN);
}

static unsigned int short_hash(const void *key)
{
	const uint16_t *addr = key;

	return *addr;
}

static int short_eq(const void *key1, const void *key2)
{
	const uint16_t addr1 = *(uint16_t *) key1, addr2 = *(uint16_t *) key2;
	return addr1 - addr2;
}

static uint16_t last_addr;
static uint16_t range_min, range_max;
uint16_t addrdb_alloc(uint8_t *hwa)
{
	struct lease *lease = shash_get(hwa_hash, hwa);
	if (lease) {
		lease->time = time(NULL);
		return lease->short_addr;
	}

	int addr = last_addr + 1;
	if (addr > range_max)
			return 0xffff;

	while (shash_get(shorta_hash, &addr)) {
		addr ++;
		if (addr == last_addr || addr > range_max)
			return 0xffff;
		else if (addr == 0xfffe)
			addr = range_min;
	}

	lease = calloc(1, sizeof(*lease));
	memcpy(lease->hwaddr, hwa, IEEE802154_ADDR_LEN);
	lease->short_addr = addr;
	lease->time = time(NULL);

	last_addr = addr;

	shash_insert(hwa_hash, lease->hwaddr, lease);
	shash_insert(shorta_hash, &lease->short_addr, lease);

	log_msg(0, "addr %d:..:%d\n", lease->hwaddr[0], lease->hwaddr[7]);
	return addr;
}

static void addrdb_free(struct lease *lease)
{
	shash_drop(hwa_hash, &lease->hwaddr);
	shash_drop(shorta_hash, &lease->short_addr);
	free(lease);
}

void addrdb_free_hw(uint8_t *hwa)
{
	struct lease *lease = shash_get(hwa_hash, hwa);
	if (!lease) {
		log_msg(0, "Can't remove unknown HWA\n");
		return;
	}

	addrdb_free(lease);
}
void addrdb_free_short(uint16_t short_addr)
{
	struct lease *lease = shash_get(shorta_hash, &short_addr);
	if (!lease) {
		log_msg(0, "Can't remove unknown short address %04x\n", short_addr);
		return;
	}

	addrdb_free(lease);
}

void addrdb_init(/*uint8_t *hwa, uint16_t short_addr, */ uint16_t min, uint16_t max)
{
	last_addr = range_min = min;
	range_max = max;

	hwa_hash = shash_new(hw_hash, hw_eq);
	if (!hwa_hash) {
		log_msg(0, "Error initialising hash\n");
		exit(1);
	}

	shorta_hash = shash_new(short_hash, short_eq);
	if (!shorta_hash) {
		log_msg(0, "Error initialising hash\n");
		exit(1);
	}
}

#define MAX_CONFIG_BLOCK 128

int addrdb_dump_leases(const char *lease_file)
{
	int i;
	struct lease *lease;
	char hwaddr_buf[8 * 3];
	FILE *f = fopen(lease_file, "w");
	if (!f)
		return -1;
	for (i = 0; i < 65536; i++)
	{
		// FIXME: shash_for_each ?!
		lease = shash_get(shorta_hash, &i);
		if (!lease) {
			continue;
		}
		snprintf(hwaddr_buf, sizeof(hwaddr_buf),
				"%02x:%02x:%02x:%02x:%02x:%02x:%02x:%02x\n",
				lease->hwaddr[0], lease->hwaddr[1],
				lease->hwaddr[2], lease->hwaddr[3],
				lease->hwaddr[4], lease->hwaddr[5],
				lease->hwaddr[6], lease->hwaddr[7]);
		fprintf(f,
			"lease {\n\thwaddr %s;"
			"\n\tshortaddr 0x%04x;\n\ttimestamp 0x%08lx;\n};\n",
			hwaddr_buf, lease->short_addr, lease->time);
	}
	fclose(f);
	return 0;
}

void addrdb_insert(uint8_t *hwaddr, uint16_t short_addr, time_t stamp)
{
	struct lease * lease = shash_get(hwa_hash, hwaddr);
	if(lease) {
		log_msg(0, "Got existing lease\n");
		if (lease->short_addr != short_addr)
			log_msg(0, "Mismatch of short addresses for the node!\n");
		else if(stamp > lease->time) /* FIXME */
			lease->time = stamp;
	} else {
		log_msg(0, "Adding lease\n");
		lease = calloc(1, sizeof(*lease));
		memcpy(lease->hwaddr, hwaddr, IEEE802154_ADDR_LEN);
		lease->short_addr = short_addr;
		lease->time = stamp;
		shash_insert(hwa_hash, lease->hwaddr, lease);
		shash_insert(shorta_hash, &lease->short_addr, lease);
	}
}
