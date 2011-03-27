/* Metageek WiSPY interface 
 * Mike Kershaw/Dragorn <dragorn@kismetwireless.net>
 *
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Extra thanks to Ryan Woodings @ Metageek for interface documentation
 */

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "spectool_gtk_channel.h"

static void wispy_channel_class_init(WispyChannelClass *class);
static void wispy_channel_init(WispyChannel *graph);
static void wispy_channel_destroy(GtkObject *object);

static gint wispy_channel_configure(GtkWidget *widget, 
									 GdkEventConfigure *event);
static gboolean wispy_channel_expose(GtkWidget *widget, 
									  GdkEventExpose *event,
									  gpointer *aux);
static gboolean wispy_channel_button_press(GtkWidget *widget,
											GdkEventButton *event,
											gpointer *aux);
static gboolean wispy_channel_mouse_move(GtkWidget *widget,
										GdkEventMotion *event,
										gpointer *aux);

G_DEFINE_TYPE(WispyChannel, wispy_channel, WISPY_TYPE_WIDGET);

inline int wispy_channel_find_chan_pt(WispyChannel *cwidget, int x, int y) {
	int dbm, maxy, nchannels;
	WispyWidget *wwidget;

	g_return_if_fail(cwidget != NULL);
	g_return_if_fail(IS_WISPY_WIDGET(cwidget));
	wwidget = WISPY_WIDGET(cwidget);

	/* Only compute if we know a chanset and if we're inside the bounding
	 * box */
	if (wwidget->chanopts == NULL)
		return -1;

	if (wwidget->chanopts->chanset == NULL)
		return -2;

	if (x < cwidget->chan_start_x || x > cwidget->chan_end_x ||
		y < cwidget->chan_start_y || y > cwidget->chan_end_y) {
		return -2;
	}

	/* Search for the channel positions */
	maxy = 0;
	nchannels = wwidget->chanopts->chanset->chan_num;
	for (dbm = 0; dbm < nchannels; dbm++) {
		if (cwidget->chan_points[dbm + nchannels].y > maxy)
			maxy = cwidget->chan_points[dbm + nchannels].y;

		if (x > cwidget->chan_points[dbm].x && 
			x < cwidget->chan_points[dbm + nchannels].x &&
			y > cwidget->chan_points[dbm].y &&
			y < cwidget->chan_points[dbm + nchannels].y) {

			return dbm;
		}
	}

	return -1;
}

void wispy_channel_draw(GtkWidget *widget, cairo_t *cr, WispyWidget *wwidget) {
	WispyChannel *channel;
	cairo_text_extents_t extents;
	int x, chpix;
	char mtext[128];

	g_return_if_fail(widget != NULL);

	channel = WISPY_CHANNEL(wwidget);

	cairo_save(cr);

	channel->chan_h = extents.height + ((double) extents.height * 0.1) + 5;

	/* Try to figure out the channels we use for this spectrum */
	for (x = 0; wwidget->chanopts != NULL &&
		 channel_list[x].name != NULL && wwidget->chanopts->chanset == NULL; x++) {
		if (channel_list[x].startkhz >= 
			wwidget->sweepcache->latest->start_khz &&
			channel_list[x].endkhz <= wwidget->sweepcache->latest->end_khz) {
			int cpos;
			double r, g, b;

			wwidget->chanopts->chanset = &(channel_list[x]);

			/* Allocate the channels that are 'hit' or highlighted */
			if (wwidget->chanopts->chanhit)
				free(wwidget->chanopts->chanhit);
			wwidget->chanopts->chanhit = 
				malloc(sizeof(int) * wwidget->chanopts->chanset->chan_num);
			memset(wwidget->chanopts->chanhit, 0, 
				   sizeof(int) * wwidget->chanopts->chanset->chan_num);

			/* Allocate color sweep */
			if (wwidget->chanopts->chancolors)
				free(wwidget->chanopts->chancolors);
			wwidget->chanopts->chancolors = malloc(sizeof(double) *
										 wwidget->chanopts->chanset->chan_num * 3);

			for (cpos = 0; cpos < wwidget->chanopts->chanset->chan_num; cpos++) {
				/* Get the RGB values of a full-intensity color somewhere
				 * along the H slider derived from the channel position */
				hsv_to_rgb(&r, &g, &b, 
						   (360 / wwidget->chanopts->chanset->chan_num) * cpos, 1, 1);
				/* Convert the hex colors to cairo colors */
				wwidget->chanopts->chancolors[(3 * cpos) + 0] = HC2CC(r);
				wwidget->chanopts->chancolors[(3 * cpos) + 1] = HC2CC(g);
				wwidget->chanopts->chancolors[(3 * cpos) + 2] = HC2CC(b);
			}

			break;
		}
	}

	/* Plot the channels if we know how */
	if (wwidget->chanopts != NULL && wwidget->chanopts->chanset != NULL && 
		wwidget->show_channels) {
		/* Allocate the channel point array if we haven't yet, so the mouse
		 * handlers know where we've clicked.  Points allocated inside the
		 * channel widget itself. */
		if (channel->chan_points == NULL) {
			channel->chan_points =
				(GdkPoint *) malloc(sizeof(GdkPoint) *
									wwidget->chanopts->chanset->chan_num * 2);
		}

		/* Draw the channel text along the bottom */
		cairo_save(cr);
		for (x = 0; x < wwidget->chanopts->chanset->chan_num; x++) {
			chpix = ((float) wwidget->g_len_x /
					 (wwidget->sweepcache->latest->end_khz -
					  wwidget->sweepcache->latest->start_khz)) *
				(wwidget->chanopts->chanset->chan_freqs[x] - 
				 wwidget->sweepcache->latest->start_khz);

			if (x == wwidget->chanopts->hi_chan) {
				cairo_set_source_rgb(cr, HC2CC(0xFF), HC2CC(0xF6), HC2CC(0x00));
				snprintf(mtext, 128, "%s", 
						 wwidget->chanopts->chanset->chan_text[x]);
			} else {
				cairo_set_source_rgb(cr, 1, 1, 1);
				snprintf(mtext, 128, "%s", 
						 wwidget->chanopts->chanset->chan_text[x]);
			}

			cairo_move_to(cr, wwidget->g_start_x + chpix, 
						  wwidget->g_start_y);
			cairo_line_to(cr, wwidget->g_start_x + chpix, wwidget->g_start_y + 5);
			cairo_stroke(cr);

			cairo_select_font_face(cr, "Helvetica", 
								   CAIRO_FONT_SLANT_NORMAL, 
								   CAIRO_FONT_WEIGHT_BOLD);
			cairo_set_font_size(cr, 14);
			cairo_text_extents(cr, mtext, &extents);
			cairo_move_to(cr, 
						  wwidget->g_start_x + chpix - (extents.width / 2),
						  wwidget->g_start_y + 10 + extents.height);
			cairo_show_text(cr, mtext);

			channel->chan_points[x].x = 
				wwidget->g_start_x + chpix - (extents.width / 2) - 4;
			channel->chan_points[x].y = wwidget->g_start_y + 10 - 4;
			channel->chan_points[x + wwidget->chanopts->chanset->chan_num].x =
				channel->chan_points[x].x + extents.width + 8;
			channel->chan_points[x + wwidget->chanopts->chanset->chan_num].y =
				channel->chan_points[x].y + extents.height + 10;

			if (wwidget->chanopts->chanhit[x]) {
				cairo_save(cr);
				cairo_set_source_rgba(cr,
								wwidget->chanopts->chancolors[(3 * x) + 0],
								wwidget->chanopts->chancolors[(3 * x) + 1],
								wwidget->chanopts->chancolors[(3 * x) + 2], 0.60);
				cairo_rectangle(cr,
						wwidget->g_start_x + chpix - (extents.width / 2) - 3.5,
						wwidget->g_start_y + 10 - 3.5,
								extents.width + 8,
								extents.height + 10);
				cairo_fill(cr);
				/* cairo_stroke(cr); */
				cairo_restore(cr);
			}
		}

		channel->chan_start_x = channel->chan_points[0].x - 1;
		channel->chan_end_x = 
			channel->chan_points[(wwidget->chanopts->chanset->chan_num * 2) - 1].x + 1;
		channel->chan_start_y = channel->chan_points[0].y - 1;
		channel->chan_end_y = 
			channel->chan_points[(wwidget->chanopts->chanset->chan_num * 2) - 1].y + 1;

		cairo_restore(cr);

	}
}

static gint wispy_channel_button_press(GtkWidget *widget, 
									  GdkEventButton *event,
									  gpointer *aux) {
	WispyChannel *channel;
	WispyWidget *wwidget;
	int ch;
	GList *upd_iter;

	g_return_if_fail(aux != NULL);
	g_return_if_fail(IS_WISPY_CHANNEL(aux));
	g_return_if_fail(IS_WISPY_WIDGET(aux));

	channel = WISPY_CHANNEL(aux);
	wwidget = WISPY_WIDGET(aux);

	g_return_if_fail(wwidget->chanopts != NULL);
	g_return_if_fail(wwidget->chanopts->chanset != NULL);
	g_return_if_fail(wwidget->sweepcache != NULL);
	g_return_if_fail(wwidget->sweepcache->latest != NULL);

	if (event->button != 1)
		return TRUE;

	if ((ch = wispy_channel_find_chan_pt(channel, event->x, event->y)) > -1) {
		/* Should never get here if we don't have a chanset, do some
		 * checking anyhow though */
		if (ch < 0 || ch > wwidget->chanopts->chanset->chan_num)
			return TRUE;

		if (wwidget->chanopts->chanhit[ch]) {
			wwidget->chanopts->chanhit[ch] = 0;
		} else {
			wwidget->chanopts->chanhit[ch] = 1;
		}

		wwidget->chanopts->hi_chan = -1;

	}

	wispy_widget_update(GTK_WIDGET(wwidget));

	upd_iter = channel->update_list;
	while (upd_iter != NULL) {
		wispy_widget_update(GTK_WIDGET(upd_iter->data));

		upd_iter = g_list_next(upd_iter);
		continue;
	}

	return TRUE;
}

static gboolean wispy_channel_mouse_move(GtkWidget *widget,
										GdkEventMotion *event,
										gpointer *aux) {
	int x, y;
	int ch;
	GdkModifierType state;
	WispyChannel *channel;
	WispyWidget *wwidget;
	GList *upd_iter;

	g_return_if_fail(aux != NULL);
	g_return_if_fail(IS_WISPY_CHANNEL(aux));
	g_return_if_fail(IS_WISPY_WIDGET(aux));

	channel = WISPY_CHANNEL(aux);
	wwidget = WISPY_WIDGET(aux);

	g_return_if_fail(wwidget->sweepcache != NULL);
	g_return_if_fail(wwidget->sweepcache->latest != NULL);

	if (event->is_hint) {
		gdk_window_get_pointer(event->window, &x, &y, &state);
	} else {
		x = (int) event->x;
		y = (int) event->y;
		state = event->state;
	}

	/* Search for the channel positions, update the graph if we've changed
	 * the highlighted channel */
	g_return_if_fail(wwidget->chanopts != NULL);
	g_return_if_fail(wwidget->chanopts->chanset != NULL);

	if ((ch = wispy_channel_find_chan_pt(channel, x, y)) >= -1) {
		if (ch != wwidget->chanopts->hi_chan) {
			wwidget->chanopts->hi_chan = ch;
			wispy_widget_update(GTK_WIDGET(wwidget));

			upd_iter = channel->update_list;
			while (upd_iter != NULL) {
				wispy_widget_update(GTK_WIDGET(upd_iter->data));

				upd_iter = g_list_next(upd_iter);
				continue;
			}
		}
	} else if (wwidget->chanopts->hi_chan > -1) {
		wwidget->chanopts->hi_chan = -1;
		wispy_widget_update(GTK_WIDGET(wwidget));

		upd_iter = channel->update_list;
		while (upd_iter != NULL) {
			wispy_widget_update(GTK_WIDGET(upd_iter->data));

			upd_iter = g_list_next(upd_iter);
			continue;
		}
	}

	return TRUE;
}

void wispy_channel_update(GtkWidget *widget) {
	WispyWidget *wwidget;
	WispyChannel *channel;

	g_return_if_fail(widget != NULL);
	g_return_if_fail(IS_WISPY_WIDGET(widget));
	g_return_if_fail(IS_WISPY_CHANNEL(widget));

	wwidget = WISPY_WIDGET(widget);
	channel = WISPY_CHANNEL(widget);

	g_return_if_fail(wwidget->draw != NULL);

	/* Bail out if we don't have any sweep data */
	if (wwidget->sweepcache == NULL)
		return;
	if (wwidget->sweepcache->pos < 0)
		return;
}

static void wispy_channel_size_request (GtkWidget *widget, GtkRequisition *requisition) {
	WispyWidget *wwidget = WISPY_WIDGET(widget);

	requisition->width = 0;
	requisition->height = 25;

	if (GTK_BIN(wwidget)->child && GTK_WIDGET_VISIBLE(GTK_BIN(wwidget)->child)) {
		GtkRequisition child_requisition;

		gtk_widget_size_request(GTK_BIN(wwidget)->child, &child_requisition);

		requisition->width += child_requisition.width;
		requisition->height += child_requisition.height;
	}
}

static void wispy_channel_class_init(WispyChannelClass *class) {
	GObjectClass *gobject_class;
	GtkObjectClass *object_class;
	GtkWidgetClass *widget_class;

	gobject_class = G_OBJECT_CLASS(class);
	object_class = GTK_OBJECT_CLASS(class);
	widget_class = GTK_WIDGET_CLASS(class);

	object_class->destroy = wispy_channel_destroy;

	widget_class->size_request = wispy_channel_size_request;
}

static void wispy_channel_destroy(GtkObject *object) {
	WispyChannel *channel = WISPY_CHANNEL(object);
	WispyWidget *wwidget;

	wwidget = WISPY_WIDGET(channel);

	GTK_OBJECT_CLASS(wispy_channel_parent_class)->destroy(object);
}

GtkWidget *wispy_channel_new() {
	WispyChannel *channel;
	WispyWidget *wwidget;

	channel = gtk_type_new(wispy_channel_get_type());
	printf("debug - channel new %p widget %p\n", channel, GTK_WIDGET(channel));

	wwidget = WISPY_WIDGET(channel);

	return GTK_WIDGET(channel);
}

static void wispy_channel_wdr_sweep(int slot, int mode,
								   wispy_sample_sweep *sweep, void *aux) {
	WispyChannel *cwidget;
	WispyWidget *wwidget;

	g_return_if_fail(aux != NULL);
	g_return_if_fail(IS_WISPY_CHANNEL(aux));

	cwidget = WISPY_CHANNEL(aux);
	wwidget = WISPY_WIDGET(aux);

	if ((mode & WISPY_POLL_CONFIGURED)) {
		if (wwidget->chanopts == NULL)
			return;

		wwidget->chanopts->chanset = NULL;
		if (wwidget->chanopts->chanhit)
			free(wwidget->chanopts->chanhit);
		wwidget->chanopts->chanhit = NULL;
		if (wwidget->chanopts->chancolors)
			free(wwidget->chanopts->chancolors);
		wwidget->chanopts->chancolors = NULL;
	}
}

static void wispy_channel_init(WispyChannel *channel) {
	WispyWidget *wwidget;
	GtkWidget *temp;

	wwidget = WISPY_WIDGET(channel);

	channel->chan_points = NULL;
	channel->chan_h = -1;
	channel->chan_start_x = channel->chan_end_x = 
		channel->chan_start_y = channel->chan_end_y = 0;

	channel->update_list = NULL;

	wwidget->sweep_num_samples = 1;
	wwidget->graph_title = "channel";
	wwidget->graph_title_bg = "#ABBBBB";
	wwidget->graph_control_bg = "#ABBBBB";
	wwidget->show_channels = 1;

	wwidget->wdr_sweep_func = wispy_channel_wdr_sweep;
	wwidget->wdr_devbind_func = NULL;
	wwidget->draw_mouse_move_func = wispy_channel_mouse_move;
	wwidget->draw_mouse_click_func = wispy_channel_button_press;

	wwidget->draw_timeout = 1000;
	wwidget->draw_func = wispy_channel_draw;
	wwidget->update_func = wispy_channel_update;

	wwidget->timeout_ref =
		g_timeout_add(wwidget->draw_timeout,
					  (GSourceFunc) wispy_widget_timeout, wwidget);

	wispy_widget_buildgui(wwidget);
}

void wispy_channel_append_update(GtkWidget *widget, GtkWidget *update) {
	WispyChannel *channel;

	g_return_if_fail(widget != NULL);
	g_return_if_fail(IS_WISPY_CHANNEL(widget));

	channel = WISPY_CHANNEL(widget);

	channel->update_list = g_list_append(channel->update_list, update);
}

