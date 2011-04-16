/* proto_hier_stats_dlg.h
 *
 * $Id: proto_hier_stats_dlg.h 24034 2008-01-08 22:54:51Z stig $
 *
 * Wireshark - Network traffic analyzer
 * By Gerald Combs <gerald@wireshark.org>
 * Copyright 1998 Gerald Combs
 *
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

#ifndef __PROTO_HIER_STATS_DLG_H__
#define __PROTO_HIER_STATS_DLG_H__

/** @file
 *  "Protocol Hierarchy" statistics window.
 */

/** Show protocol hierarchy statistics.
 * 
 * @param widget parent widget (unused)
 * @param data unused
 */
extern void proto_hier_stats_cb(GtkWidget *widget, gpointer data);

#endif
