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
#ifndef ADDRDB_H
#define ADDRDB_H

#include <time.h>

void addrdb_init(/*uint8_t *hwa, uint16_t short_addr, */ uint16_t min, uint16_t max);
uint16_t addrdb_alloc(uint8_t *hwa);
void addrdb_free_hw(uint8_t *hwa);
void addrdb_free_short(uint16_t shirt_addr);

int addrdb_parse(const char *fname);
int addrdb_dump_leases(const char *lease_file);
void addrdb_insert(uint8_t *hwa, uint16_t short_addr, time_t stamp);


#endif
