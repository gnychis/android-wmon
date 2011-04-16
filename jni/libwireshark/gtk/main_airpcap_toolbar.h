/* main_airpcap_toolbar.h
 * Definitions for the airpcap toolbar routines
 *
 * $Id: main_airpcap_toolbar.h 26899 2008-12-02 08:11:23Z jmayer $
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

#ifndef __MAIN_AIRPCAP_TOOLBAR_H__
#define __MAIN_AIRPCAP_TOOLBAR_H__

extern int    airpcap_dll_ret_val;

GtkWidget *airpcap_toolbar_new(void);
void airpcap_toolbar_show(GtkWidget *airpcap_tb);

#endif /* __MAIN_AIRPCAP_TOOLBAR_H__ */
