/* packet-p3.h
 * Routines for X.411 (X.400 Message Transfer) packet dissection
 * Graeme Lunt 2005
 *
 * $Id: packet-p1-template.h 36025 2011-02-22 10:23:44Z stig $
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

#ifndef PACKET_P1_H
#define PACKET_P1_H

#include "packet-p1-val.h"

void p1_initialize_content_globals (proto_tree *tree, gboolean report_unknown_cont_type);
char* p1_get_last_oraddress(void);
void dissect_p1_mts_apdu (tvbuff_t *tvb, packet_info *pinfo, proto_tree *parent_tree);
#include "packet-p1-exp.h"

void proto_reg_handoff_p1(void);
void proto_register_p1(void);

#endif  /* PACKET_P1_H */
