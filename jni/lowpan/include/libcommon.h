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
#ifndef _LIBCOMMON_H_
#define _LIBCOMMON_H_

void printbuf(const unsigned char *buf, int len);

int parse_hw_addr(const char *addr, unsigned char *buf);

struct nl_handle;
int nl_get_multicast_id(struct nl_handle *handle, const char *family, const char *group);

struct simple_hash;
typedef unsigned int (*shash_hash)(const void *key);
typedef int (*shash_eq)(const void *key1, const void *key2);
struct simple_hash *shash_new(shash_hash hashfn, shash_eq eqfn);
void *shash_insert(struct simple_hash *hash, const void *key, void *ptr);
void *shash_get(struct simple_hash *hash, const void *key);
void *shash_drop(struct simple_hash *hash, const void *key);

#endif
