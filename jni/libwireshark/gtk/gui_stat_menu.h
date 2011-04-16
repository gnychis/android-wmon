/* gui_stat_menu.h
 * GTK+-specific menu definitions for use by stats
 *
 * $Id: gui_stat_menu.h 33929 2010-08-26 15:02:27Z etxrab $
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

#ifndef __GTK_STAT_MENU_H__
#define __GTK_STAT_MENU_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/** @file
 * Add a new menu item for a stat.
 */

/**
 * Add a new menu item for a stat.
 * This must be called after we've created the main menu, so it can't
 * be called from the routine that registers stats - we have to introduce
 * another per-stat registration routine.
 *
 * @param name the menu label
 *
 * @param group the menu group this stat should be registered to
 *
 * @param callback gets called when the menu item is selected; it should do
 * the work of creating the stat window.
 *
 * @param selected_packet_enabled gets called by set_menus_for_selected_packet();
 * it's passed a pointer to the "frame_data" structure for the current frame,
 * if any, and to the "epan_dissect_t" structure for that frame, if any, and
 * should return TRUE if the stat will work now (which might depend on whether
 * a frame is selected and, if one is, on the frame) and FALSE if not.
 *
 * @param selected_tree_row_enabled gets called by
 * set_menus_for_selected_tree_row(); it's passed a pointer to the
 * "field_info" structure for the currently selected field, if any,
 * and should return TRUE if the stat will work now (which might depend on
 * whether a tree row is selected and, if one is, on the tree row) and
 * FALSE if not.
 *
 * @param callback_data data for callback function
 */    
extern void register_stat_menu_item(
    const char *name, 
    register_stat_group_t group,
    GtkItemFactoryCallback callback,
    gboolean (*selected_packet_enabled)(frame_data *, epan_dissect_t *, gpointer callback_data),
    gboolean (*selected_tree_row_enabled)(field_info *, gpointer callback_data),
    gpointer callback_data);

/**
 * Same as register_stat_menu_item() but with optional stock item.
 *
 * @param name the menu label
 *
 * @param group the menu group this stat should be registered to
 *
 * @param stock_id the stock_id (icon) to show, or NULL
 *
 * @param callback gets called when the menu item is selected; it should do
 * the work of creating the stat window.
 *
 * @param selected_packet_enabled gets called by set_menus_for_selected_packet();
 * it's passed a pointer to the "frame_data" structure for the current frame,
 * if any, and to the "epan_dissect_t" structure for that frame, if any, and
 * should return TRUE if the stat will work now (which might depend on whether
 * a frame is selected and, if one is, on the frame) and FALSE if not.
 *
 * @param selected_tree_row_enabled gets called by
 * set_menus_for_selected_tree_row(); it's passed a pointer to the
 * "field_info" structure for the currently selected field, if any,
 * and should return TRUE if the stat will work now (which might depend on
 * whether a tree row is selected and, if one is, on the tree row) and
 * FALSE if not.
 *
 * @param callback_data data for callback function
 */
extern void register_stat_menu_item_stock(
    const char *name, 
    register_stat_group_t group,
    const gchar *stock_id,
    GtkItemFactoryCallback callback,
    gboolean (*selected_packet_enabled)(frame_data *, epan_dissect_t *, gpointer callback_data),
    gboolean (*selected_tree_row_enabled)(field_info *, gpointer callback_data),
    gpointer callback_data);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __GTK_STAT_MENU_H__ */
