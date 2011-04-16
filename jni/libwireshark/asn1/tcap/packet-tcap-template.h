/* packet-tcap.h
 *
 * $Id: packet-tcap-template.h 22415 2007-07-27 19:24:40Z morriss $
 *
 * Copyright 2004, Tim Endean <endeant@hotmail.com>
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */


#ifndef PACKET_tcap_H
#define PACKET_tcap_H
/* TCAP component type */
#define TCAP_COMP_INVOKE	0xa1
#define TCAP_COMP_RRL		0xa2
#define TCAP_COMP_RE		0xa3
#define TCAP_COMP_REJECT	0xa4
#define TCAP_COMP_RRN		0xa7


#define ANSI_TC_INVOKE_L	0xe9
#define ANSI_TC_RRL		0xea
#define ANSI_TC_RE		0xeb
#define ANSI_TC_REJECT		0xec
#define ANSI_TC_INVOKE_N	0xed
#define ANSI_TC_RRN		0xee


#define	TCAP_SEQ_TAG		0x30
#define	TCAP_SET_TAG		0x31

#define TCAP_INVOKE_ID_TAG	0x02
#define TCAP_LINKED_ID_TAG	0x80

#define	TCAP_EOC_LEN		2

#define	TCAP_CONSTRUCTOR(TCtag)	(TCtag & 0x20)

#define TC_BEGIN 1
#define TC_CONT 2
#define TC_END 3
#define TC_ABORT 4
#define TC_ANSI_ABORT 5
#define TC_ANSI_ALL 6

struct tcap_private_t {
  gboolean acv; /* Is the Application Context Version present */
  void * oid;
  guint32 session_id;
  void * context;
  gchar *TransactionID_str;
};

extern gint tcap_standard;

extern const value_string tcap_component_type_str[];
void proto_reg_handoff_tcap(void);
void proto_register_tcap(void);

extern dissector_handle_t get_itu_tcap_subdissector(guint32 ssn);
dissector_handle_t get_ansi_tcap_subdissector(guint32 ssn);

extern void add_ansi_tcap_subdissector(guint32 ssn, dissector_handle_t dissector);
extern void add_itu_tcap_subdissector(guint32 ssn, dissector_handle_t dissector);

extern void delete_ansi_tcap_subdissector(guint32 ssn, dissector_handle_t dissector);
extern void delete_itu_tcap_subdissector(guint32 ssn, dissector_handle_t dissector);

extern void call_tcap_dissector(dissector_handle_t, tvbuff_t*, packet_info*, proto_tree*);

#include "packet-tcap-exp.h"

#endif  /* PACKET_tcap_H */
