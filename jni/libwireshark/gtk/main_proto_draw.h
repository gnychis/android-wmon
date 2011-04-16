/* proto_draw.h
 * Definitions for GTK+ packet display structures and routines
 *
 * $Id: main_proto_draw.h 33929 2010-08-26 15:02:27Z etxrab $
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

#ifndef __MAIN_PROTO_DRAW_H__
#define __MAIN_PROTO_DRAW_H__

/** @file
 *  Packet tree and details panes.
 *  @ingroup main_window_group
 */

/** Create byte views in the main window.
 */
void add_main_byte_views(epan_dissect_t *edt);

/** Display the protocol tree in the main window.
 */
void main_proto_tree_draw(proto_tree *protocol_tree);

/** Clear the hex dump and protocol tree panes.
 */
void clear_tree_and_hex_views(void);


/** Get the current text notebook page of the packet details notebook.
 *
 * @param nb_ptr the notebook widget
 * @return the notebook page
 */
extern GtkWidget *get_notebook_bv_ptr(GtkWidget *nb_ptr);

/**
 * Get the data and length for a byte view, given the byte view page widget.
 *
 * @param byte_view the byte view to look at
 * @param data_len set "*data_len" to the length
 * @return the pointer, or NULL on error
 */
extern const guint8 *get_byte_view_data_and_length(GtkWidget *byte_view,
						   guint *data_len);

/** Set the current text page of the notebook to the window that
 * refers to a particular tvbuff.
 *
 * @param nb_ptr the byte view notebook
 * @param tvb the tvbuff to look at
 */
extern void set_notebook_page(GtkWidget *nb_ptr, tvbuff_t *tvb);

/** Redraw a given byte view window.
 *
 * @param nb_ptr the byte view notebook
 * @param fd selected frame
 * @param finfo selected field_info
 */
extern void redraw_packet_bytes(GtkWidget *nb_ptr, frame_data *fd, field_info *finfo);

/** Redraw all byte view windows. */
extern void redraw_packet_bytes_all(void);

/** Create a new byte view (packet details pane).
 *
 * @return the new byte view
 */
extern GtkWidget *byte_view_new(void);

/** Clear and fill all the byte view notebook tabs.
 *
 * @param edt current dissections
 * @param tree_view the corresponding packet tree
 * @param nb_ptr the byte view notebook
 */
extern void add_byte_views(epan_dissect_t *edt, GtkWidget *tree_view,
                           GtkWidget *nb_ptr);

/** Gdk button click appeared, select the byte view from that position.
 * 
 * @param widget the byte view
 * @param event the button event clicked
 * @return TRUE if could be selected
 */
extern gboolean byte_view_select(GtkWidget *widget, GdkEventButton *event);

/** This highlights the field in the proto tree that is at position byte
 *
 * @param tvb the current tvbuff
 * @param byte the byte offset within the packet to highlight
 * @param tree_view the current tree_view
 * @param tree the current tree
 * @return TRUE if highlighting was successful
 */
gboolean
highlight_field(tvbuff_t *tvb, gint byte, GtkTreeView *tree_view,
		proto_tree *tree);

/** Callback for "Export Selected Packet Bytes" operation.
 *
 * @param w unused
 * @param data unused
 */
extern void savehex_cb(GtkWidget * w, gpointer data);

/** Format of packet data to copy to clipboard.
 *  Lower nibble holds data type, next nibble holds flags.
 */
typedef enum {
    CD_ALLINFO,     /* All information - columated hex with text in separate column */
    CD_TEXTONLY,    /* Printable characters */
    CD_HEX,         /* Hex, space separated, no linebreaks */
    CD_HEXCOLUMNS,  /* Like "All Information" but with no ASCII */
    CD_BINARY,      /* Raw binary octets */

    CD_TYPEMASK = 0x0000FFFF,          /* Mask for extracting type */
    CD_FLAGSMASK = 0xFFFF0000,         /* Mask for extracting flags */

    CD_FLAGS_SELECTEDONLY = 0x00010000 /* Copy only selected bytes */
} copy_data_type;


/** Callback for "Copy packet bytes to clipboard" operation.
 *
 * @param w unused
 * @param data unused
 * @param data_type copy_data_type 
 *
 */
extern void copy_hex_cb(GtkWidget * w, gpointer data, copy_data_type data_type);

/** Redraw a given byte view window.
 *
 * @param bv the byte view
 * @param pd the packet data
 * @param fd the current fame
 * @param finfo the current field info
 * @param len the byte view length
 */
extern void packet_hex_print(GtkWidget *bv, const guint8 *pd, frame_data *fd,
		 field_info *finfo, guint len);

/**
 * Redraw the text using the saved information. Usually called if
 * the preferences have changed.
 *
 * @param bv the byte view
 */
extern void packet_hex_reprint(GtkWidget *bv);

/** Set a new font for all protocol trees.
 *
 * @param font the new font
 */
extern void set_ptree_font_all(PangoFontDescription *font);

/** Find field in tree view by field_info.
 *
 * @param tree_view the tree view to look at
 * @param finfo the field info the look for
 * @return the path to the field
 */
extern GtkTreePath *tree_find_by_field_info(GtkTreeView *tree_view, field_info *finfo);

/** Create a new tree view (packet details).
 *
 * @param prefs current preferences
 * @param tree_view_p fill in the new tree view
 * @return the new scrolled window (parent of the tree view)
 */
extern GtkWidget * main_tree_view_new(e_prefs *prefs, GtkWidget **tree_view_p);

/** Clear and redraw the whole tree view.
 *
 * @param protocol_tree the currently dissected protocol tree
 * @param tree_view the tree view to redraw
 */
extern void proto_tree_draw(proto_tree *protocol_tree, GtkWidget *tree_view);

/** Expand the whole tree view.
 *
 * @param protocol_tree the currently dissected protocol tree
 * @param tree_view the tree view to redraw
 */
extern void expand_all_tree(proto_tree *protocol_tree, GtkWidget *tree_view);

/** Collapse the whole tree view.
 *
 * @param protocol_tree the currently dissected protocol tree
 * @param tree_view the tree view to redraw
 */
extern void collapse_all_tree(proto_tree *protocol_tree, GtkWidget *tree_view);

/** Gdk button click appeared, select the byte view from that position.
 * 
 * @param widget the tree view
 * @param event the button event clicked
 * @return TRUE if could be selected
 */
extern gboolean tree_view_select(GtkWidget *widget, GdkEventButton *event);

/** Set the selection mode of all packet tree windows.
 *
 * @param val GTK_SELECTION_SINGLE if TRUE, GTK_SELECTION_BROWSE if FALSE
 */
extern void set_ptree_sel_browse_all(gboolean val);

typedef enum {
  BYTES_HEX,
  BYTES_BITS
} bytes_view_type;
extern void select_bytes_view (GtkWidget *widget, gpointer data, gint view);

/** init the expert colors */
extern void proto_draw_colors_init(void);

/** the expert colors */
extern GdkColor	expert_color_chat;
extern GdkColor	expert_color_note;
extern GdkColor	expert_color_warn;
extern GdkColor	expert_color_error;
extern GdkColor	expert_color_foreground;

/* string representation of expert colors */
extern gchar *expert_color_chat_str;
extern gchar *expert_color_note_str;
extern gchar *expert_color_warn_str;
extern gchar *expert_color_error_str;
extern gchar *expert_color_foreground_str;

#endif /* __MAIN_PROTO_DRAW_H__ */
