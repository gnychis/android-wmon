/* capture_if_dlg.h
 * Definitions for packet capture interface windows
 *
 * $Id: capture_if_dlg.h 32104 2010-03-04 01:12:04Z guy $
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

#ifndef __CAPTURE_IF_DLG_H__
#define __CAPTURE_IF_DLG_H__

/** User requested the "Capture Interfaces" dialog box by menu or toolbar.
 *
 * @param capture_in_progress capture is in progress
 */
void
set_capture_if_dialog_for_capture_in_progress(gboolean capture_in_progress);

#ifdef HAVE_LIBPCAP

#include "capture_ifinfo.h"	/* for if_info_t */

/** User requested the "Capture Interfaces" dialog box by menu or toolbar.
 *
 * @param widget parent widget (unused)
 * @param data unused
 */
void
capture_if_cb(GtkWidget *widget, gpointer data);

/*
 * Used to retrieve the interface icon
 */
GtkWidget *
capture_get_if_icon(const if_info_t* if_info);

#endif /* HAVE_LIBPCAP */

#endif /* capture_if_dlg.h */


