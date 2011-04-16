dnl Macros that test for specific features.
dnl This file is part of the Autoconf packaging for Wireshark.
dnl Copyright (C) 1998-2000 by Gerald Combs.
dnl
dnl $Id: acinclude.m4 33509 2010-07-13 23:26:07Z guy $
dnl
dnl This program is free software; you can redistribute it and/or modify
dnl it under the terms of the GNU General Public License as published by
dnl the Free Software Foundation; either version 2, or (at your option)
dnl any later version.
dnl
dnl This program is distributed in the hope that it will be useful,
dnl but WITHOUT ANY WARRANTY; without even the implied warranty of
dnl MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
dnl GNU General Public License for more details.
dnl
dnl You should have received a copy of the GNU General Public License
dnl along with this program; if not, write to the Free Software
dnl Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
dnl 02111-1307, USA.
dnl
dnl As a special exception, the Free Software Foundation gives unlimited
dnl permission to copy, distribute and modify the configure scripts that
dnl are the output of Autoconf.  You need not follow the terms of the GNU
dnl General Public License when using or distributing such scripts, even
dnl though portions of the text of Autoconf appear in them.  The GNU
dnl General Public License (GPL) does govern all other use of the material
dnl that constitutes the Autoconf program.
dnl
dnl Certain portions of the Autoconf source text are designed to be copied
dnl (in certain cases, depending on the input) into the output of
dnl Autoconf.  We call these the "data" portions.  The rest of the Autoconf
dnl source text consists of comments plus executable code that decides which
dnl of the data portions to output in any given case.  We call these
dnl comments and executable code the "non-data" portions.  Autoconf never
dnl copies any of the non-data portions into its output.
dnl
dnl This special exception to the GPL applies to versions of Autoconf
dnl released by the Free Software Foundation.  When you make and
dnl distribute a modified version of Autoconf, you may extend this special
dnl exception to the GPL to apply to your modified version as well, *unless*
dnl your modified version has the potential to copy into its output some
dnl of the text that was the non-data portion of the version that you started
dnl with.  (In other words, unless your change moves or copies text from
dnl the non-data portions to the data portions.)  If your modification has
dnl such potential, you must delete any notice of this special exception
dnl to the GPL from your modified version.
dnl
dnl Written by David MacKenzie, with help from
dnl Franc,ois Pinard, Karl Berry, Richard Pixley, Ian Lance Taylor,
dnl Roland McGrath, Noah Friedman, david d zuhn, and many others.

#
# AC_WIRESHARK_ADD_DASH_L
#
# Add to the variable specified as the first argument a "-L" flag for the
# directory specified as the second argument, and, on Solaris, add a
# "-R" flag for it as well.
#
# XXX - IRIX, and other OSes, may require some flag equivalent to
# "-R" here.
#
AC_DEFUN([AC_WIRESHARK_ADD_DASH_L],
[$1="$$1 -L$2"
case "$host_os" in
  solaris*)
    $1="$$1 -R$2"
  ;;
esac
])

#
# AC_WIRESHARK_TIMEZONE_ABBREV
#

AC_DEFUN([AC_WIRESHARK_TIMEZONE_ABBREV],
[
  AC_CACHE_CHECK([for tm_zone in struct tm],
    ac_cv_wireshark_have_tm_zone,
    [
      AC_TRY_COMPILE(
        [#include <time.h>],
        [struct tm t; t.tm_zone;],
        ac_cv_wireshark_have_tm_zone=yes,
        ac_cv_wireshark_have_tm_zone=no)
    ])
  if test $ac_cv_wireshark_have_tm_zone = yes; then
    AC_DEFINE(HAVE_TM_ZONE, 1, [Define if tm_zone field exists in struct tm])
  else
    AC_CACHE_CHECK([for tzname],
      ac_cv_wireshark_have_tzname,
      [
        AC_TRY_LINK(
[#include <time.h>
#include <stdio.h>],
          [printf("%s", tzname[0]);],
          ac_cv_wireshark_have_tzname=yes,
          ac_cv_wireshark_have_tzname=no)
      ])
    if test $ac_cv_wireshark_have_tzname = yes; then
      AC_DEFINE(HAVE_TZNAME, 1, [Define if tzname array exists])
    fi
  fi
])


#
# AC_WIRESHARK_STRUCT_SA_LEN
#
dnl AC_STRUCT_ST_BLKSIZE extracted from the file in question,
dnl "acspecific.m4" in GNU Autoconf 2.12, and turned into
dnl AC_WIRESHARK_STRUCT_SA_LEN, which checks if "struct sockaddr"
dnl has the 4.4BSD "sa_len" member, and defines HAVE_SA_LEN; that's
dnl what's in this file.
dnl Done by Guy Harris <guy@alum.mit.edu> on 1998-11-14.

dnl ### Checks for structure members

AC_DEFUN([AC_WIRESHARK_STRUCT_SA_LEN],
[AC_CACHE_CHECK([for sa_len in struct sockaddr], ac_cv_wireshark_struct_sa_len,
[AC_TRY_COMPILE([#include <sys/types.h>
#include <sys/socket.h>], [struct sockaddr s; s.sa_len;],
ac_cv_wireshark_struct_sa_len=yes, ac_cv_wireshark_struct_sa_len=no)])
if test $ac_cv_wireshark_struct_sa_len = yes; then
  AC_DEFINE(HAVE_SA_LEN, 1, [Define if sa_len field exists in struct sockaddr])
fi
])


#
# AC_WIRESHARK_IPV6_STACK
#
# By Jun-ichiro "itojun" Hagino, <itojun@iijlab.net>
#
AC_DEFUN([AC_WIRESHARK_IPV6_STACK],
[
	v6type=unknown
	v6lib=none

	AC_MSG_CHECKING([ipv6 stack type])
	for i in v6d toshiba kame inria zeta linux linux-glibc solaris8; do
		case $i in
		v6d)
			AC_EGREP_CPP(yes, [
#include </usr/local/v6/include/sys/types.h>
#ifdef __V6D__
yes
#endif],
				[v6type=$i; v6lib=v6;
				v6libdir=/usr/local/v6/lib;
				CFLAGS="-I/usr/local/v6/include $CFLAGS"])
			;;
		toshiba)
			AC_EGREP_CPP(yes, [
#include <sys/param.h>
#ifdef _TOSHIBA_INET6
yes
#endif],
				[v6type=$i; v6lib=inet6;
				v6libdir=/usr/local/v6/lib;
				CFLAGS="-DINET6 $CFLAGS"])
			;;
		kame)
			AC_EGREP_CPP(yes, [
#include <netinet/in.h>
#ifdef __KAME__
yes
#endif],
				[v6type=$i; v6lib=inet6;
				v6libdir=/usr/local/v6/lib;
				CFLAGS="-DINET6 $CFLAGS"])
			;;
		inria)
			AC_EGREP_CPP(yes, [
#include <netinet/in.h>
#ifdef IPV6_INRIA_VERSION
yes
#endif],
				[v6type=$i; CFLAGS="-DINET6 $CFLAGS"])
			;;
		zeta)
			AC_EGREP_CPP(yes, [
#include <sys/param.h>
#ifdef _ZETA_MINAMI_INET6
yes
#endif],
				[v6type=$i; v6lib=inet6;
				v6libdir=/usr/local/v6/lib;
				CFLAGS="-DINET6 $CFLAGS"])
			;;
		linux)
			if test -d /usr/inet6; then
				v6type=$i
				v6lib=inet6
				v6libdir=/usr/inet6
				CFLAGS="-DINET6 $CFLAGS"
			fi
			;;
		linux-glibc)
			AC_EGREP_CPP(yes, [
#include <features.h>
#if defined(__GLIBC__) && defined(__GLIBC_MINOR__)
#if (__GLIBC__ == 2 && __GLIBC_MINOR__ >= 1) || __GLIBC__ > 2
yes
#endif
#endif],
			[v6type=$i; v6lib=inet6; CFLAGS="-DINET6 $CFLAGS"])
			;;
		solaris8)
			if test "`uname -s`" = "SunOS" && test "`uname -r`" = "5.8"; then
				v6type=$i
				v6lib=inet6
				[CFLAGS="-DINET6 -DSOLARIS8_INET6 $CFLAGS"]
			fi
			;;
		esac
		if test "$v6type" != "unknown"; then
			break
		fi
	done

	if test "$v6lib" != "none"; then
		for dir in $v6libdir /usr/local/v6/lib /usr/local/lib; do
			if test -d $dir -a -f $dir/lib$v6lib.a; then
				LIBS="-L$dir $LIBS -l$v6lib"
				break
			fi
		done
		enable_ipv6="yes"
	else
		enable_ipv6="no"
	fi
	AC_MSG_RESULT(["$v6type, $v6lib"])
])

#
# AC_WIRESHARK_GETHOSTBY_LIB_CHECK
#
# Checks whether we need "-lnsl" to get "gethostby*()", which we use
# in "resolv.c".
#
# Adapted from stuff in the AC_PATH_XTRA macro in "acspecific.m4" in
# GNU Autoconf 2.13; the comment came from there.
# Done by Guy Harris <guy@alum.mit.edu> on 2000-01-14.
#
AC_DEFUN([AC_WIRESHARK_GETHOSTBY_LIB_CHECK],
[
    # msh@cis.ufl.edu says -lnsl (and -lsocket) are needed for his 386/AT,
    # to get the SysV transport functions.
    # chad@anasazi.com says the Pyramid MIS-ES running DC/OSx (SVR4)
    # needs -lnsl.
    # The nsl library prevents programs from opening the X display
    # on Irix 5.2, according to dickey@clark.net.
    AC_CHECK_FUNC(gethostbyname, ,
	AC_CHECK_LIB(nsl, gethostbyname, NSL_LIBS="-lnsl"))
    AC_SUBST(NSL_LIBS)
])

#
# AC_WIRESHARK_SOCKET_LIB_CHECK
#
# Checks whether we need "-lsocket" to get "socket()", which is used
# by libpcap on some platforms - and, in effect, "gethostby*()" on
# most if not all platforms (so that it can use NIS or DNS or...
# to look up host names).
#
# Adapted from stuff in the AC_PATH_XTRA macro in "acspecific.m4" in
# GNU Autoconf 2.13; the comment came from there.
# Done by Guy Harris <guy@alum.mit.edu> on 2000-01-14.
#
# We use "connect" because that's what AC_PATH_XTRA did.
#
AC_DEFUN([AC_WIRESHARK_SOCKET_LIB_CHECK],
[
    # lieder@skyler.mavd.honeywell.com says without -lsocket,
    # socket/setsockopt and other routines are undefined under SCO ODT
    # 2.0.  But -lsocket is broken on IRIX 5.2 (and is not necessary
    # on later versions), says simon@lia.di.epfl.ch: it contains
    # gethostby* variants that don't use the nameserver (or something).
    # -lsocket must be given before -lnsl if both are needed.
    # We assume that if connect needs -lnsl, so does gethostbyname.
    AC_CHECK_FUNC(connect, ,
      AC_CHECK_LIB(socket, connect, SOCKET_LIBS="-lsocket",
		AC_MSG_ERROR(Function 'socket' not found.), $NSL_LIBS))
    AC_SUBST(SOCKET_LIBS)
])

#
# AC_WIRESHARK_BREAKLOOP_TRY_LINK
#
AC_DEFUN([AC_WIRESHARK_PCAP_BREAKLOOP_TRY_LINK],
[
  AC_LINK_IFELSE(
  [
      AC_LANG_SOURCE(
      [[
#	include <pcap.h>
	int main(void)
	{
	  pcap_t  *pct = NULL;
	  pcap_breakloop(pct);
	  return 0;
	}
      ]])
  ],
  [
    ws_breakloop_compiled=yes
  ],
  [
    ws_breakloop_compiled=no
  ])
])



#
# AC_WIRESHARK_PCAP_CHECK
#
AC_DEFUN([AC_WIRESHARK_PCAP_CHECK],
[
	if test -z "$pcap_dir"
	then
	  # Pcap header checks
	  # XXX need to set a var AC_CHECK_HEADER(pcap.h,,)

	  #
	  # The user didn't specify a directory in which libpcap resides.
	  # First, look for a pcap-config script.
	  #
	  AC_PATH_PROG(PCAP_CONFIG, pcap-config)

	  if test -n "$PCAP_CONFIG" ; then
	    #
	    # Found it.
	    #
	    # Now check whether it's the libpcap 1.0 version, which
	    # put a space after "-L" - on some platforms, that doesn't
	    # work.
	    #
	    AC_MSG_CHECKING(for broken pcap-config)
	    case "`\"$PCAP_CONFIG\" --libs`" in

	    "-L "*)
	      #
	      # Space after -L.  Pretend pcap-config doesn't exist.
	      #
	      AC_MSG_RESULT(yes)
	      PCAP_CONFIG=""
	      ;;

	    *)
	      #
	      # No space after -L.
	      #
	      AC_MSG_RESULT(no)
	      ;;
	    esac
	  fi
	  if test -n "$PCAP_CONFIG" ; then
	    #
	    # Found it, and it's usable; use it to get the include flags
	    # for libpcap.
	    #
	    CFLAGS="$CFLAGS `\"$PCAP_CONFIG\" --cflags`"
	    CPPFLAGS="$CPPFLAGS `\"$PCAP_CONFIG\" --cflags`"
	  else  
	    #
	    # Didn't find it; we have to look for libpcap ourselves.
	    # We assume that the current library search path will work,
	    # but we may have to look for the header in a "pcap"
	    # subdirectory of "/usr/include" or "/usr/local/include",
	    # as some systems apparently put "pcap.h" in a "pcap"
	    # subdirectory, and we also check "$prefix/include" - and
	    # "$prefix/include/pcap", in case $prefix is set to
	    # "/usr/include" or "/usr/local/include".
	    #
	    # XXX - should we just add "$prefix/include" to the include
	    # search path and "$prefix/lib" to the library search path?
	    #
	    AC_MSG_CHECKING(for extraneous pcap header directories)
	    found_pcap_dir=""
	    pcap_dir_list="/usr/include/pcap $prefix/include/pcap $prefix/include"
	    if test "x$ac_cv_enable_usr_local" = "xyes" ; then
	      pcap_dir_list="$pcap_dir_list /usr/local/include/pcap"
	    fi
	    for pcap_dir in $pcap_dir_list
	    do
	      if test -d $pcap_dir ; then
		if test x$pcap_dir != x/usr/include -a x$pcap_dir != x/usr/local/include ; then
		    CFLAGS="$CFLAGS -I$pcap_dir"
		    CPPFLAGS="$CPPFLAGS -I$pcap_dir"
		fi
		found_pcap_dir=" $found_pcap_dir -I$pcap_dir"
		break
	      fi
	    done

	    if test "$found_pcap_dir" != "" ; then
	      AC_MSG_RESULT(found --$found_pcap_dir added to CFLAGS)
	    else
	      AC_MSG_RESULT(not found)
	    fi
	  fi
	else
	  #
	  # The user specified a directory in which libpcap resides,
	  # so add the "include" subdirectory of that directory to
	  # the include file search path and the "lib" subdirectory
	  # of that directory to the library search path.
	  #
	  # XXX - if there's also a libpcap in a directory that's
	  # already in CFLAGS, CPPFLAGS, or LDFLAGS, this won't
	  # make us find the version in the specified directory,
	  # as the compiler and/or linker will search that other
	  # directory before it searches the specified directory.
	  #
	  CFLAGS="$CFLAGS -I$pcap_dir/include"
	  CPPFLAGS="$CPPFLAGS -I$pcap_dir/include"
	  AC_WIRESHARK_ADD_DASH_L(LDFLAGS, $pcap_dir/lib)
	fi

	# Pcap header check
	AC_CHECK_HEADER(pcap.h,,
	    AC_MSG_ERROR([[Header file pcap.h not found; if you installed libpcap
from source, did you also do \"make install-incl\", and if you installed a
binary package of libpcap, is there also a developer's package of libpcap,
and did you also install that package?]]))

	if test -n "$PCAP_CONFIG" ; then
	  #
	  # We have pcap-config; we assume that means we have libpcap
	  # installed and that pcap-config will tell us whatever
	  # libraries libpcap needs.
	  #
	  if test x$enable_static = xyes; then
	    PCAP_LIBS="`\"$PCAP_CONFIG\" --libs --static`"
	  else
	    PCAP_LIBS="`\"$PCAP_CONFIG\" --libs`"
	  fi
	  AC_DEFINE(HAVE_LIBPCAP, 1, [Define to use libpcap library])
	else
	  #
	  # Check to see if we find "pcap_open_live" in "-lpcap".
	  # Also check for various additional libraries that libpcap might
	  # require.
	  #
	  AC_CHECK_LIB(pcap, pcap_open_live,
	    [
	      PCAP_LIBS=-lpcap
	      AC_DEFINE(HAVE_LIBPCAP, 1, [Define to use libpcap library])
	    ], [
	      ac_wireshark_extras_found=no
	      ac_save_LIBS="$LIBS"
	      for extras in "-lcfg -lodm" "-lpfring"
	      do
		AC_MSG_CHECKING([for pcap_open_live in -lpcap with $extras])
		LIBS="-lpcap $extras"
		#
		# XXX - can't we use AC_CHECK_LIB here?
		#
		AC_TRY_LINK(
		    [
#	include <pcap.h>
		    ],
		    [
	pcap_open_live(NULL, 0, 0, 0, NULL);
		    ],
		    [
			ac_wireshark_extras_found=yes
			AC_MSG_RESULT([yes])
			PCAP_LIBS="-lpcap $extras"
			AC_DEFINE(HAVE_LIBPCAP, 1, [Define to use libpcap library])
		    ],
		    [
			AC_MSG_RESULT([no])
		    ])
		if test x$ac_wireshark_extras_found = xyes
		then
		    break
		fi
	      done
	      if test x$ac_wireshark_extras_found = xno
	      then
		AC_MSG_ERROR([Can't link with library libpcap.])
	      fi
	      LIBS=$ac_save_LIBS
	    ], $SOCKET_LIBS $NSL_LIBS)
	fi
	AC_SUBST(PCAP_LIBS)

	#
	# Check whether various variables and functions are defined by
	# libpcap.
	#
	ac_save_LIBS="$LIBS"
	AC_MSG_CHECKING(whether pcap_version is defined by libpcap)
	LIBS="$PCAP_LIBS $SOCKET_LIBS $NSL_LIBS $LIBS"
	AC_TRY_LINK(
	   [
#	include <stdio.h>
	extern char *pcap_version;
	   ],
	   [
	printf ("%s\n", pcap_version);
	   ],
	   ac_cv_pcap_version_defined=yes,
	   ac_cv_pcap_version_defined=no,
	   [echo $ac_n "cross compiling; assumed OK... $ac_c"])
	if test "$ac_cv_pcap_version_defined" = yes ; then
		AC_MSG_RESULT(yes)
		AC_DEFINE(HAVE_PCAP_VERSION, 1, [Define if libpcap version is known])
	else
		AC_MSG_RESULT(no)
	fi
	AC_CHECK_FUNCS(pcap_open_dead pcap_freecode)
	#
	# pcap_breakloop may be present in the library but not declared
	# in the pcap.h header file.  If it's not declared in the header
	# file, attempts to use it will get warnings, and, if we're
	# building with warnings treated as errors, that warning will
	# cause compilation to fail.
	#
	# We are therefore first testing whether the function is present
	# and then, if we're compiling with warnings as errors, testing
	# whether it is usable.  It is usable if it compiles without
	# a -Wimplicit warning (the "compile with warnings as errors"
	# option requires GCC). If it is not usable, we fail and tell
	# the user that the pcap.h header needs to be updated.
	#
	# Ceteris paribus, this should only happen with Mac OS X 10.3[.x] which
	# can have an up-to-date pcap library without the corresponding pcap
	# header.
	#
	# However, it might also happen on some others OSes with some erroneous
	# system manipulations where multiple versions of libcap might co-exist
	# e.g. hand made symbolic link from libpcap.so -> libpcap.so.0.8 but
	# having the pcap header version 0.7.
	#
	AC_MSG_CHECKING([whether pcap_breakloop is present])
	ac_CFLAGS_saved="$CFLAGS"
	AC_WIRESHARK_PCAP_BREAKLOOP_TRY_LINK
	if test "x$ws_breakloop_compiled" = "xyes"; then
	  AC_MSG_RESULT(yes)
	  AC_DEFINE(HAVE_PCAP_BREAKLOOP, 1, [Define if pcap_breakloop is known])
	  if test "x$with_warnings_as_errors" = "xyes"; then
	    AC_MSG_CHECKING([whether pcap_breakloop is usable])
	    CFLAGS="$CFLAGS -Werror -Wimplicit"
	    AC_WIRESHARK_PCAP_BREAKLOOP_TRY_LINK
	    if test "x$ws_breakloop_compiled" = "xyes"; then
	      AC_MSG_RESULT(yes)
	    else
	      AC_MSG_RESULT(no)
	      AC_MSG_ERROR(
[Your pcap library is more recent than your pcap header.
As you are building with compiler warnings treated as errors, Wireshark
won't be able to use functions not declared in that header.
If you wish to build with compiler warnings treated as errors, You should
install a newer version of the header file.])
	    fi
	    CFLAGS="$ac_CFLAGS_saved"
	  fi
	else
	  AC_MSG_RESULT(no)
	fi

	#
	# Later versions of Mac OS X 10.3[.x] ship a pcap.h that
	# doesn't define pcap_if_t but ship an 0.8[.x] libpcap,
	# so the library has "pcap_findalldevs()", but pcap.h
	# doesn't define "pcap_if_t" so you can't actually *use*
	# "pcap_findalldevs()".
	#
	# That even appears to be true of systems shipped with
	# 10.3.4, so it doesn't appear only to be a case of
	# Software Update not updating header files.
	#
	# (You can work around this by installing the 0.8 header
	# files.)
	#
	AC_CACHE_CHECK([whether pcap_findalldevs is present and usable],
	  [ac_cv_func_pcap_findalldevs],
	  [
	    AC_LINK_IFELSE(
	      [
		AC_LANG_SOURCE(
		  [[
		    #include <pcap.h>
		    main()
		    {
		      pcap_if_t *devpointer;
		      char errbuf[1];

		      pcap_findalldevs(&devpointer, errbuf);
		    }
		  ]])
	      ],
	      [
		ac_cv_func_pcap_findalldevs=yes
	      ],
	      [
		ac_cv_func_pcap_findalldevs=no
	      ])
	  ])
	#
	# Don't check for other new routines that showed up after
	# "pcap_findalldevs()" if we don't have a usable
	# "pcap_findalldevs()", so we don't end up using them if the
	# "pcap.h" is crufty and old and doesn't declare them.
	#
	if test $ac_cv_func_pcap_findalldevs = "yes" ; then
	  AC_DEFINE(HAVE_PCAP_FINDALLDEVS, 1,
	   [Define to 1 if you have the `pcap_findalldevs' function and a pcap.h that declares pcap_if_t.])
	  AC_CHECK_FUNCS(pcap_datalink_val_to_name pcap_datalink_name_to_val)
	  AC_CHECK_FUNCS(pcap_datalink_val_to_description)
	  AC_CHECK_FUNCS(pcap_list_datalinks pcap_set_datalink pcap_lib_version)
	  AC_CHECK_FUNCS(pcap_get_selectable_fd pcap_free_datalinks)
	  AC_CHECK_FUNCS(pcap_create bpf_image)
	fi
	LIBS="$ac_save_LIBS"
])

AC_DEFUN([AC_WIRESHARK_PCAP_REMOTE_CHECK],
[
    ac_save_LIBS="$LIBS"
    LIBS="$PCAP_LIBS $SOCKET_LIBS $NSL_LIBS $LIBS"
    AC_DEFINE(HAVE_REMOTE, 1, [Define to 1 to enable remote
              capturing feature in WinPcap library])
    AC_CHECK_FUNCS(pcap_open pcap_findalldevs_ex pcap_createsrcstr)
    if test $ac_cv_func_pcap_open = "yes" -a \
            $ac_cv_func_pcap_findalldevs_ex = "yes" -a \
            $ac_cv_func_pcap_createsrcstr = "yes" ; then
        AC_DEFINE(HAVE_PCAP_REMOTE, 1,
            [Define to 1 if you have WinPcap remote capturing support and prefer to use these new API features.])
    fi
    AC_CHECK_FUNCS(pcap_setsampling)
    LIBS="$ac_save_LIBS"
])

#
# AC_WIRESHARK_ZLIB_CHECK
#
AC_DEFUN([AC_WIRESHARK_ZLIB_CHECK],
[
	if test "x$zlib_dir" != "x"
	then
	  #
	  # The user specified a directory in which zlib resides,
	  # so add the "include" subdirectory of that directory to
	  # the include file search path and the "lib" subdirectory
	  # of that directory to the library search path.
	  #
	  # XXX - if there's also a zlib in a directory that's
	  # already in CFLAGS, CPPFLAGS, or LDFLAGS, this won't
	  # make us find the version in the specified directory,
	  # as the compiler and/or linker will search that other
	  # directory before it searches the specified directory.
	  #
	  wireshark_save_CFLAGS="$CFLAGS"
	  CFLAGS="$CFLAGS -I$zlib_dir/include"
	  wireshark_save_CPPFLAGS="$CPPFLAGS"
	  CPPFLAGS="$CPPFLAGS -I$zlib_dir/include"
	  wireshark_save_LIBS="$LIBS"
	  AC_WIRESHARK_ADD_DASH_L(LIBS, $zlib_dir/lib)
	fi

	#
	# Make sure we have "zlib.h".  If we don't, it means we probably
	# don't have zlib, so don't use it.
	#
	AC_CHECK_HEADER(zlib.h,,
	  [
	    if test "x$zlib_dir" != "x"
	    then
	      #
	      # The user used "--with-zlib=" to specify a directory
	      # containing zlib, but we didn't find the header file
	      # there; that either means they didn't specify the
	      # right directory or are confused about whether zlib
	      # is, in fact, installed.  Report the error and give up.
	      #
	      AC_MSG_ERROR([zlib header not found in directory specified in --with-zlib])
	    else
	      if test "x$want_zlib" = "xyes"
	      then
		#
		# The user tried to force us to use the library, but we
		# couldn't find the header file; report an error.
		#
		AC_MSG_ERROR(Header file zlib.h not found.)
	      else
		#
		# We couldn't find the header file; don't use the
		# library, as it's probably not present.
		#
		want_zlib=no
	      fi
	    fi
	  ])

	if test "x$want_zlib" != "xno"
	then
		#
		# Well, we at least have the zlib header file.
		#
		# Check for "gzgets()" in zlib, because we need it, but
		# some older versions of zlib don't have it.  It appears
		# from the zlib ChangeLog that any released version of zlib
		# with "gzgets()" should have the other routines we
		# depend on, such as "gzseek()", "gztell()", and "zError()".
		#
		# Another reason why we require "gzgets()" is that
		# some versions of zlib that didn't have it, such
		# as 1.0.8, had a bug in "gzseek()" that meant that it
		# doesn't work correctly on uncompressed files; this
		# means we cannot use version 1.0.8.  (Unfortunately,
		# that's the version that comes with recent X11 source,
		# and many people who install XFree86 on their Slackware
		# boxes don't realize that they should configure it to
		# use the native zlib rather than building and installing
		# the crappy old version that comes with XFree86.)
		#
		# I.e., we can't just avoid using "gzgets()", as
		# versions of zlib without "gzgets()" are likely to have
		# a broken "gzseek()".
		#
		AC_CHECK_LIB(z, gzgets,
		[
			if test "x$zlib_dir" != "x"
			then
				#
				# Put the "-I" and "-L" flags for zlib at
				# the beginning of CFLAGS, CPPFLAGS, and
				# LIBS.
				#
				LIBS=""
				AC_WIRESHARK_ADD_DASH_L(LIBS, $zlib_dir/lib)
				LIBS="$LIBS -lz $wireshark_save_LIBS"
			else
				LIBS="-lz $LIBS"
			fi
			AC_DEFINE(HAVE_LIBZ, 1, [Define to use libz library])
		],[
			if test "x$zlib_dir" != "x"
			then
				#
				# Restore the versions of CFLAGS, CPPFLAGS,
				# and LIBS before we added the "-with-zlib="
				# directory, as we didn't actually find
				# zlib there, or didn't find a zlib that
				# contains gzgets there.
				#
				CFLAGS="$wireshark_save_CFLAGS"
				CPPFLAGS="$wireshark_save_CPPFLAGS"
				LIBS="$wireshark_save_LIBS"
			fi
			want_zlib=no
		])
	fi

	if test "x$want_zlib" != "xno"
	then
		#
		# Well, we at least have the zlib header file and a zlib
		# with "gzgets()".
		#
		# Now check for "gzgets()" in zlib when linking with the
		# linker flags for GTK+ applications; people often grab
		# XFree86 source and build and install it on their systems,
		# and they appear sometimes to misconfigure XFree86 so that,
		# even on systems with zlib, it assumes there is no zlib,
		# so the XFree86 build process builds and installs its
		# own zlib in the X11 library directory.
		#
		# The XFree86 zlib is an older version that lacks
		# "gzgets()", and that's the zlib with which Wireshark
		# gets linked, so the build of Wireshark fails.
		#
		ac_save_CFLAGS="$CFLAGS"
		ac_save_LIBS="$LIBS"
		CFLAGS="$CFLAGS $GTK_CFLAGS"
		LIBS="$GTK_LIBS -lz $LIBS"
		AC_MSG_CHECKING([for gzgets missing when linking with X11])
		AC_TRY_LINK_FUNC(gzgets, AC_MSG_RESULT(no),
		  [
		    AC_MSG_RESULT(yes)
		    AC_MSG_ERROR(old zlib found when linking with X11 - get rid of old zlib.)
		  ])
		CFLAGS="$ac_save_CFLAGS"
		LIBS="$ac_save_LIBS"
	fi
])

#
# AC_WIRESHARK_LIBPCRE_CHECK
#
AC_DEFUN([AC_WIRESHARK_LIBPCRE_CHECK],
[
	if test "x$pcre_dir" != "x"
	then
	  #
	  # The user specified a directory in which libpcre resides,
	  # so add the "include" subdirectory of that directory to
	  # the include file search path and the "lib" subdirectory
	  # of that directory to the library search path.
	  #
	  # XXX - if there's also a libpcre in a directory that's
	  # already in CFLAGS, CPPFLAGS, or LDFLAGS, this won't
	  # make us find the version in the specified directory,
	  # as the compiler and/or linker will search that other
	  # directory before it searches the specified directory.
	  #
	  wireshark_save_CFLAGS="$CFLAGS"
	  CFLAGS="$CFLAGS -I$pcre_dir/include"
	  wireshark_save_CPPFLAGS="$CPPFLAGS"
	  CPPFLAGS="$CPPFLAGS -I$pcre_dir/include"
	  wireshark_save_LIBS="$LIBS"
	  LIBS="$LIBS -lpcre"
	  wireshark_save_LDFLAGS="$LDFLAGS"
	  LDFLAGS="$LDFLAGS -L$pcre_dir/lib"
	fi

	#
	# Make sure we have "pcre.h".  If we don't, it means we probably
	# don't have libpcre, so don't use it.
	#
	AC_CHECK_HEADER(pcre.h,,
	  [
	    if test "x$pcre_dir" != "x"
	    then
	      #
	      # The user used "--with-pcre=" to specify a directory
	      # containing libpcre, but we didn't find the header file
	      # there; that either means they didn't specify the
	      # right directory or are confused about whether libpcre
	      # is, in fact, installed.  Report the error and give up.
	      #
	      AC_MSG_ERROR([libpcre header not found in directory specified in --with-pcre])
	    else
	      if test "x$want_pcre" = "xyes"
	      then
		#
		# The user tried to force us to use the library, but we
		# couldn't find the header file; report an error.
		#
		AC_MSG_ERROR(Header file pcre.h not found.)
	      else
		#
		# We couldn't find the header file; don't use the
		# library, as it's probably not present.
		#
		want_pcre=no
	      fi
	    fi
	  ])

	if test "x$want_pcre" != "xno"
	then
		#
		# Well, we at least have the pcre header file.
		#
		# We're only using standard functions from libpcre,
		# so we don't need to perform extra checks.
		#
		AC_CHECK_LIB(pcre, pcre_compile,
		[
			if test "x$pcre_dir" != "x"
			then
				#
				# Put the "-I" and "-L" flags for pcre at
				# the beginning of CFLAGS, CPPFLAGS,
				# LDFLAGS, and LIBS.
				#
				PCRE_LIBS="-L$pcre_dir/lib -lpcre $wireshark_save_LIBS"
			else
				PCRE_LIBS="-lpcre"
			fi
			AC_DEFINE(HAVE_LIBPCRE, 1, [Define to use libpcre library])
		],[
			if test "x$pcre_dir" != "x"
			then
				#
				# Restore the versions of CFLAGS, CPPFLAGS,
				# LDFLAGS, and LIBS before we added the
				# "--with-pcre=" directory, as we didn't
				# actually find pcre there.
				#
				CFLAGS="$wireshark_save_CFLAGS"
				CPPFLAGS="$wireshark_save_CPPFLAGS"
				LDFLAGS="$wireshark_save_LDFLAGS"
				LIBS="$wireshark_save_LIBS"
				PCRE_LIBS=""
			fi
			want_pcre=no
		])
		AC_SUBST(PCRE_LIBS)
	fi
])

#
# AC_WIRESHARK_LIBLUA_CHECK
#
AC_DEFUN([AC_WIRESHARK_LIBLUA_CHECK],[

	if test "x$lua_dir" != "x"
	then
		#
		# The user specified a directory in which liblua resides,
		# so add the "include" subdirectory of that directory to
		# the include file search path and the "lib" subdirectory
		# of that directory to the library search path.
		#
		# XXX - if there's also a liblua in a directory that's
		# already in CFLAGS, CPPFLAGS, or LDFLAGS, this won't
		# make us find the version in the specified directory,
		# as the compiler and/or linker will search that other
		# directory before it searches the specified directory.
		#
		wireshark_save_CFLAGS="$CFLAGS"
		CFLAGS="$CFLAGS -I$lua_dir/include"
		wireshark_save_CPPFLAGS="$CPPFLAGS"
		CPPFLAGS="$CPPFLAGS -I$lua_dir/include"
		wireshark_save_LIBS="$LIBS"
		LIBS="$LIBS -L$lua_dir/lib -llua -lm"
		wireshark_save_LDFLAGS="$LDFLAGS"
		LDFLAGS="$LDFLAGS -L$lua_dir/lib"
	else
		#
		# The user specified no directory in which liblua resides,
		# so just add "-llua -lliblua" to the used libs.
		#
		wireshark_save_CFLAGS="$CFLAGS"
		wireshark_save_CPPFLAGS="$CPPFLAGS"
		wireshark_save_LDFLAGS="$LDFLAGS"
		wireshark_save_LIBS="$LIBS"
		LIBS="$LIBS -llua -lm"
	fi

	#
	# Make sure we have "lua.h", "lualib.h" and "lauxlib.h".  If we don't, it means we probably
	# don't have liblua, so don't use it.
	#
	AC_CHECK_HEADERS(lua.h lualib.h lauxlib.h,,
	[
		AC_CHECK_HEADERS(lua5.1/lua.h lua5.1/lualib.h lua5.1/lauxlib.h,
		[
			if test "x$lua_dir" != "x"
			then
				LUA_INCLUDES="-I$lua_dir/include/lua5.1"
			else
				#
				# The user didn't specify a directory in which liblua resides;
				# we must look for the headers in a "lua5.1" subdirectory of
				# "/usr/include", "/usr/local/include", or "$prefix/include"
				# as some systems apparently put the headers in a "lua5.1"
				# subdirectory.
				AC_MSG_CHECKING(for extraneous lua header directories)
				found_lua_dir=""
				lua_dir_list="/usr/include/lua5.1 $prefix/include/lua5.1"
				if test "x$ac_cv_enable_usr_local" = "xyes"
				then
					lua_dir_list="$lua_dir_list /usr/local/include/lua5.1"
				fi
				for lua_dir_ent in $lua_dir_list
				do
					if test -d $lua_dir_ent
					then
						LUA_INCLUDES="-I$lua_dir_ent"
						found_lua_dir="$lua_dir_ent"
						break
					fi
				done

				if test "x$found_lua_dir" != "x"
				then
					AC_MSG_RESULT(found -- $found_lua_dir)
				else
					AC_MSG_RESULT(not found)
					#
					# Restore the versions of CFLAGS, CPPFLAGS,
					# LDFLAGS, and LIBS before we added the
					# "--with-lua=" directory, as we didn't
					# actually find lua there.
					#
					CFLAGS="$wireshark_save_CFLAGS"
					CPPFLAGS="$wireshark_save_CPPFLAGS"
					LDFLAGS="$wireshark_save_LDFLAGS"
					LIBS="$wireshark_save_LIBS"
					LUA_LIBS=""
					if test "x$want_lua" = "xyes"
					then
						# we found lua5.1/lua.h, but we don't know which include dir contains it
						AC_MSG_ERROR(Header file lua.h was found as lua5.1/lua.h but we can't locate the include directory. Please set the DIR for the --with-lua configure parameter.)
					else
						#
						# We couldn't find the header file; don't use the
						# library, as it's probably not present.
						#
						want_lua=no
					fi
	  			fi
			fi
		],
		[
			#
			# Restore the versions of CFLAGS, CPPFLAGS,
			# LDFLAGS, and LIBS before we added the
			# "--with-lua=" directory, as we didn't
			# actually find lua there.
			#
			CFLAGS="$wireshark_save_CFLAGS"
			CPPFLAGS="$wireshark_save_CPPFLAGS"
			LDFLAGS="$wireshark_save_LDFLAGS"
			LIBS="$wireshark_save_LIBS"
			LUA_LIBS=""
			if test "x$lua_dir" != "x"
			then
				#
				# The user used "--with-lua=" to specify a directory
				# containing liblua, but we didn't find the header file
				# there; that either means they didn't specify the
				# right directory or are confused about whether liblua
				# is, in fact, installed.  Report the error and give up.
				#
				AC_MSG_ERROR([liblua header not found in directory specified in --with-lua])
			else
				if test "x$want_lua" = "xyes"
				then
					#
					# The user tried to force us to use the library, but we
					# couldn't find the header file; report an error.
					#
					AC_MSG_ERROR(Header file lua.h not found.)
				else
					#
					# We couldn't find the header file; don't use the
					# library, as it's probably not present.
					#
					want_lua=no
				fi
			fi
		])
	])

	if test "x$want_lua" != "xno"
	then
		#
		# Well, we at least have the lua header file.
		#
		# let's check if the libs are there
		#

		# At least on Suse 9.3 systems, liblualib needs linking
		# against libm.
		LIBS="$LIBS $LUA_LIBS -lm"

		AC_CHECK_LIB(lua, luaL_register,
		[
		    #
		    #  Lua 5.1 found
		    #
			if test "x$lua_dir" != "x"
			then
				#
				# Put the "-I" and "-L" flags for lua at
				# the beginning of CFLAGS, CPPFLAGS,
				# LDFLAGS, and LIBS.
				#
				LUA_LIBS="-L$lua_dir/lib -llua -lm"
				LUA_INCLUDES="-I$lua_dir/include"
			else
				LUA_LIBS="-llua -lm"
				LUA_INCLUDES=""
			fi
			AC_DEFINE(HAVE_LUA_5_1, 1, [Define to use Lua 5.1])
			want_lua=yes

		],[
			#
			# We could not find the libs, maybe we have version number in the lib name
			#

			LIBS="$wireshark_save_LIBS -llua5.1 -lm"

			AC_CHECK_LIB(lua5.1, luaL_register,
			[
			    #
			    #  Lua 5.1 found
			    #
			    LUA_LIBS=" -llua5.1 -lm"
			    AC_DEFINE(HAVE_LUA_5_1, 1, [Define to use Lua 5.1])
			    want_lua=yes
			],[
				#
				# Restore the versions of CFLAGS, CPPFLAGS,
				# LDFLAGS, and LIBS before we added the
				# "--with-lua=" directory, as we didn't
				# actually find lua there.
				#
				CFLAGS="$wireshark_save_CFLAGS"
				CPPFLAGS="$wireshark_save_CPPFLAGS"
				LDFLAGS="$wireshark_save_LDFLAGS"
				LIBS="$wireshark_save_LIBS"
				LUA_LIBS=""
				# User requested --with-lua but it isn't available
				if test "x$want_lua" = "xyes"
				then
					AC_MSG_ERROR(Linking with liblua failed.)
				fi
				want_lua=no
			])
		])

	CFLAGS="$wireshark_save_CFLAGS"
	CPPFLAGS="$wireshark_save_CPPFLAGS"
	LDFLAGS="$wireshark_save_LDFLAGS"
	LIBS="$wireshark_save_LIBS"
	AC_SUBST(LUA_LIBS)
	AC_SUBST(LUA_INCLUDES)

	fi
])

#
# AC_WIRESHARK_LIBPORTAUDIO_CHECK
#
AC_DEFUN([AC_WIRESHARK_LIBPORTAUDIO_CHECK],[

	if test "x$portaudio_dir" != "x"
	then
		#
		# The user specified a directory in which libportaudio
		# resides, so add the "include" subdirectory of that directory to
		# the include file search path and the "lib" subdirectory
		# of that directory to the library search path.
		#
		# XXX - if there's also a libportaudio in a directory that's
		# already in CFLAGS, CPPFLAGS, or LDFLAGS, this won't
		# make us find the version in the specified directory,
		# as the compiler and/or linker will search that other
		# directory before it searches the specified directory.
		#
		wireshark_save_CFLAGS="$CFLAGS"
		CFLAGS="$CFLAGS -I$portaudio_dir/include"
		wireshark_save_CPPFLAGS="$CPPFLAGS"
		CPPFLAGS="$CPPFLAGS -I$portaudio_dir/include"
		wireshark_save_LIBS="$LIBS"
		LIBS="$LIBS -L$portaudio_dir/lib -lportaudio"
		wireshark_save_LDFLAGS="$LDFLAGS"
		LDFLAGS="$LDFLAGS -L$portaudio_dir/lib"
	else
		#
		# The user specified no directory in which libportaudio resides,
		# so just add "-lportaudio" to the used libs.
		#
		wireshark_save_CFLAGS="$CFLAGS"
		wireshark_save_CPPFLAGS="$CPPFLAGS"
		wireshark_save_LDFLAGS="$LDFLAGS"
		wireshark_save_LIBS="$LIBS"
		LIBS="$LIBS -lportaudio"
	fi

	#
	# Make sure we have "portaudio.h".  If we don't, it means we probably
	# don't have libportaudio, so don't use it.
	#
	AC_CHECK_HEADERS(portaudio.h,,
	[
		if test "x$portaudio_dir" != "x"
		then
			#
			# The user used "--with-portaudio=" to specify a directory
			# containing libportaudio, but we didn't find the header file
			# there; that either means they didn't specify the
			# right directory or are confused about whether libportaudio
			# is, in fact, installed.  Report the error and give up.
			#
			AC_MSG_ERROR([libportaudio header not found in directory specified in --with-portaudio])
		else
			CFLAGS="$wireshark_save_CFLAGS"
			CPPFLAGS="$wireshark_save_CPPFLAGS"
			LDFLAGS="$wireshark_save_LDFLAGS"
			LIBS="$wireshark_save_LIBS"
			PORTAUDIO_LIBS=""
			if test "x$want_portaudio" = "xyes"
			then
				#
				# The user tried to force us to use the library, but we
				# couldn't find the header file; report an error.
				#
				AC_MSG_ERROR(Header file portaudio.h not found.)
			else
				#
				# We couldn't find the header file; don't use the
				# library, as it's probably not present.
				#
				want_portaudio=no
			fi
		fi
	])

	#
	# Check whether we have the right version of portaudio
	#
	if test "x$want_portaudio" != "xno"
	then
		AC_CHECK_TYPE(PortAudioStream,
		AC_DEFINE(PORTAUDIO_API_1, 1, [Define if we are using version of of the Portaudio library API]),
		,
		[#include <portaudio.h>])
	fi

	if test "x$want_portaudio" != "xno"
	then
		#
		# Well, we at least have the portaudio header file.
		#
		# let's check if the libs are there
		#

		AC_CHECK_LIB(portaudio, Pa_Initialize,
		[
			if test "x$portaudio_dir" != "x"
			then
				#
				# Put the "-I" and "-L" flags for portaudio at
				# the beginning of CFLAGS, CPPFLAGS,
				# LDFLAGS, and LIBS.
				#
				PORTAUDIO_LIBS="-L$portaudio_dir/lib -lportaudio"
				PORTAUDIO_INCLUDES="-I$portaudio_dir/include"
			else
				PORTAUDIO_LIBS="-lportaudio"
				PORTAUDIO_INCLUDES=""
			fi
			AC_DEFINE(HAVE_LIBPORTAUDIO, 1, [Define to use libportaudio library])
			want_portaudio=yes
		],[
			#
			# Restore the versions of CFLAGS, CPPFLAGS,
			# LDFLAGS, and LIBS before we added the
			# "--with-portaudio=" directory, as we didn't
			# actually find portaudio there.
			#
			CFLAGS="$wireshark_save_CFLAGS"
			CPPFLAGS="$wireshark_save_CPPFLAGS"
			LDFLAGS="$wireshark_save_LDFLAGS"
			LIBS="$wireshark_save_LIBS"
			PORTAUDIO_LIBS=""
			# User requested --with-portaudio but it isn't available
			if test "x$want_portaudio" = "xyes"
			then
				AC_MSG_ERROR(Linking with libportaudio failed.)
			fi
			want_portaudio=no
		])

	CFLAGS="$wireshark_save_CFLAGS"
	CPPFLAGS="$wireshark_save_CPPFLAGS"
	LDFLAGS="$wireshark_save_LDFLAGS"
	LIBS="$wireshark_save_LIBS"
	AC_SUBST(PORTAUDIO_LIBS)
	AC_SUBST(PORTAUDIO_INCLUDES)

	fi
])

#
# AC_WIRESHARK_RPM_CHECK
# Looks for the rpm program, and checks to see if we can redefine "_topdir".
#
AC_DEFUN([AC_WIRESHARK_RPM_CHECK],
[
	AC_CHECK_PROG(ac_cv_wireshark_have_rpm, rpm, "yes", "no")
	if test "x$ac_cv_wireshark_have_rpm" = "xyes"; then
		rpm --define '_topdir /tmp' > /dev/null 2>&1
		AC_MSG_CHECKING(to see if we can redefine _topdir)
		if test $? -eq 0 ; then
			AC_MSG_RESULT(yes)
			HAVE_RPM=yes
		else
			AC_MSG_RESULT(no.  You'll have to build packages manually.)
			HAVE_RPM=no
		fi
	fi
])

#
# AC_WIRESHARK_GNU_SED_CHECK
# Checks if GNU sed is the first sed in PATH.
#
AC_DEFUN([AC_WIRESHARK_GNU_SED_CHECK],
[
	AC_MSG_CHECKING(for GNU sed as first sed in PATH)
	if  ( sh -c "sed --version" </dev/null 2> /dev/null | grep "GNU sed" 2>&1 > /dev/null ) ;  then
		AC_MSG_RESULT(yes)
		HAVE_GNU_SED=yes
	else
		AC_MSG_RESULT(no)
		HAVE_GNU_SED=no
	fi
])

#
# AC_WIRESHARK_C_ARES_CHECK
#
AC_DEFUN([AC_WIRESHARK_C_ARES_CHECK],
[
	want_c_ares=defaultyes

	if test "x$want_c_ares" = "xdefaultyes"; then
		want_c_ares=yes
		if test "x$ac_cv_enable_usr_local" = "xyes" ; then
			withval=/usr/local
			if test -d "$withval"; then
				AC_WIRESHARK_ADD_DASH_L(LDFLAGS, ${withval}/lib)
			fi
		fi
	fi

	if test "x$want_c_ares" = "xyes"; then
		AC_CHECK_LIB(cares, ares_init,
		  [
		    C_ARES_LIBS=-lcares
	    	AC_DEFINE(HAVE_C_ARES, 1, [Define to use c-ares library])
		have_good_c_ares=yes
		  ],, $SOCKET_LIBS $NSL_LIBS
		)
	else
		AC_MSG_RESULT(not required)
	fi
])


#
# AC_WIRESHARK_ADNS_CHECK
#
AC_DEFUN([AC_WIRESHARK_ADNS_CHECK],
[
	want_adns=defaultyes

	if test "x$want_adns" = "xdefaultyes"; then
		want_adns=yes
		if test "x$ac_cv_enable_usr_local" = "xyes" ; then
			withval=/usr/local
			if test -d "$withval"; then
				AC_WIRESHARK_ADD_DASH_L(LDFLAGS, ${withval}/lib)
			fi
		fi
	fi

	if test "x$want_adns" = "xyes"; then
		AC_CHECK_LIB(adns, adns_init,
		  [
		    ADNS_LIBS=-ladns
	    	AC_DEFINE(HAVE_GNU_ADNS, 1, [Define to use GNU ADNS library])
		have_good_adns=yes
		  ],, $SOCKET_LIBS $NSL_LIBS
		)
	else
		AC_MSG_RESULT(not required)
	fi
])


#
# AC_WIRESHARK_LIBCAP_CHECK
#
AC_DEFUN([AC_WIRESHARK_LIBCAP_CHECK],
[
	want_libcap=defaultyes

	if test "x$want_libcap" = "xdefaultyes"; then
		want_libcap=yes
		if test "x$ac_cv_enable_usr_local" = "xyes" ; then
			withval=/usr/local
			if test -d "$withval"; then
				AC_WIRESHARK_ADD_DASH_L(LDFLAGS, ${withval}/lib)
			fi
		fi
	fi

	if test "x$want_libcap" = "xyes"; then
		AC_CHECK_LIB(cap, cap_set_flag,
		  [
		    LIBCAP_LIBS=-lcap
	    	AC_DEFINE(HAVE_LIBCAP, 1, [Define to use the libcap library])
		have_good_libcap=yes
		  ],,
		)
	else
		AC_MSG_RESULT(not required)
	fi
])


#
# AC_WIRESHARK_KRB5_CHECK
#
AC_DEFUN([AC_WIRESHARK_KRB5_CHECK],
[
	wireshark_save_CFLAGS="$CFLAGS"
	wireshark_save_CPPFLAGS="$CPPFLAGS"
	if test "x$krb5_dir" != "x"
	then
	  #
	  # The user specified a directory in which kerberos resides,
	  # so add the "include" subdirectory of that directory to
	  # the include file search path and the "lib" subdirectory
	  # of that directory to the library search path.
	  #
	  # XXX - if there's also a kerberos in a directory that's
	  # already in CFLAGS, CPPFLAGS, or LDFLAGS, this won't
	  # make us find the version in the specified directory,
	  # as the compiler and/or linker will search that other
	  # directory before it searches the specified directory.
	  #
	  CFLAGS="$CFLAGS -I$krb5_dir/include"
	  CPPFLAGS="$CPPFLAGS -I$krb5_dir/include"
	  ac_heimdal_version=`grep heimdal $krb5_dir/include/krb5.h | head -n 1 | sed 's/^.*heimdal.*$/HEIMDAL/'`
	  # MIT Kerberos moved krb5.h to krb5/krb5.h starting with release 1.5
	  ac_mit_version_olddir=`grep 'Massachusetts Institute of Technology' $krb5_dir/include/krb5.h | head -n 1 | sed 's/^.*Massachusetts Institute of Technology.*$/MIT/'`
	  ac_mit_version_newdir=`grep 'Massachusetts Institute of Technology' $krb5_dir/include/krb5/krb5.h | head -n 1 | sed 's/^.*Massachusetts Institute of Technology.*$/MIT/'`
	  ac_krb5_version="$ac_heimdal_version$ac_mit_version_olddir$ac_mit_version_newdir"
	  if test "x$ac_krb5_version" = "xHEIMDAL"
	      KRB5_LIBS="-L$krb5_dir/lib -lkrb5 -lasn1 $SSL_LIBS -lroken -lcrypt"
	  then
	      KRB5_LIBS="-L$krb5_dir/lib -lkrb5 -lk5crypto -lcom_err"
	  fi
	  if test "x$ac_krb5_version" = "xMIT"
	  then
	    AC_DEFINE(HAVE_MIT_KERBEROS, 1, [Define to use MIT kerberos])
	  fi
	else
	  AC_PATH_PROG(KRB5_CONFIG, krb5-config)
	  if test -x "$KRB5_CONFIG"
	  then
	    KRB5_FLAGS=`"$KRB5_CONFIG" --cflags`
	    KRB5_LIBS=`"$KRB5_CONFIG" --libs`
	    CFLAGS="$CFLAGS $KRB5_FLAGS"
	    CPPFLAGS="$CPPFLAGS $KRB5_FLAGS"
	    #
	    # If -lcrypto is in KRB5_FLAGS, we require it to build
	    # with Heimdal/MIT.  We don't want to built with it by
	    # default, due to annoying license incompatibilities
	    # between the OpenSSL license and the GPL, so:
	    #
	    #	if SSL_LIBS is set to a non-empty string, we
	    #	remove -lcrypto from KRB5_LIBS and replace
	    #	it with SSL_LIBS;
	    #
	    #	if SSL_LIBS is not set to a non-empty string
	    #	we fail with an appropriate error message.
	    #
	    case "$KRB5_LIBS" in
	    *-lcrypto*)
		if test ! -z "$SSL_LIBS"
		then
		    KRB5_LIBS=`echo $KRB5_LIBS | sed 's/-lcrypto//'`
		    KRB5_LIBS="$KRB5_LIBS $SSL_LIBS"
		else
		    AC_MSG_ERROR([Kerberos library requires -lcrypto but --with-ssl not specified])
		fi
		;;
	    esac
	    ac_krb5_version=`"$KRB5_CONFIG" --version | head -n 1 | sed -e 's/^.*heimdal.*$/HEIMDAL/' -e 's/^Kerberos .*$/MIT/' -e 's/^Solaris Kerberos .*$/MIT/'`
 	  fi
	fi

	#
	# Make sure we have "krb5.h".  If we don't, it means we probably
	# don't have kerberos, so don't use it.
	#
	AC_CHECK_HEADER(krb5.h,,
	  [
	    if test "x$krb5_dir" != "x"
	    then
	      #
	      # The user used "--with-krb5=" to specify a directory
	      # containing kerberos, but we didn't find the header file
	      # there; that either means they didn't specify the
	      # right directory or are confused about whether kerberos
	      # is, in fact, installed.  Report the error and give up.
	      #
	      AC_MSG_ERROR([kerberos header not found in directory specified in --with-krb5])
	    else
	      if test "x$want_krb5" = "xyes"
	      then
		#
		# The user tried to force us to use the library, but we
		# couldn't find the header file; report an error.
		#
		AC_MSG_ERROR(Header file krb5.h not found.)
	      else
		#
		# We couldn't find the header file; don't use the
		# library, as it's probably not present.
		#
		want_krb5=no
		AC_MSG_RESULT(No Heimdal or MIT header found - disabling dissection for some kerberos data in packet decoding)
	      fi
	    fi
	  ])

	if test "x$want_krb5" != "xno"
	then
	    #
	    # Well, we at least have the krb5 header file.
	    # Check whether this is Heimdal or MIT.
	    #
	    AC_MSG_CHECKING(whether the Kerberos library is Heimdal or MIT)
	    if test "x$ac_krb5_version" = "xHEIMDAL" -o "x$ac_krb5_version" = "xMIT"
	    then
		#
		# Yes.
		# Check whether we have krb5_kt_resolve - and whether
		# we need to link with -lresolv when linking with
		# the Kerberos library.
		#
		AC_MSG_RESULT($ac_krb5_version)
		wireshark_save_LIBS="$LIBS"
		found_krb5_kt_resolve=no
		for extras in "" "-lresolv"
		do
		    LIBS="$KRB5_LIBS $extras"
		    if test -z "$extras"
		    then
			AC_MSG_CHECKING([whether $ac_krb5_version includes krb5_kt_resolve])
		    else
			AC_MSG_CHECKING([whether $ac_krb5_version includes krb5_kt_resolve (linking with $extras)])
		    fi
		    AC_TRY_LINK(
			[
			],
			[
			    krb5_kt_resolve();
			],
			[
			    #
			    # We found "krb5_kt_resolve()", and required
			    # the libraries in extras as well.
			    #
			    AC_MSG_RESULT(yes)
			    KRB5_LIBS="$LIBS"
			    AC_DEFINE(HAVE_KERBEROS, 1, [Define to use kerberos])
	    		    if test "x$ac_krb5_version" = "xHEIMDAL"
			    then
				AC_DEFINE(HAVE_HEIMDAL_KERBEROS, 1, [Define to use heimdal kerberos])
			    elif test "x$ac_krb5_version" = "xMIT"
			    then
				AC_DEFINE(HAVE_MIT_KERBEROS, 1, [Define to use MIT kerberos])
			    fi
			    found_krb5_kt_resolve=yes
			    break
			],
			[
			    AC_MSG_RESULT(no)
			])
		done
		if test "$found_krb5_kt_resolve" = no
		then
		    #
		    # We didn't find "krb5_kt_resolve()" in the
		    # Kerberos library, even when we tried linking
		    # with -lresolv; we can't link with kerberos.
		    #
		    if test "x$want_krb5" = "xyes"
		    then
			#
			# The user tried to force us to use the library,
			# but we can't do so; report an error.
			#
			AC_MSG_ERROR(Usable $ac_krb5_version not found)
		    else
			#
			# Restore the versions of CFLAGS and CPPFLAGS
			# from before we added the flags for Kerberos.
			#
			AC_MSG_RESULT(Usable $ac_krb5_version not found - disabling dissection for some kerberos data in packet decoding)
			CFLAGS="$wireshark_save_CFLAGS"
			CPPFLAGS="$wireshark_save_CPPFLAGS"
			KRB5_LIBS=""
			want_krb5=no
		    fi
		else
		    #
		    # We can link with Kerberos; see whether krb5.h
		    # defines KEYTYPE_ARCFOUR_56 (where "defines" means
		    # "as a #define or as an enum member).
		    #
		    AC_MSG_CHECKING([whether krb5.h defines KEYTYPE_ARCFOUR_56])
		    AC_COMPILE_IFELSE(
		      [
			AC_LANG_SOURCE(
			  [[
			    #include <krb5.h>
			    #include <stdio.h>

			    main()
			    {
			      printf("%u\n", KEYTYPE_ARCFOUR_56);
			    }
			  ]])
		      ],
		      [
			AC_MSG_RESULT(yes)
			AC_DEFINE(HAVE_KEYTYPE_ARCFOUR_56, 1, [Define if krb5.h defines KEYTYPE_ARCFOUR_56])
		      ],
		      [
			AC_MSG_RESULT(no)
		      ])
		fi
		LIBS="$wireshark_save_LIBS"
	    else
		#
		# It's not Heimdal or MIT.
		#
		AC_MSG_RESULT(no)
		if test "x$want_krb5" = "xyes"
		then
		    #
		    # The user tried to force us to use the library,
		    # but we can't do so; report an error.
		    #
		    AC_MSG_ERROR(Kerberos not found)
		else
		    #
		    # Restore the versions of CFLAGS and CPPFLAGS
		    # from before we added the flags for Kerberos.
		    #
		    AC_MSG_RESULT(Kerberos not found - disabling dissection for some kerberos data in packet decoding)
		    CFLAGS="$wireshark_save_CFLAGS"
		    CPPFLAGS="$wireshark_save_CPPFLAGS"
		    KRB5_LIBS=""
		    want_krb5=no
		fi
	    fi
	else
	    #
	    # The user asked us not to use Kerberos, or they didn't
	    # say whether they wanted us to use it but we found
	    # that we couldn't.
	    #
	    # Restore the versions of CFLAGS and CPPFLAGS
	    # from before we added the flags for Kerberos.
	    #
	    CFLAGS="$wireshark_save_CFLAGS"
	    CPPFLAGS="$wireshark_save_CPPFLAGS"
	    KRB5_LIBS=""
	    want_krb5=no
	fi
	AC_SUBST(KRB5_LIBS)
])

#
# AC_WIRESHARK_GEOIP_CHECK
#
AC_DEFUN([AC_WIRESHARK_GEOIP_CHECK],
[
	want_geoip=defaultyes

	if test "x$want_geoip" = "xdefaultyes"; then
		want_geoip=yes
		if test "x$ac_cv_enable_usr_local" = "xyes" ; then
			withval=/usr/local
			if test -d "$withval"; then
				AC_WIRESHARK_ADD_DASH_L(LDFLAGS, ${withval}/lib)
			fi
		fi
	fi

	if test "x$want_geoip" = "xyes"; then
		AC_CHECK_LIB(GeoIP, GeoIP_new,
		  [
		    GEOIP_LIBS=-lGeoIP
	    	AC_DEFINE(HAVE_GEOIP, 1, [Define to use GeoIP library])
		have_good_geoip=yes
		  ],,
		)
	else
		AC_MSG_RESULT(not required)
	fi
])

#AC_WIRESHARK_GCC_LDFLAGS_CHECK
#
# $1 : ldflag(s) to test
#
# The macro first determines if the compiler is GCC. Then compile with the
# defined ldflags. The defined flags are added to LDFLAGS only if the
# compilation succeeds.
#
AC_DEFUN([AC_WIRESHARK_GCC_LDFLAGS_CHECK],
[GCC_OPTION="$1"
AC_MSG_CHECKING(whether we can add $GCC_OPTION to LDFLAGS)
if test "x$GCC" != "x"; then
  LDFLAGS_saved="$CFLAGS"
  LDFLAGS="$LDFLAGS $GCC_OPTION"
  AC_LINK_IFELSE([
    AC_LANG_SOURCE([[
		main() { return; }
                  ]])],
                  [
                    AC_MSG_RESULT(yes)
                  ],
                  [
                    AC_MSG_RESULT(no)
                    LDFLAGS="$LDFLAGS_saved"
                  ])
else
  AC_MSG_RESULT(no)
fi
])

#
# AC_WIRESHARK_GCC_CFLAGS_CHECK
#
# $1 : cflags to test
#
# The macro first determines if the compiler is GCC. Then compile with the
# defined cflags. The defined flags are added to CFLAGS only if the compilation
# succeeds.
#
AC_DEFUN([AC_WIRESHARK_GCC_CFLAGS_CHECK],
[GCC_OPTION="$1"
AC_MSG_CHECKING(whether we can add $GCC_OPTION to CFLAGS)
if test "x$GCC" != "x"; then
  CFLAGS_saved="$CFLAGS"
  CFLAGS="$CFLAGS $GCC_OPTION"
  AC_COMPILE_IFELSE([
    AC_LANG_SOURCE([[
                      int foo;
                  ]])],
                  [
                    AC_MSG_RESULT(yes)
                  ],
                  [
                    AC_MSG_RESULT(no)
                    CFLAGS="$CFLAGS_saved"
                  ])
else
  AC_MSG_RESULT(no)
fi
])

#
# AC_WIRESHARK_OSX_INTEGRATION_CHECK
#
# Checks for the presence of OS X integration functions in the GTK+ framework
# or as a separate library.
#
# http://sourceforge.net/apps/trac/gtk-osx/wiki/Integrate
#
# http://live.gnome.org/GTK%2B/OSX/Integration
#    for the old Carbon-based integration functions
#
# http://gtk-osx.sourceforge.net/ige-mac-integration/
#    for the new Cocoa-based integration functions
#
AC_DEFUN([AC_WIRESHARK_OSX_INTEGRATION_CHECK],
[
	ac_save_CFLAGS="$CFLAGS"
	ac_save_LIBS="$LIBS"
	CFLAGS="$CFLAGS $GTK_CFLAGS"
	LIBS="$GTK_LIBS $LIBS"

	#
	# Check for the new integration functions in a -ligemacintegration
	# library.
	#
	AC_CHECK_LIB(igemacintegration, gtk_osxapplication_set_menu_bar,
	[
		AC_DEFINE(HAVE_GTKOSXAPPLICATION, 1,
			[Define to 1 if -ligemacintegration includes the GtkOSXApplication Integration functions.])
		have_ige_mac=yes
		# We don't want gtk stuff in LIBS (which is reset below) so
		# manually set GTK_LIBS (which is more appropriate)
		GTK_LIBS="$GTK_LIBS -ligemacintegration"
	])

	if test x$have_ige_mac == x
	then
		#
		# Not found - check for the old integration functions in
		# the Gtk framework.
		#
		AC_CHECK_LIB(Gtk, ige_mac_menu_set_menu_bar,
		[
			AC_DEFINE(HAVE_IGE_MAC_INTEGRATION, 1,
				[Define to 1 if the the Gtk+ framework or a separate library includes the Imendio IGE Mac OS X Integration functions.])
			have_ige_mac=yes
			# We don't want gtk stuff in LIBS (which is reset below) so
			# manually set GTK_LIBS (which is more appropriate)
			GTK_LIBS="$GTK_LIBS -lGtk"
		])
	fi

	if test x$have_ige_mac == x
	then
		#
		# Not found - check for the old integration functions in
		# a -ligemacintegration library.
		#
		AC_CHECK_LIB(igemacintegration, ige_mac_menu_set_menu_bar,
		[
			AC_DEFINE(HAVE_IGE_MAC_INTEGRATION, 1,
				[Define to 1 if the the Gtk+ framework or a separate library includes the Imendio IGE Mac OS X Integration functions.])
			have_ige_mac=yes
			# We don't want gtk stuff in LIBS (which is reset below) so
			# manually set GTK_LIBS (which is more appropriate)
			GTK_LIBS="$GTK_LIBS -ligemacintegration"
		])
	fi
	CFLAGS="$ac_save_CFLAGS"
	LIBS="$ac_save_LIBS"
])

#
# AC_WIRESHARK_PYTHON_CHECK
#
# Check whether python devel package is present
#
AC_DEFUN([AC_WIRESHARK_PYTHON_CHECK],
  [
    #
    # Checking whether we have a python devel environment available
    #
#  AC_CACHE_CHECK([checking python devel package], ac_cv_wireshark_python_devel,
#    [
    AC_CHECK_PROG([ac_ws_python_config], [python-config], "yes", "no")
    if test ac_ws_python_config = "no"; then
      ac_cv_wireshark_python_devel = "no"
    else
      AC_MSG_CHECKING([python devel])
      ac_save_ws_cflags=$CFLAGS
      ac_save_ws_libs=$LIBS
      CFLAGS=$(python-config --includes)
      LIBS=$(python-config --ldflags)
      AC_COMPILE_IFELSE(
        [
          AC_LANG_PROGRAM(
            [[#include <Python.h>]],
            [[Py_Initialiaze();]]
          )
        ],
        [
          #
          # Compilation successful, we have python devel available
          #
          ac_cv_wireshark_python_devel=yes
          PY_LIBS=$LIBS
          PY_CFLAGS=$CFLAGS
          AC_SUBST(PY_LIBS)
          AC_SUBST(PY_CFLAGS)
          CFLAGS="$ac_save_ws_cflags"
          LIBS="$ac_save_ws_libs"
          AC_DEFINE(HAVE_PYTHON, 1, [Define if python devel package available])
          AC_MSG_RESULT([yes])
        ],
        [
          #
          # Compilation unsuccessful, python devel not available
          #
          ac_cv_wireshark_python_devel=no
          CFLAGS=$ac_save_ws_cflags
          LIBS=$ac_save_ws_libs
          AC_MSG_RESULT([no])
        ])
    fi
#    ])
])
