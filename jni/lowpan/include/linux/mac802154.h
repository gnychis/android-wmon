/*
 * Copyright (C) 2007, 2008, 2009 Siemens AG
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

#ifndef LINUX_MAC802154_H
#define LINUX_MAC802154_H

enum {
	IFLA_WPAN_UNSPEC,
	IFLA_WPAN_CHANNEL,
	IFLA_WPAN_PAN_ID,
	IFLA_WPAN_SHORT_ADDR,
	IFLA_WPAN_COORD_SHORT_ADDR,
	IFLA_WPAN_COORD_EXT_ADDR,
	IFLA_WPAN_PHY,
	IFLA_WPAN_PAGE,
	__IFLA_WPAN_MAX,
};

#define IFLA_WPAN_MAX	(__IFLA_WPAN_MAX - 1)

#endif

