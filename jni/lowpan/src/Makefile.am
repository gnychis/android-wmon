include $(top_srcdir)/Makefile.common

sbin_PROGRAMS = izattach izcoordinator iz
bin_PROGRAMS = izchat

manpages = izcoordinator.8 iz.8 izattach.8 izchat.1
izattach_DESC = "attach a serial device to IEEE 802.15.4 stack"
izcoordinator_DESC = "simple coordinator for IEEE 802.15.4 network"
iz_DESC = "configure an IEEE 802.15.4 interface"
izchat_DESC = "simple chat program using IEEE 802.15.4"

if MANPAGES
dist_man_MANS = $(manpages)
endif
EXTRA_DIST = $(manpages)

izattach_SOURCES = serial.c

iz_SOURCES = iz.c iz-common.c iz-mac.c iz-phy.c
noinst_HEADERS = iz.h

izcoordinator_SOURCES = coordinator.c
izcoordinator_CFLAGS = $(AM_CFLAGS) $(NL_CFLAGS) -DLEASE_FILE=\"$(leasefile)\"
izcoordinator_CFLAGS += -DPID_FILE=\"$(pidfile)\"
izcoordinator_LDADD = ../addrdb/libaddrdb.la $(LDADD) $(NL_LIBS) $(LEXLIB)

iz_CFLAGS = $(AM_CFLAGS) $(NL_CFLAGS)
iz_LDADD = $(LDADD) $(NL_LIBS)

izattach.8: $(izattach_SOURCES) $(top_srcdir)/configure.ac
	-$(HELP2MAN) -o $@ -s 8 -N -n $(izattach_DESC) $(builddir)/izattach

izcoordinator.8: $(izcoordinator_SOURCES) $(top_srcdir)/configure.ac
	-$(HELP2MAN) -o $@ -s 8 -N -n $(izcoordinator_DESC) $(builddir)/izcoordinator

iz.8: $(iz_SOURCES) $(top_srcdir)/configure.ac
	-$(HELP2MAN) -o $@ -s 8 -N -n $(iz_DESC) $(builddir)/iz

izchat.1: $(izchat_SOURCES) $(top_srcdir)/configure.ac
	-$(HELP2MAN) -o $@ -s 1 -N -n $(izchat_DESC) $(builddir)/izchat

install-data-hook:
	$(mkinstalldirs) $(DESTDIR)`dirname $(leasefile)`
	$(mkinstalldirs) $(DESTDIR)`dirname $(pidfile)`

