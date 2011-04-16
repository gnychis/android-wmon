/* follow_tcp.h
 * TCP specific routines for following traffic streams
 *
 * $Id: follow_tcp.h 24034 2008-01-08 22:54:51Z stig $
 *
 * Wireshark - Network traffic analyzer
 * By Gerald Combs <gerald@wireshark.org>
 * Copyright 2000 Gerald Combs
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
 *
 */

#ifndef __FOLLOW_TCP_H__
#define __FOLLOW_TCP_H__

/** @file
 *  "Follow TCP Stream" dialog box.
 *  @ingroup dialog_group
 */

/** User requested the "Follow TCP Stream" dialog box by menu or toolbar.
 *
 * @param widget parent widget (unused)
 * @param data unused
 */
extern void follow_tcp_stream_cb( GtkWidget *widget, gpointer data);

/** Redraw the text in all "Follow TCP Stream" windows. */
extern void follow_tcp_redraw_all(void);

#endif /* __FOLLOW_TCP_H__ */
