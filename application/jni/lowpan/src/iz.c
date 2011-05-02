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

#include <errno.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <netlink/netlink.h>
#include <netlink/genl/genl.h>
#include <netlink/genl/ctrl.h>
#include <getopt.h>

#include <ieee802154.h>
#include <nl802154.h>
#include <libcommon.h>

#include "iz.h"

static int iz_cb_seq_check(struct nl_msg *msg, void *arg);
static int iz_cb_valid(struct nl_msg *msg, void *arg);
static int iz_cb_finish(struct nl_msg *msg, void *arg);

#ifdef HAVE_GETOPT_LONG
static const struct option iz_long_opts[] = {
	{ "debug", optional_argument, NULL, 'd' },
	{ "version", no_argument, NULL, 'v' },
	{ "help", no_argument, NULL, 'h' },
	{ NULL, 0, NULL, 0 },
};
#endif

/* Expected sequence number */
static int iz_seq = 0;

/* Parsed options */
static int iz_debug = 0;

#define dprintf(lvl, fmt...)			\
	do {					\
		if (iz_debug >= lvl)		\
			printf(fmt);		\
	} while(0)


/* Exit from receive loop (set from receive callback) */
static int iz_exit = 0;

const struct iz_cmd_desc *get_cmd(const char *name)
{
	int i;

	for (i = 0; iz_commands[i].name; i++) {
		if (!strcmp(name, iz_commands[i].name)) {
			return &iz_commands[i];
		}
	}

	for (i = 0; mac_commands[i].name; i++) {
		if (!strcmp(name, mac_commands[i].name)) {
			return &mac_commands[i];
		}
	}

	for (i = 0; phy_commands[i].name; i++) {
		if (!strcmp(name, phy_commands[i].name)) {
			return &phy_commands[i];
		}
	}

	return NULL;
}

int main(int argc, char **argv)
{
	int c;
	int i;
	int family;
	int group;
	struct nl_handle *nl;
	struct nl_msg *msg;
	char *dummy = NULL;

	/* Currently processed command info */
	struct iz_cmd cmd;


	/* Parse options */
	while (1) {
#ifdef HAVE_GETOPT_LONG
		int opt_idx = -1;
		c = getopt_long(argc, argv, "d::vh", iz_long_opts, &opt_idx);
#else
		c = getopt(argc, argv, "d::vh");
#endif
		if (c == -1)
			break;

		switch(c) {
		case 'd':
			if (optarg) {
				i = strtol(optarg, &dummy, 10);
				if (*dummy == '\0')
					iz_debug = nl_debug = i;
				else {
					fprintf(stderr, "Error: incorrect debug level: '%s'\n", optarg);
					exit(1);
				}
			} else
				iz_debug = nl_debug = 1;
			break;
		case 'v':
			printf(	"iz " VERSION "\n"
				"Copyright (C) 2008, 2009 by Siemens AG\n"
				"License GPLv2 GNU GPL version 2 <http://gnu.org/licenses/gpl.html>.\n"
				"This is free software: you are free to change and redistribute it.\n"
				"There is NO WARRANTY, to the extent permitted by law.\n"
				"\n"
				"Written by Dmitry Eremin-Solenikov, Sergey Lapin and Maxim Osipov\n");
			return 0;
		case 'h':
			iz_help(argv[0]);
			return 0;
		default:
			iz_help(argv[0]);
			return 1;
		}
	}
	if (optind >= argc) {
		iz_help(argv[0]);
		return 1;
	}

	memset(&cmd, 0, sizeof(cmd));

	cmd.argc = argc - optind;
	cmd.argv = argv + optind;

	/* Parse command */
	cmd.desc = get_cmd(argv[optind]);
	if (!cmd.desc) {
		printf("Unknown command %s!\n", argv[optind]);
		return 1;
	}
	if (cmd.desc->parse) {
		i = cmd.desc->parse(&cmd);
		if (i == IZ_STOP_OK) {
			return 0;
		} else if (i == IZ_STOP_ERR) {
			printf("Command line parsing error!\n");
			return 1;
		}
	}

	/* Prepare NL command */
	nl = nl_handle_alloc();
	if (!nl) {
		nl_perror("Could not allocate NL handle");
		return 1;
	}
	genl_connect(nl);
	family = genl_ctrl_resolve(nl, IEEE802154_NL_NAME);
	group = nl_get_multicast_id(nl,
			IEEE802154_NL_NAME, IEEE802154_MCAST_COORD_NAME);
	if (group < 0) {
		fprintf(stderr, "Could not get multicast group ID: %s\n", strerror(-group));
		return 1;
	}
	nl_socket_add_membership(nl, group);
	iz_seq = nl_socket_use_seq(nl) + 1;
	nl_socket_modify_cb(nl, NL_CB_VALID, NL_CB_CUSTOM,
		iz_cb_valid, (void*)&cmd);
	nl_socket_modify_cb(nl, NL_CB_FINISH, NL_CB_CUSTOM,
		iz_cb_finish, (void*)&cmd);
	nl_socket_modify_cb(nl, NL_CB_SEQ_CHECK, NL_CB_CUSTOM,
		iz_cb_seq_check, (void*)&cmd);

	/* Send request, if necessary */
	if (cmd.desc->request) {
		msg = nlmsg_alloc();
		if (!msg) {
			nl_perror("Could not allocate NL message!\n");
			return 1;
		}
		genlmsg_put(msg, NL_AUTO_PID, NL_AUTO_SEQ, family, 0,
			cmd.flags, cmd.desc->nl_cmd, 1);

		if (cmd.desc->request(&cmd, msg) != IZ_CONT_OK) {
			printf("Request processing error!\n");
			return 1;
		}

		dprintf(1, "nl_send_auto_complete\n");
		nl_send_auto_complete(nl, msg);
		cmd.seq = nlmsg_hdr(msg)->nlmsg_seq;

		dprintf(1, "nlmsg_free\n");
		nlmsg_free(msg);
	}

	/* Received message handling loop */
	while (iz_exit == IZ_CONT_OK) {
		if(nl_recvmsgs_default(nl)) {
			nl_perror("Receive failed");
			return 1;
		}
	}
	nl_close(nl);

	if (iz_exit == IZ_STOP_ERR)
		return 1;

	return 0;
}

void iz_help(const char *pname)
{
	int i;

	printf("Usage: %s [options] [command]\n", pname);
	printf("Manage IEEE 802.15.4 network interfaces\n\n");

	printf("Options:\n");
	printf("  -d, --debug[=N]                print netlink messages and other debug information\n");
	printf("  -v, --version                  print version\n");
	printf("  -h, --help                     print help\n");

	/* Print short help for available commands */
	printf("\nCommon commands:\n");
	for (i = 0; iz_commands[i].name; i++) {
		printf("  %s  %s\n     %s\n\n", iz_commands[i].name,
			iz_commands[i].usage,
			iz_commands[i].doc);
	}

	printf("\nPHY 802.15.4 commands:\n");
	for (i = 0; phy_commands[i].name; i++) {
		printf("  %s  %s\n     %s\n\n", phy_commands[i].name,
			phy_commands[i].usage,
			phy_commands[i].doc);
	}

	printf("\nMAC 802.15.4 commands:\n");
	for (i = 0; mac_commands[i].name; i++) {
		printf("  %s  %s\n     %s\n\n", mac_commands[i].name,
			mac_commands[i].usage,
			mac_commands[i].doc);
	}

	printf("\n");
	printf("Report bugs to " PACKAGE_BUGREPORT "\n\n");
	printf(PACKAGE_NAME " homepage <" PACKAGE_URL ">\n");
}

/* Callback for sequence number check */
static int iz_cb_seq_check(struct nl_msg *msg, void *arg)
{
	uint32_t seq;

	if(nlmsg_get_src(msg)->nl_groups)
		return NL_OK;

	seq = nlmsg_hdr(msg)->nlmsg_seq;
	if (seq == iz_seq) {
		if (!(nlmsg_hdr(msg)->nlmsg_flags & NLM_F_MULTI))
			iz_seq ++;
		return NL_OK;
	}
	printf("Sequence number mismatch (%i, %i)!", seq, iz_seq);
	return NL_SKIP;
}

/* Callback for received valid messages */
static int iz_cb_valid(struct nl_msg *msg, void *arg)
{
	struct nlmsghdr *nlh = nlmsg_hdr(msg);
	struct nlattr *attrs[IEEE802154_ATTR_MAX+1];
        struct genlmsghdr *ghdr;
	struct iz_cmd *cmd = arg;

	/* Validate message and parse attributes */
	genlmsg_parse(nlh, 0, attrs, IEEE802154_ATTR_MAX, ieee802154_policy);

        ghdr = nlmsg_data(nlh);

	dprintf(1, "Received command %d (%d) for interface\n",
			ghdr->cmd, ghdr->version);

	if (cmd->desc->listener || cmd->desc->nl_resp == ghdr->cmd) {
		iz_exit = cmd->desc->response(cmd, ghdr, attrs);
	}

	return 0;
}

/* Callback for the end of the multipart message */
static int iz_cb_finish(struct nl_msg *msg, void *arg)
{
	struct nlmsghdr *nlh = nlmsg_hdr(msg);
	struct iz_cmd *cmd = arg;

	dprintf(1, "Received finish for interface\n");

	if (cmd->seq == nlh->nlmsg_seq) {
		if (cmd->desc->finish)
			iz_exit = cmd->desc->finish(cmd);
		else
			iz_exit = IZ_STOP_ERR;
	}

	return 0;
}

