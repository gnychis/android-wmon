/*
 * netlink/netlink.h		Netlink Interface
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation version 2.1
 *	of the License.
 *
 * Copyright (c) 2003-2006 Thomas Graf <tgraf@suug.ch>
 */

#ifndef NETLINK_NETLINK_H_
#define NETLINK_NETLINK_H_

#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/time.h>
#include <netdb.h>
#include <netlink/netlink-compat.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <linux/genetlink.h>
#include <linux/netfilter/nfnetlink.h>
#include <netlink/version.h>
#include <netlink/errno.h>
#include <netlink/types.h>
#include <netlink/handlers.h>
#include <netlink/socket.h>

#ifdef __cplusplus
extern "C" {
#endif

#define NLA_F_NESTED		(1 << 15)
#define NLA_F_NET_BYTEORDER	(1 << 14)
#define NLA_TYPE_MASK		~(NLA_F_NESTED | NLA_F_NET_BYTEORDER)

#  define EAI_NODATA	  -5	/* No address associated with NAME.  */
#  define EAI_ADDRFAMILY  -9	/* Address family for NAME not supported.  */
#  define EAI_INPROGRESS  -100	/* Processing request in progress.  */
#  define EAI_CANCELED	  -101	/* Request canceled.  */
#  define EAI_NOTCANCELED -102	/* Request not canceled.  */
#  define EAI_ALLDONE	  -103	/* All requests done.  */
#  define EAI_INTR	  -104	/* Interrupted by a signal.  */
#  define EAI_IDN_ENCODE  -105	/* IDN encoding failed.  */

#define NETLINK_SCSITRANSPORT      18
#define NETLINK_ECRYPTFS   19

#define	IFA_F_NODAD		0x02
#define IFA_F_OPTIMISTIC	0x04
#define IFA_F_DADFAILED		0x08
#define	IFA_F_HOMEADDRESS	0x10
#define IFA_F_DEPRECATED	0x20
#define IFA_F_TENTATIVE		0x40
#define IFA_F_PERMANENT		0x80

#define IFF_ECHO        0x40000

struct ucred;

extern int nl_debug;
extern struct nl_dump_params nl_debug_dp;

/* Connection Management */
extern int			nl_connect(struct nl_sock *, int);
extern void			nl_close(struct nl_sock *);

/* Send */
extern int			nl_sendto(struct nl_sock *, void *, size_t);
extern int			nl_sendmsg(struct nl_sock *, struct nl_msg *,
					   struct msghdr *);
extern int			nl_send(struct nl_sock *, struct nl_msg *);
extern int			nl_send_auto_complete(struct nl_sock *,
						      struct nl_msg *);
extern int			nl_send_simple(struct nl_sock *, int, int,
					       void *, size_t);

/* Receive */
extern int			nl_recv(struct nl_sock *,
					struct sockaddr_nl *, unsigned char **,
					struct ucred **);

extern int			nl_recvmsgs(struct nl_sock *, struct nl_cb *);

extern int			nl_recvmsgs_default(struct nl_sock *);

extern int			nl_wait_for_ack(struct nl_sock *);

/* Netlink Family Translations */
extern char *			nl_nlfamily2str(int, char *, size_t);
extern int			nl_str2nlfamily(const char *);

#ifdef __cplusplus
}
#endif

#endif
