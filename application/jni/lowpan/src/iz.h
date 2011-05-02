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

#ifndef IZ_H
#define IZ_H

struct iz_cmd;
struct nl_msg;
struct nlattr;
struct genlmsghdr;

typedef enum {
	IZ_CONT_OK,
	IZ_STOP_OK,
	IZ_STOP_ERR,
} iz_res_t;

/* iz command descriptor */
struct iz_cmd_desc {
	const char *name;	/* Name (as in command line) */
	const char *usage;	/* Arguments list */
	const char *doc;	/* One line command description */
	unsigned char nl_cmd;	/* NL command ID */
	unsigned char nl_resp;	/* NL command response ID (optional) */
	unsigned listener : 1;	/* Listen for all events */

	/* Parse command line, fill in iz_cmd struct. */
	/* You must set cmd->flags here! */
	iz_res_t (*parse)(struct iz_cmd *cmd);

	/* Prepare an outgoing netlink message */
	/* If request is not defined, we go to receive wo sending message */
	iz_res_t (*request)(struct iz_cmd *cmd, struct nl_msg *msg);

	/* Handle an incoming netlink message */
	iz_res_t (*response)(struct iz_cmd *cmd, struct genlmsghdr *ghdr, struct nlattr **attrs);

	/* Handle the end of multipart message */
	iz_res_t (*finish)(struct iz_cmd *cmd);
};

/* Parsed command results */
struct iz_cmd {
	int argc;	/* number of arguments to the command */
	char **argv;	/* NULL-terminated arrays of arguments */

	const struct iz_cmd_desc *desc;

	/* Fields below are prepared by parse function */
	int flags;	/* NL message flags */
	char *iface;	/* Interface for a command */
	char *phy;	/* Phy for a command */

	/* Filled before calling response */
	uint32_t seq;
};

extern const struct iz_cmd_desc iz_commands[];
extern const struct iz_cmd_desc mac_commands[];
extern const struct iz_cmd_desc phy_commands[];

void iz_help(const char *pname);
const struct iz_cmd_desc *get_cmd(const char *name);

#endif
