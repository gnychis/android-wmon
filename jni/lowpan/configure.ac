#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

AC_PREREQ(2.61)
AC_INIT([Userspace tools for Linux IEEE 802.15.4 stack],
	[0.2.2],[linux-zigbee-devel@lists.sourceforge.net],
	[lowpan-tools], [http://linux-zigbee.sourceforge.net/])
m4_ifndef([AC_PACKAGE_URL],
	  [AC_SUBST([PACKAGE_URL], [http://linux-zigbee.sourceforge.net/])
	  AC_DEFINE_UNQUOTED([PACKAGE_URL], ["$PACKAGE_URL"],
			     [Define to the home page for this package.])])
AC_CONFIG_AUX_DIR([misc])
AC_CONFIG_MACRO_DIR([m4])
AM_INIT_AUTOMAKE([-Wall -Werror gnu])
AM_MAINTAINER_MODE([enable])
AC_CONFIG_SRCDIR([src/serial.c])
AC_CONFIG_HEADER([config.h])

AC_ARG_WITH(zbtestdir, [AC_HELP_STRING([--with-zbtestdir],
	    [directory where all test programs will be installed])],
	    [zbtestdir=$withval],
	    [zbtestdir='${libexecdir}/lowpan-tools'])
AC_SUBST(zbtestdir)

AC_ARG_WITH(iproutedir, [AC_HELP_STRING([--with-iproutedir],
	    [directory where iproute2 plugins will be installed])],
	    [iproutedir=$withval],
	    [iproutedir='${libdir}/ip'])
AC_SUBST(iproutedir)

AC_ARG_WITH(leasefile, [AC_HELP_STRING([--with-leasefile],
	    [file for persistent lease storage])],
	    [leasefile=$withval],
	    [leasefile='${localstatedir}/lib/lowpan-tools/izcoordinator.leases'])
AC_SUBST(leasefile)

AC_ARG_ENABLE([manpages],[AS_HELP_STRING([--disable-manpages],
	      [disable manpages generation and installation])],
	[case "${enableval}" in
		yes | true) manpages=yes ;;
		no | fale) manpages=no ;;
		*) AC_MSG_ERROR([bad value ${enableval} for --enable-manpages]) ;;
	esac], [manpages=yes])
AM_CONDITIONAL([MANPAGES], [test x$manpages = xyes])

AC_ARG_ENABLE([ip-plugin],[AS_HELP_STRING([--enable-ip-plugin],
	      [enables iptools plugin for devices management (obsolete)])],
	 [case "${enableval}" in
		yes | true) ipplugin=yes ;;
		no | fale) ipplugin=no ;;
		*) AC_MSG_ERROR([bad value ${enableval} for --enable-ip-plugin]) ;;
	esac], [ipplugin=no])
AM_CONDITIONAL([IP_PLUGIN], [test x$ipplugin = xyes])

AC_ARG_ENABLE([kernel-compat],[AS_HELP_STRING([--enable-kernel-compat],
	      [enable compatibility with older kernels])],
	[case "${enableval}" in
		yes | true) kernelcompat=yes ;;
		no | false) kernelcompat=no ;;
		*) AC_MSG_ERROR([bad value ${enableval} for --enable-kernel-compat]) ;;
	esac], [kernelcompat=yes])
if test "x$kernelcompat" == "xyes"
then
	AC_DEFINE([ENABLE_KERNEL_COMPAT], [1],
		  [Define to 1 if you want to enable compatibility with older kernels])
fi

AC_ARG_WITH(pidfile, [AC_HELP_STRING([--with-pidfile],
	    [pid file location])],
	    [pidfile=$withval],
	    [pidfile='${localstatedir}/run/izcoordinator.pid'])
AC_SUBST(pidfile)

# Checks for programs.
AC_PROG_CC
AM_PROG_CC_C_O
AC_PROG_CPP
AC_PROG_INSTALL
AC_PROG_LN_S
AC_PROG_MAKE_SET
AC_PROG_LIBTOOL
AM_PROG_LEX
AC_PROG_YACC
AC_SUBST(LIBTOOL_DEPS)
AM_PATH_PYTHON(,, [:])
AM_MISSING_PROG(HELP2MAN, help2man, $missing_dir)

# Checks for libraries.
PKG_CHECK_MODULES([NL], [libnl-1])

# Checks for header files.
AC_HEADER_STDC
AC_FUNC_ALLOCA
AC_CHECK_HEADERS([arpa/inet.h fcntl.h inttypes.h libintl.h limits.h malloc.h stddef.h stdint.h stdlib.h string.h sys/ioctl.h sys/socket.h sys/time.h syslog.h termios.h unistd.h])
AC_CHECK_HEADERS([net/ieee802154.h net/af_ieee802154.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_TYPE_UID_T
AC_TYPE_INT16_T
AC_TYPE_INT32_T
AC_TYPE_INT8_T
AC_TYPE_SIZE_T
AC_TYPE_UINT16_T
AC_TYPE_UINT32_T
AC_TYPE_UINT64_T
AC_TYPE_UINT8_T
AC_CHECK_TYPE(struct sockaddr_ieee802154,
	      [AC_DEFINE([HAVE_STRUCT_SOCKADDR_IEEE802154],[],
			 [Define to 1 if you have struct sockaddr_ieee802154]
			)],[],
	      [#include <sys/types.h>
	       #include <sys/socket.h>
	       #ifdef HAVE_NET_IEEE802154_H
	       #include <net/ieee802154.h>
	       #endif
	       #ifdef HAVE_NET_AF_IEEE802154_H
	       #include <net/af_ieee802154.h>
	       #endif
	       ])

# Checks for library functions.
AC_FUNC_FORK
AC_CHECK_FUNCS([malloc realloc memset socket getopt_long select strdup strtol])

# Handle this last so -Werror doesn't get in the way of compile tests
AC_ARG_ENABLE(werror, [AC_HELP_STRING([--disable-werror],
	      [do not compile with -Werror])])
if test "x$enable_werror" != "xno" ; then
	CFLAGS="-Werror $CFLAGS"
fi

AC_CONFIG_FILES([
	Makefile
	lib/Makefile
	addrdb/Makefile
	src/Makefile
	iplink/Makefile
	tests/Makefile
	test-serial/Makefile
])
AC_OUTPUT
