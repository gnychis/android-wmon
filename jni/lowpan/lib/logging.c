/*
 * Linux IEEE 802.15.4 userspace tools
 *
 * Copyright (C) 2008, 2009 Sismens AG
 *
 * Written-by: Dmitry Eremin-Solenikov
 * Written-by: Sergey Lapin
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
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#ifdef HAVE_SYSLOG_H
#include <syslog.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

static int log_level = 0;

static void log_string(int level, char *s)
{
#ifdef HAVE_SYSLOG_H
	if (level <= log_level)
		syslog(LOG_INFO, "%s", s);
#endif
}

void init_log(char * name, int level)
{
#ifdef HAVE_SYSLOG_H
	log_level = level;
	openlog(name, LOG_PID|LOG_CONS|LOG_NOWAIT, LOG_DAEMON);
#endif
}

void log_msg(int level, char * format, ...)
{
	int n;
	int size = 100;
	va_list ap;
	char *p, *np;
	p = malloc(size);
	if (!p)
		return;
	while(1) {
		va_start(ap, format);
		n = vsnprintf(p, size, format, ap);
		va_end(ap);                                               
		if (n > -1 && n < size) {
			log_string(level, p);
			free(p);
			return;
		}
		/* Else try again with more space. */
		if (n > -1)    /* glibc 2.1 */
			size = n+1; /* precisely what is needed */
		else           /* glibc 2.0 */
			size *= 2;  /* twice the old size */
		if ((np = realloc (p, size)) == NULL) {
	    		free(p);
			return;
		} else {
			p = np;
		}
	}
}
