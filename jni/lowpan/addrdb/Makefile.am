include $(top_srcdir)/Makefile.common

AM_YFLAGS = -d --report=all
BUILT_SOURCE = coord-config-parse.h

noinst_LTLIBRARIES = libaddrdb.la
check_PROGRAMS = parsetest
check_SCRIPTS = parsetest-check.sh
TESTS = parsetest-check.sh

libaddrdb_la_SOURCES = coord-config-parse.y coord-config-lex.l addrdb.c parser.h
parsetest_CFLAGS = $(AM_CFLAGS) -DLEASE_FILE=\"$(leasefile)\"
parsetest_LDADD = libaddrdb.la $(LDADD)

EXTRA_DIST = $(TESTS)
