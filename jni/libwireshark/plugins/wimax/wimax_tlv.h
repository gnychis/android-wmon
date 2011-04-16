/* wimax_tlv.h
 * WiMax TLV handling function header file
 *
 * Copyright (c) 2007 by Intel Corporation.
 *
 * Author: Lu Pan <lu.pan@intel.com>
 *
 * $Id: wimax_tlv.h 35590 2011-01-19 22:53:46Z jake $
 *
 * Wireshark - Network traffic analyzer
 * By Gerald Combs <gerald@wireshark.org>
 * Copyright 1999 Gerald Combs
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in t/he hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
#ifndef _WIMAX_TLV_H_
#define _WIMAX_TLV_H_

#include <glib.h>
#include <epan/packet.h>

#define	WIMAX_TLV_EXTENDED_LENGTH_MASK 0x80
#define	WIMAX_TLV_LENGTH_MASK          0x7F

#define MAX_TLV_LEN 64000

typedef struct
{
	guint8   valid;          /* TLV info status: 0=invalid; 1=valid */
	guint8   type;           /* TLV type */
	guint8   length_type;    /* length type: 0=single byte; 1=multiple bytes */
	guint8   size_of_length; /* size of the TLV length */
	guint    value_offset;   /* the offset of TLV value field */
	gint32   length;         /* length of TLV value field */
} tlv_info_t;

gint   init_tlv_info(tlv_info_t *this, tvbuff_t *tvb, gint offset);
gint   valid_tlv_info(tlv_info_t *this);
gint   get_tlv_type(tlv_info_t *this);
gint   get_tlv_length_type(tlv_info_t *this);
gint   get_tlv_size_of_length(tlv_info_t *this);
gint   get_tlv_value_offset(tlv_info_t *this);
gint32 get_tlv_length(tlv_info_t *this);
proto_tree *add_tlv_subtree(tlv_info_t *this, gint idx, proto_tree *tree, int hfindex, tvbuff_t *tvb, gint start, gint length, gboolean little_endian);
proto_tree *add_protocol_subtree(tlv_info_t *this, gint idx, proto_tree *tree, int hfindex, tvbuff_t *tvb, gint start, gint length, const char *format, ...);

#endif /* WIMAX_TLV_H */
