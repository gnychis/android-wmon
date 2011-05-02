/*
 * Linux IEEE 802.15.4 userspace tools
 *
 * Copyright (C) 2008, 2009 Siemens AG
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Written-by:
 * Dmitry Eremin-Solenikov <dbaryshkov@gmail.com>
 * Sergey Lapin <slapin@ossfans.org>
 * Maxim Osipov <maxim.osipov@siemens.com>
 *
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <netlink/netlink.h>
#include <netlink/genl/genl.h>
#include <netlink/genl/ctrl.h>

#include <ieee802154.h>
#include <nl802154.h>
#include <libcommon.h>

#include "iz.h"

/******************/
/* HELP handling  */
/******************/

static iz_res_t help_parse(struct iz_cmd *cmd)
{
	const struct iz_cmd_desc *desc;

	if (cmd->argc > 2) {
		printf("Too many arguments!\n");
		return IZ_STOP_ERR;
	} else if (cmd->argc == 1) {
		iz_help("iz");
		return IZ_STOP_OK;
	}

	/* Search for command help */
	desc = get_cmd(cmd->argv[1]);
	if (!desc) {
		printf("Unknown command %s!\n", cmd->argv[1]);
		return IZ_STOP_ERR;
	} else {
		printf("%s %s\n\t%s\n\n", desc->name,
			desc->usage,
			desc->doc);
	}
	return IZ_STOP_OK;
}

/******************/
/* EVENT handling */
/******************/

static iz_res_t event_parse(struct iz_cmd *cmd)
{
	cmd->flags = 0;

	if (cmd->argc > 2) {
		printf("Too many arguments!\n");
		return IZ_STOP_ERR;
	}

	return IZ_CONT_OK;
}

static char *iz_cmd_names[__IEEE802154_CMD_MAX + 1] = {
	[__IEEE802154_COMMAND_INVALID] = "IEEE802154_COMMAND_INVALID",

	[IEEE802154_ASSOCIATE_REQ] = "IEEE802154_ASSOCIATE_REQ",
	[IEEE802154_ASSOCIATE_CONF] = "IEEE802154_ASSOCIATE_CONF",
	[IEEE802154_DISASSOCIATE_REQ] = "IEEE802154_DISASSOCIATE_REQ",
	[IEEE802154_DISASSOCIATE_CONF] = "IEEE802154_DISASSOCIATE_CONF",
	[IEEE802154_GET_REQ] = "IEEE802154_GET_REQ",
	[IEEE802154_GET_CONF] = "IEEE802154_GET_CONF",
	[IEEE802154_RESET_REQ] = "IEEE802154_RESET_REQ",
	[IEEE802154_RESET_CONF] = "IEEE802154_RESET_CONF",
	[IEEE802154_SCAN_REQ] = "IEEE802154_SCAN_REQ",
	[IEEE802154_SCAN_CONF] = "IEEE802154_SCAN_CONF",
	[IEEE802154_SET_REQ] = "IEEE802154_SET_REQ",
	[IEEE802154_SET_CONF] = "IEEE802154_SET_CONF",
	[IEEE802154_START_REQ] = "IEEE802154_START_REQ",
	[IEEE802154_START_CONF] = "IEEE802154_START_CONF",
	[IEEE802154_SYNC_REQ] = "IEEE802154_SYNC_REQ",
	[IEEE802154_POLL_REQ] = "IEEE802154_POLL_REQ",
	[IEEE802154_POLL_CONF] = "IEEE802154_POLL_CONF",

	[IEEE802154_ASSOCIATE_INDIC] = "IEEE802154_ASSOCIATE_INDIC",
	[IEEE802154_ASSOCIATE_RESP] = "IEEE802154_ASSOCIATE_RESP",
	[IEEE802154_DISASSOCIATE_INDIC] = "IEEE802154_DISASSOCIATE_INDIC",
	[IEEE802154_BEACON_NOTIFY_INDIC] = "IEEE802154_BEACON_NOTIFY_INDIC",
	[IEEE802154_ORPHAN_INDIC] = "IEEE802154_ORPHAN_INDIC",
	[IEEE802154_ORPHAN_RESP] = "IEEE802154_ORPHAN_RESP",
	[IEEE802154_COMM_STATUS_INDIC] = "IEEE802154_COMM_STATUS_INDIC",
	[IEEE802154_SYNC_LOSS_INDIC] = "IEEE802154_SYNC_LOSS_INDIC",

	[IEEE802154_GTS_REQ] = "IEEE802154_GTS_REQ",
	[IEEE802154_GTS_INDIC] = "IEEE802154_GTS_INDIC",
	[IEEE802154_GTS_CONF] = "IEEE802154_GTS_CONF",
	[IEEE802154_RX_ENABLE_REQ] = "IEEE802154_RX_ENABLE_REQ",
	[IEEE802154_RX_ENABLE_CONF] = "IEEE802154_RX_ENABLE_CONF",

	[IEEE802154_LIST_IFACE] = "IEEE802154_LIST_IFACE",
};


static iz_res_t event_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	const char *iface = nla_get_string(attrs[IEEE802154_ATTR_DEV_NAME]);

	if (cmd->argv[1] && (!iface || strcmp(cmd->argv[1], iface)))
		return IZ_CONT_OK;

	if (!iface)
		iface = "?????";

	if (ghdr->cmd < __IEEE802154_CMD_MAX)
		fprintf(stdout, "%s: %s (%i)\n", iface, iz_cmd_names[ghdr->cmd], ghdr->cmd);
	else
		fprintf(stdout, "%s: UNKNOWN (%i)\n", iface, ghdr->cmd);

	fflush(stdout);

	return IZ_CONT_OK;
}

static iz_res_t event_finish(struct iz_cmd *cmd)
{
	return IZ_CONT_OK;
}


/* Command descriptors */
const struct iz_cmd_desc iz_commands[] = {
	{
		.name		= "help",
		.usage		= "[command]",
		.doc		= "Print detailed help for a command.",
		.parse		= help_parse,
	},
	{
		.name		= "event",
		.usage		= "[iface]",
		.doc		= "Monitor events from the kernel (^C to stop).",
		.parse		= event_parse,
		.response	= event_response,
		.finish		= event_finish,
		.listener	= 1,
	},
	{}
};

