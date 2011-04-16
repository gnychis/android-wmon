/* paket-ethercat-frame.h
 *
 * $Id: packet-ethercat-frame.h 24024 2008-01-07 20:56:24Z stig $
 *
 * Copyright (c) 2007 by Beckhoff Automation GmbH
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
#ifndef _PACKET_ETHERCAT_FRAME_H
#define _PACKET_ETHERCAT_FRAME_H

/* structure for decoding the header -----------------------------------------*/
typedef union _EtherCATFrameParser
{
   struct
   {
      guint16 length   : 11;
      guint16 reserved : 1;
      guint16 protocol : 4;
   } v;
   guint16 hdr;
} EtherCATFrameParserHDR;
typedef EtherCATFrameParserHDR *PEtherCATFrameParserHDR;

#define EtherCATFrameParserHDR_Len sizeof(EtherCATFrameParserHDR)

#endif
