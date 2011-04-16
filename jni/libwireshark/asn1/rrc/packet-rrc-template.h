/* packet-rrc-template.h
 * Copyright 2009, Anders Broman <anders.broman@ericsson.com>
 *
 * $Id: packet-rrc-template.h 31961 2010-02-23 04:35:23Z guy $
 *
 * Wireshark - Network traffic analyzer
 * By Gerald Combs <gerald@wireshark.org>
 * Copyright 1998 Gerald Combs
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

#ifndef PACKET_RRC_H
#define PACKET_RRC_H

extern int proto_rrc;
#include "packet-rrc-exp.h"

enum rrc_message_type {
	RRC_MESSAGE_TYPE_INVALID	= 0,
	RRC_MESSAGE_TYPE_PCCH		= 1,
	RRC_MESSAGE_TYPE_UL_CCCH,
	RRC_MESSAGE_TYPE_DL_CCCH,
	RRC_MESSAGE_TYPE_UL_DCCH,
	RRC_MESSAGE_TYPE_DL_DCCH
};

#define MAX_RRC_FRAMES	64
typedef struct rrc_info
{
	enum rrc_message_type msgtype[MAX_RRC_FRAMES];
} rrc_info;

#endif  /* PACKET_RRC_H */
