/* proto_help.h
 * Routines for dynamic protocol help menus
 *
 * $Id: proto_help.h 33929 2010-08-26 15:02:27Z etxrab $
 *
 * Edgar Gladkich <edgar.gladkich@incacon.de>
 * Gerald Combs <gerald@wireshark.org>
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

#ifndef __PROTO_HELP_H__
#define __PROTO_HELP_H__

/** Search for and read configuration files
 *
 */
extern void proto_help_init(void);

/** Initialize the menu
 *
 * @param widget Context menu root
 * @return void
 */
extern void proto_help_menu_init(GtkWidget *widget);

/** Fill in the protocol help menu
 *
 * @param selection Currently-selected packet
 * @param cfile Capture file
 * @return void
 */
extern void proto_help_menu_modify(GtkTreeSelection* selection, capture_file *cfile);

#endif /* __PROTO_HELP_H__ */
