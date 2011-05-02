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


/******************
 * LIST handling  *
 ******************/

static iz_res_t list_phy_parse(struct iz_cmd *cmd)
{
	if (cmd->argc > 2) {
		printf("Incorrect number of arguments!\n");
		return IZ_STOP_ERR;
	}

	/* iz list wpan0 */
	if (cmd->argc == 2) {
		cmd->phy = cmd->argv[1];
		cmd->flags = NLM_F_REQUEST;
	} else {
		/* iz list */
		cmd->phy = NULL;
		cmd->flags = NLM_F_REQUEST | NLM_F_DUMP;
	}

	return IZ_CONT_OK;
}

static iz_res_t list_phy_request(struct iz_cmd *cmd, struct nl_msg *msg)
{
	/* List single interface */
	if (cmd->phy)
		NLA_PUT_STRING(msg, IEEE802154_ATTR_PHY_NAME, cmd->phy);

	return IZ_CONT_OK;

nla_put_failure:
	return IZ_STOP_ERR;
}

static iz_res_t list_phy_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	char * dev_name;
	uint8_t chan;

	/* Check for mandatory attributes */
	if (!attrs[IEEE802154_ATTR_PHY_NAME] ||
	    !attrs[IEEE802154_ATTR_CHANNEL] ||
	    !attrs[IEEE802154_ATTR_PAGE])
		return IZ_STOP_ERR;

	/* Get attribute values from the message */
	dev_name = nla_get_string(attrs[IEEE802154_ATTR_PHY_NAME]);

	/* Display information about interface */
	printf("%-10s IEEE 802.15.4 PHY object\n", dev_name);
	printf("    page: %d  channel: ",
			nla_get_u8(attrs[IEEE802154_ATTR_PAGE]));
	chan = nla_get_u8(attrs[IEEE802154_ATTR_CHANNEL]);
	if (chan == 255)
		printf("n/a\n");
	else
		printf("%d\n", chan);

	if (attrs[IEEE802154_ATTR_CHANNEL_PAGE_LIST]) {
		int len = nla_len(attrs[IEEE802154_ATTR_CHANNEL_PAGE_LIST]);
		int i, j;
		uint32_t *data = nla_data(attrs[IEEE802154_ATTR_CHANNEL_PAGE_LIST]);
		if (len % 4 != 0) {
			printf("    Error in PAGE LIST\n");
			return IZ_STOP_ERR;
		}

		for (i = 0; i < len / 4; i++) {
			printf("    channels on page %d:", data[i] >> 27);
			for (j = 0; j < 27; j++) {
				if (data[i] & (1 << j))
					printf(" %d", j);
			}

			printf("\n");
		}
	}
	printf("\n");

	return (cmd->flags & NLM_F_MULTI) ? IZ_CONT_OK : IZ_STOP_OK;
}

static iz_res_t list_phy_finish(struct iz_cmd *cmd)
{
	return IZ_STOP_OK;
}

/******************
 *  ADD handling  *
 ******************/

static iz_res_t add_phy_parse(struct iz_cmd *cmd)
{
	if (cmd->argc != 2 && cmd->argc != 3) {
		printf("Incorrect number of arguments!\n");
		return IZ_STOP_ERR;
	}

	cmd->phy = cmd->argv[1];
	cmd->iface = cmd->argv[2]; /* Either iface name or NULL */

	return IZ_CONT_OK;
}

static iz_res_t add_phy_request(struct iz_cmd *cmd, struct nl_msg *msg)
{
	/* add single interface */
	NLA_PUT_STRING(msg, IEEE802154_ATTR_PHY_NAME, cmd->phy);
	if (cmd->iface)
		NLA_PUT_STRING(msg, IEEE802154_ATTR_DEV_NAME, cmd->iface);

	return IZ_CONT_OK;

nla_put_failure:
	return IZ_STOP_ERR;

}

static iz_res_t add_phy_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	if (!attrs[IEEE802154_ATTR_DEV_NAME] ||
	    !attrs[IEEE802154_ATTR_PHY_NAME])
		return IZ_STOP_ERR;

	printf("Registered new device ('%s') on phy %s\n",
			nla_get_string(attrs[IEEE802154_ATTR_DEV_NAME]),
			nla_get_string(attrs[IEEE802154_ATTR_PHY_NAME]));

	return IZ_STOP_OK;
}

/******************
 *  DEL handling  *
 ******************/

static iz_res_t del_phy_parse(struct iz_cmd *cmd)
{
	switch (cmd->argc) {
	case 2:
		cmd->iface = cmd->argv[1];
		break;
	case 3:
		cmd->phy = cmd->argv[1];
		cmd->iface = cmd->argv[2];
		break;
	default:
		printf("Incorrect number of arguments!\n");
		return IZ_STOP_ERR;
	}

	return IZ_CONT_OK;
}

static iz_res_t del_phy_request(struct iz_cmd *cmd, struct nl_msg *msg)
{
	/* add single interface */
	NLA_PUT_STRING(msg, IEEE802154_ATTR_DEV_NAME, cmd->iface);
	if (cmd->phy)
		NLA_PUT_STRING(msg, IEEE802154_ATTR_PHY_NAME, cmd->phy);

	return IZ_CONT_OK;

nla_put_failure:
	return IZ_STOP_ERR;
}

static iz_res_t del_phy_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	if (!attrs[IEEE802154_ATTR_DEV_NAME] ||
	    !attrs[IEEE802154_ATTR_PHY_NAME])
		return IZ_STOP_ERR;

	printf("Removed device ('%s') from phy %s\n",
			nla_get_string(attrs[IEEE802154_ATTR_DEV_NAME]),
			nla_get_string(attrs[IEEE802154_ATTR_PHY_NAME]));

	return IZ_STOP_OK;
}

const struct iz_cmd_desc phy_commands[] = {
	{
		.name		= "listphy",
		.usage		= "[phy]",
		.doc		= "List phys(s).",
		.nl_cmd		= IEEE802154_LIST_PHY,
		.nl_resp	= IEEE802154_LIST_PHY,
		.parse		= list_phy_parse,
		.request	= list_phy_request,
		.response	= list_phy_response,
		.finish		= list_phy_finish,
	},
	{
		.name		= "add",
		.usage		= "phy [iface]",
		.doc		= "Add an interface attached to specified phy.",
		.nl_cmd		= IEEE802154_ADD_IFACE,
		.nl_resp	= IEEE802154_ADD_IFACE,
		.parse		= add_phy_parse,
		.request	= add_phy_request,
		.response	= add_phy_response,
	},
	{
		.name		= "del",
		.usage		= "[phy] iface",
		.doc		= "Delete the specified interface.",
		.nl_cmd		= IEEE802154_DEL_IFACE,
		.nl_resp	= IEEE802154_DEL_IFACE,
		.parse		= del_phy_parse,
		.request	= del_phy_request,
		.response	= del_phy_response,
	},
	{}
};

