/* print_mswin.h
 * Printing support for MSWindows
 *
 * $Id: print_win32.h 35032 2010-11-26 19:16:06Z sfisher $
 *
 * Wireshark - Network traffic analyzer
 * By Gerald Combs <gerald@wireshark.org>
 * Copyright 2002, Jeffrey C. Foster <jfoste@woodward.com>
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

#ifndef __PRINT_MSWIN_H__
#define __PRINT_MSWIN_H__

/** @file
 * Win32 specific printing.
 */

/** Print the given file.
 *
 * @param file_name the file to print
 */
void print_mswin(char *file_name);

#endif
