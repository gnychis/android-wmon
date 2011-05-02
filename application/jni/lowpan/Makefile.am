include $(top_srcdir)/Makefile.common

ACLOCAL_AMFLAGS = -I m4

SUBDIRS = lib addrdb src tests test-serial
if IP_PLUGIN
SUBDIRS += iplink
endif

include_HEADERS = include/ieee802154.h include/nl802154.h
noinst_HEADERS = include/libcommon.h include/addrdb.h include/logging.h

EXTRA_DIST = $(srcdir)/debian/changelog $(srcdir)/debian/compat $(srcdir)/debian/control $(srcdir)/debian/copyright \
	     $(srcdir)/debian/rules $(srcdir)/debian/watch $(srcdir)/debian/source/format \
	     $(srcdir)/debian/dirs $(srcdir)/debian/docs $(srcdir)/debian/*.examples $(srcdir)/debian/*.install \
	     $(srcdir)/include/linux

libtool: $(LIBTOOL_DEPS)
	$(SHELL) ./config.status --recheck

resync:
	if [ -z "$(KDIR)" ] ; then echo "please set KDIR variable" ; exit 1 ; fi
	cp $(KDIR)/include/linux/nl802154.h include/
	patch include/nl802154.h include/nl802154.h.patch
	cp $(KDIR)/net/ieee802154/nl_policy.c lib/
	patch lib/nl_policy.c lib/nl_policy.c.patch
	[ ! -z "$(NOCOMMIT)" ] || git commit -s include/linux/mac802154.h include/nl802154.h lib/nl_policy.c -m "Resync with kernel"
