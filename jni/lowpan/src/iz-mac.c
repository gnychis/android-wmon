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

/*****************/
/* SCAN handling */
/*****************/

static iz_res_t scan_parse(struct iz_cmd *cmd)
{
	cmd->flags = NLM_F_REQUEST;
	return IZ_CONT_OK;
}

static iz_res_t scan_request(struct iz_cmd *cmd, struct nl_msg *msg) {
	int type;
	int duration;
	char *dummy;
	int channels;

	if (cmd->argc != 5) {
		printf("Incorrect number of arguments!\n");
		return IZ_STOP_ERR;
	}

	if (!cmd->argv[1])
		return IZ_STOP_ERR;
	NLA_PUT_STRING(msg, IEEE802154_ATTR_DEV_NAME, cmd->argv[1]);

	if (!cmd->argv[2])
		return IZ_STOP_ERR;
	if (!strcmp(cmd->argv[2], "ed")) {
		type = IEEE802154_MAC_SCAN_ED;
	} else if (!strcmp(cmd->argv[2], "active")) {
		type = IEEE802154_MAC_SCAN_ACTIVE;
	} else if (!strcmp(cmd->argv[2], "passive")) {
		type = IEEE802154_MAC_SCAN_PASSIVE;
	} else if (!strcmp(cmd->argv[2], "orphan")) {
		type = IEEE802154_MAC_SCAN_ORPHAN;
	} else {
		printf("Unknown scan type %s!\n", cmd->argv[2]);
		return IZ_STOP_ERR;
	}

	if (!cmd->argv[3])
		return IZ_STOP_ERR;
	channels = strtol(cmd->argv[3], &dummy, 16);
	if (*dummy) {
		printf("Bad channels %s!\n", cmd->argv[3]);
		return IZ_STOP_ERR;
	}

	if (!cmd->argv[4])
		return IZ_STOP_ERR;
	duration = strtol(cmd->argv[4], &dummy, 10);
	if (*dummy) {
		printf("Bad duration %s!\n", cmd->argv[4]);
		return IZ_STOP_ERR;
	}

	NLA_PUT_U8(msg, IEEE802154_ATTR_SCAN_TYPE, type);
	NLA_PUT_U32(msg, IEEE802154_ATTR_CHANNELS, channels);
	NLA_PUT_U8(msg, IEEE802154_ATTR_DURATION, duration);

	return IZ_CONT_OK;

nla_put_failure:
	return IZ_STOP_ERR;
}

static iz_res_t scan_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	uint8_t status, type;
	int i;
	uint8_t edl[27];

	if (!attrs[IEEE802154_ATTR_DEV_INDEX] ||
	    !attrs[IEEE802154_ATTR_STATUS] ||
	    !attrs[IEEE802154_ATTR_SCAN_TYPE])
		return IZ_STOP_ERR;

	status = nla_get_u8(attrs[IEEE802154_ATTR_STATUS]);
	if (status != 0)
		printf("Scan failed: %02x\n", status);

	type = nla_get_u8(attrs[IEEE802154_ATTR_SCAN_TYPE]);
	switch (type) {
		case IEEE802154_MAC_SCAN_ED:
			if (!attrs[IEEE802154_ATTR_ED_LIST])
				return IZ_STOP_ERR;

			nla_memcpy(edl, attrs[IEEE802154_ATTR_ED_LIST], 27);
			printf("ED Scan results:\n");
			for (i = 0; i < 27; i++)
				printf("  Ch%2d --- ED = %02x\n", i, edl[i]);
			return IZ_STOP_OK;

		case IEEE802154_MAC_SCAN_ACTIVE:
			printf("Started active (beacons) scan...\n");
			return IZ_CONT_OK;
		default:
			printf("Unsupported scan type: %d\n", type);
			break;
	}

	return IZ_STOP_OK;
}

/******************/
/* LIST handling  */
/******************/

static iz_res_t list_parse(struct iz_cmd *cmd)
{
	if (cmd->argc > 2) {
		printf("Incorrect number of arguments!\n");
		return IZ_STOP_ERR;
	}

	/* iz list wpan0 */
	if (cmd->argc == 2) {
		cmd->iface = cmd->argv[1];
		cmd->flags = NLM_F_REQUEST;
	} else {
		/* iz list */
		cmd->iface = NULL;
		cmd->flags = NLM_F_REQUEST | NLM_F_DUMP;
	}

	return IZ_CONT_OK;
}

static iz_res_t list_request(struct iz_cmd *cmd, struct nl_msg *msg)
{
	/* List single interface */
	if (cmd->iface)
		NLA_PUT_STRING(msg, IEEE802154_ATTR_DEV_NAME, cmd->iface);

	return IZ_CONT_OK;

nla_put_failure:
	return IZ_STOP_ERR;
}

static iz_res_t list_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	char * dev_name;
	char * phy_name = NULL;
	uint32_t dev_index;
	unsigned char hw_addr[IEEE802154_ADDR_LEN];
	uint16_t short_addr;
	uint16_t pan_id;
	char * dev_type_str;

	/* Check for mandatory attributes */
	if (!attrs[IEEE802154_ATTR_DEV_NAME]     ||
	    !attrs[IEEE802154_ATTR_DEV_INDEX]    ||
	    !attrs[IEEE802154_ATTR_HW_ADDR]      ||
	    !attrs[IEEE802154_ATTR_SHORT_ADDR]   ||
	    !attrs[IEEE802154_ATTR_PAN_ID])
		return IZ_STOP_ERR;

	/* Get attribute values from the message */
	dev_name = nla_get_string(attrs[IEEE802154_ATTR_DEV_NAME]);
	dev_index = nla_get_u32(attrs[IEEE802154_ATTR_DEV_INDEX]);
	nla_memcpy(hw_addr, attrs[IEEE802154_ATTR_HW_ADDR],
		IEEE802154_ADDR_LEN);
	short_addr = nla_get_u16(attrs[IEEE802154_ATTR_SHORT_ADDR]);
	pan_id = nla_get_u16(attrs[IEEE802154_ATTR_PAN_ID]);

	if (attrs[IEEE802154_ATTR_PHY_NAME])
		phy_name = nla_get_string(attrs[IEEE802154_ATTR_PHY_NAME]);

	/* Display information about interface */
	printf("%s\n", dev_name);
	dev_type_str = "IEEE 802.15.4 MAC interface";
	printf("    link: %s\n", dev_type_str);
	if (phy_name)
		printf("    phy %s\n", phy_name);

	printf("    hw %02x:%02x:%02x:%02x:%02x:%02x:%02x:%02x",
			hw_addr[0], hw_addr[1], hw_addr[2], hw_addr[3],
			hw_addr[4], hw_addr[5], hw_addr[6], hw_addr[7]);
	printf(" pan 0x%04hx short 0x%04hx\n", pan_id, short_addr);

	return (cmd->flags & NLM_F_MULTI) ? IZ_CONT_OK : IZ_STOP_OK;
}

static iz_res_t list_finish(struct iz_cmd *cmd)
{
	return IZ_STOP_OK;
}

/************************/
/* ASSOCIATE handling   */
/************************/

static iz_res_t assoc_parse(struct iz_cmd *cmd)
{
	cmd->flags = NLM_F_REQUEST;
	return IZ_CONT_OK;
}

static iz_res_t assoc_request(struct iz_cmd *cmd, struct nl_msg *msg)
{
	char *dummy;
	uint16_t pan_id, coord_short_addr;
	uint8_t chan;
	unsigned char hwa[IEEE802154_ADDR_LEN];
	int ret;
	uint8_t cap =  0
			| (1 << 1) /* FFD */
			| (1 << 3) /* Receiver ON */
			/*| (1 << 7) */ /* allocate short */
			;

	if (!cmd->argv[1])
		return IZ_STOP_ERR;
	NLA_PUT_STRING(msg, IEEE802154_ATTR_DEV_NAME, cmd->argv[1]);

	if (!cmd->argv[2])
		return IZ_STOP_ERR;
	pan_id = strtol(cmd->argv[2], &dummy, 16);
	if (*dummy) {
		printf("Bad PAN ID!\n");
		return IZ_STOP_ERR;
	}

	if (!cmd->argv[3])
		return IZ_STOP_ERR;
	if (cmd->argv[3][0] == 'H' || cmd->argv[3][0] == 'h') {
		ret = parse_hw_addr(cmd->argv[3]+1, hwa);
		if (ret) {
			printf("Bad coordinator address!\n");
			return IZ_STOP_ERR;
		}
		NLA_PUT(msg, IEEE802154_ATTR_COORD_HW_ADDR,
			IEEE802154_ADDR_LEN, hwa);
	} else {
		coord_short_addr = strtol(cmd->argv[3], &dummy, 16);
		if (*dummy) {
			printf("Bad coordinator address!\n");
			return IZ_STOP_ERR;
		}
		NLA_PUT_U16(msg, IEEE802154_ATTR_COORD_SHORT_ADDR,
			coord_short_addr);
	}


	if (!cmd->argv[4])
		return IZ_STOP_ERR;
	chan = strtol(cmd->argv[4], &dummy, 10);
	if (*dummy) {
		printf("Bad channel number!\n");
		return IZ_STOP_ERR;
	}

	if (cmd->argv[5]) {
		if (strcmp(cmd->argv[5], "short") ||
			cmd->argv[6])
			return IZ_STOP_ERR;
		else
			cap |= 1 << 7; /* Request short addr */
	}

	NLA_PUT_U8(msg, IEEE802154_ATTR_CHANNEL, chan);
	NLA_PUT_U16(msg, IEEE802154_ATTR_COORD_PAN_ID, pan_id);
	NLA_PUT_U8(msg, IEEE802154_ATTR_CAPABILITY, cap);

	return IZ_CONT_OK;

nla_put_failure:
	return IZ_STOP_ERR;
}

static iz_res_t assoc_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	if (!attrs[IEEE802154_ATTR_SHORT_ADDR] ||
		!attrs[IEEE802154_ATTR_STATUS] )
		return IZ_STOP_ERR;

	printf("Received short address %04hx, status %02hhx\n",
		nla_get_u16(attrs[IEEE802154_ATTR_SHORT_ADDR]),
		nla_get_u8(attrs[IEEE802154_ATTR_STATUS]));

	return IZ_STOP_OK;
}

/*************************/
/* DISASSOCIATE handling */
/*************************/

static iz_res_t disassoc_parse(struct iz_cmd *cmd)
{
	cmd->flags = NLM_F_REQUEST;
	return IZ_CONT_OK;
}

static iz_res_t disassoc_request(struct iz_cmd *cmd, struct nl_msg *msg)
{
	char *dummy;
	uint8_t reason;
	unsigned char hwa[IEEE802154_ADDR_LEN];
	uint16_t  short_addr;
	int ret;

	if (!cmd->argv[1])
		return IZ_STOP_ERR;
	NLA_PUT_STRING(msg, IEEE802154_ATTR_DEV_NAME, cmd->argv[1]);

	if (!cmd->argv[2])
		return IZ_STOP_ERR;
	if (cmd->argv[2][0] == 'H' || cmd->argv[2][0] == 'h') {
		ret = parse_hw_addr(cmd->argv[2]+1, hwa);
		if (ret) {
			printf("Bad destination address!\n");
			return IZ_STOP_ERR;
		}
		NLA_PUT(msg, IEEE802154_ATTR_DEST_HW_ADDR,
			IEEE802154_ADDR_LEN, hwa);
	} else {
		short_addr = strtol(cmd->argv[2], &dummy, 16);
		if (*dummy) {
			printf("Bad destination address!\n");
			return IZ_STOP_ERR;
		}
		NLA_PUT_U16(msg, IEEE802154_ATTR_DEST_SHORT_ADDR, short_addr);
	}

	if (!cmd->argv[3])
		return IZ_STOP_ERR;
	reason = strtol(cmd->argv[3], &dummy, 16);
	if (*dummy) {
		printf("Bad disassociation reason!\n");
		return IZ_STOP_ERR;
	}

	if (cmd->argv[4])
		return IZ_STOP_ERR;

	NLA_PUT_U8(msg, IEEE802154_ATTR_REASON, reason);

	return IZ_CONT_OK;

nla_put_failure:
	return IZ_STOP_ERR;

}

static iz_res_t disassoc_response(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs)
{
	/* Hmm... TODO? */
	printf("Done.\n");

	return IZ_STOP_OK;
}

const struct iz_cmd_desc mac_commands[] = {
	{
		.name		= "scan",
		.usage		= "<iface> <ed|active|passive|orphan> <channels> <duration>",
		.doc		= "Perform network scanning on specified channels.",
		.nl_cmd		= IEEE802154_SCAN_REQ,
		.nl_resp	= IEEE802154_SCAN_CONF,
		.parse		= scan_parse,
		.request	= scan_request,
		.response	= scan_response,
	},
	{
		.name		= "assoc",
		.usage		= "<iface> <pan> <coord> <chan> ['short']",
		.doc		= "Associate with a given network via coordinator.",
		.nl_cmd		= IEEE802154_ASSOCIATE_REQ,
		.nl_resp	= IEEE802154_ASSOCIATE_CONF,
		.parse		= assoc_parse,
		.request	= assoc_request,
		.response	= assoc_response,
	},
	{
		.name		= "disassoc",
		.usage		= "<iface> <addr> <reason>",
		.doc		= "Disassociate from a network.",
		.nl_cmd		= IEEE802154_DISASSOCIATE_REQ,
		.nl_resp	= IEEE802154_DISASSOCIATE_CONF,
		.parse		= disassoc_parse,
		.request	= disassoc_request,
		.response	= disassoc_response,
	},
	{
		.name		= "list",
		.usage		= "[iface]",
		.doc		= "List interface(s).",
		.nl_cmd		= IEEE802154_LIST_IFACE,
		.nl_resp	= IEEE802154_LIST_IFACE,
		.parse		= list_parse,
		.request	= list_request,
		.response	= list_response,
		.finish		= list_finish,
	},
	{}
};

