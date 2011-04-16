/* font_utils.h
 * Declarations of utilities to use for font manipulation
 *
 * $Id: font_utils.h 35126 2010-12-06 01:34:58Z wmeier $
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


/** @file
 * Utilities for font manipulation. 
 *
 * There are two different fonts used:
 * - the application font for menu's, dialog's and such
 * - the user font for the packet panes
 * 
 * The user font is also available in regular (m_r_font) and bold (m_b_font) versions.
 */

#ifndef __FONT_UTILS_H__
#define __FONT_UTILS_H__

/** Init the application and user fonts at program start. */
extern void font_init(void);

/** Return value from font_apply() */
typedef enum {
	FA_SUCCESS,             /**< function succeeded */
	FA_FONT_NOT_RESIZEABLE, /**< the chosen font isn't resizable */
	FA_FONT_NOT_AVAILABLE   /**< the chosen font isn't available */
} fa_ret_t;

/** Applies a new user font, corresponding to the preferences font name and recent zoom level. 
 *  Will also redraw the screen.
 *
 * @return if the new font could be set or not
 */
extern fa_ret_t user_font_apply(void);

/** Test, if the given font name is available.
 *
 * @param font_name the font to test
 * @return TRUE, if this font is available
 */
extern gboolean user_font_test(gchar *font_name);

/** Get the regular user font.
 *
 * @return the regular user font
 */
extern PangoFontDescription *user_font_get_regular(void);

/** Get the bold user font.
 *
 * @return the bold user font
 */
extern PangoFontDescription *user_font_get_bold(void);

#endif
