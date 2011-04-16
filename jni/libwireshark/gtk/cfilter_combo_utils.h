/* cfilter_combo_utils.h
 * Capture filter combo box routines
 *
 * $Id: cfilter_combo_utils.h 33230 2010-06-15 21:18:31Z stig $
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

#ifndef __CFILTER_COMBO_UTILS_H__
#define __CFILTER_COMBO_UTILS_H__

/** @file
 *  Capture filter combo box routines
 */

extern void cfilter_combo_recent_write_all(FILE *rf);
extern gboolean cfilter_combo_add_recent(gchar *s);

#define E_CFILTER_CM_KEY          "capture_filter_combo"
#define E_CFILTER_FL_KEY          "capture_filter_list"
#define RECENT_KEY_CAPTURE_FILTER "recent.capture_filter"

#endif /* __CFILTER_COMBO_UTILS_H__ */
