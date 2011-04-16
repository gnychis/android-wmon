/* capture_dlg.h
 * Definitions for packet capture windows
 *
 * $Id: capture_dlg.h 32820 2010-05-15 19:38:13Z guy $
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

#ifndef __CAPTURE_DLG_H__
#define __CAPTURE_DLG_H__

/* extern GtkWidget* airpcap_tb; */

/** @file
 *  "Capture Options" dialog box.
 *  @ingroup dialog_group
 */

/** User requested the "Capture Options" dialog box by menu or toolbar.
 *
 * @param widget parent widget (unused)
 * @param data unused
 */
void capture_prep_cb(GtkWidget *widget, gpointer data);

/** User requested capture start by menu or toolbar.
 *
 * @param widget parent widget (unused)
 * @param data unused
 */
void capture_start_cb(GtkWidget *widget, gpointer data);

/** User requested capture stop by menu or toolbar.
 *
 * @param widget parent widget (unused)
 * @param data unused
 */
void capture_stop_cb(GtkWidget *widget, gpointer data);

/** User requested capture restart by menu or toolbar.
 *
 * @param widget parent widget (unused)
 * @param data unused
 */
void capture_restart_cb(GtkWidget *widget, gpointer data);

/* capture start confirmed by "Save unsaved capture", so do it now */
void capture_start_confirmed(void);

/** User requested the "Capture Airpcap" dialog box by menu or toolbar.
 *
 * @param widget parent widget (unused)
 * @param data unused
 */
void
capture_air_cb(GtkWidget *widget, gpointer data);

/*
 * We remember the capture settings for each interface when a capture
 * is started on it; the next time we select that interface we start
 * out with those settings.
 *
 * XXX - we currently only do that for monitor mode and the link-layer
 * type; arguably we should do it for the snapshot length, and perhaps
 * promiscuous mode.
 */
typedef struct {
	gboolean	monitor_mode;
	int		linktype;
} cap_settings_t;

/** Get capture settings for interface
 *
 * @param if_name interface name
 */
cap_settings_t
capture_get_cap_settings (gchar *if_name);


#ifdef HAVE_PCAP_REMOTE
struct remote_host {
  gchar    *remote_host;          /**< Host name or network address for remote capturing */
  gchar    *remote_port;          /**< TCP port of remote RPCAP server */
  gint      auth_type;            /**< Authentication type */
  gchar    *auth_username;        /**< Remote authentication parameters */
  gchar    *auth_password;        /**< Remote authentication parameters */
};

#define RECENT_KEY_REMOTE_HOST "recent.remote_host"

/** Write all remote hosts to the recent file
 *
 * @param rf recent file
 */
void
capture_remote_combo_recent_write_all(FILE *rf);

/** Add a new remote host from the recent file
 *
 * @param s string with hostname,port,auth_type
 * @return TRUE if correctly added
 */
gboolean 
capture_remote_combo_add_recent(gchar *s);
#endif

#endif /* capture_dlg.h */
