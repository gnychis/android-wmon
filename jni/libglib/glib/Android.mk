LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    ./libcharset/localcharset.c \
    garray.c        \
    gasyncqueue.c   \
    gatomic.c       \
    gbacktrace.c    \
    gbase64.c       \
    gbookmarkfile.c \
    gcache.c        \
    gcompletion.c   \
    gconvert.c      \
    gdataset.c      \
    gdate.c         \
    gdir.c          \
    gerror.c        \
    gfileutils.c    \
    ghash.c         \
    ghook.c         \
    giochannel.c    \
    gkeyfile.c      \
    glist.c         \
    gmain.c         \
    gmappedfile.c   \
    gmarkup.c       \
    gmem.c          \
    gmessages.c     \
    gnode.c         \
    goption.c       \
    gpattern.c      \
    gprimes.c       \
    gqsort.c        \
    gqueue.c        \
    grel.c          \
    grand.c         \
    gregex.c        \
    gscanner.c      \
    gsequence.c     \
    gshell.c        \
    gslice.c        \
    gslist.c        \
    gstdio.c        \
    gstrfuncs.c     \
    gstring.c       \
    gthread.c       \
    gthreadpool.c   \
		gtestutils.c		\
    gtimer.c        \
    gtree.c         \
    guniprop.c      \
    gutf8.c         \
    gunibreak.c     \
    gunicollate.c   \
    gunidecomp.c    \
    gutils.c        \
    gprintf.c       \
    giounix.c       \
    gspawn.c

LOCAL_SRC_FILES +=	\
	pcre/pcre_chartables.c		\
	pcre/pcre_compile.c			\
	pcre/pcre_config.c			\
	pcre/pcre_dfa_exec.c		\
	pcre/pcre_exec.c			\
	pcre/pcre_fullinfo.c		\
	pcre/pcre_get.c				\
	pcre/pcre_globals.c			\
	pcre/pcre_info.c			\
	pcre/pcre_maketables.c		\
	pcre/pcre_newline.c			\
	pcre/pcre_ord2utf8.c		\
	pcre/pcre_refcount.c		\
	pcre/pcre_study.c			\
	pcre/pcre_tables.c			\
	pcre/pcre_try_flipped.c		\
	pcre/pcre_ucp_searchfuncs.c	\
	pcre/pcre_valid_utf8.c		\
	pcre/pcre_version.c			\
	pcre/pcre_xclass.c

LOCAL_MODULE:= libglib-2.0

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/..                \
    $(LOCAL_PATH)/../android        \
    $(LOCAL_PATH)/libcharset        \
    $(LOCAL_PATH)/gnulib            \
    $(LOCAL_PATH)/pcre

# ./glib private macros, copy from Makefile.am
LOCAL_CFLAGS := \
    -DLIBDIR=\"$(libdir)\"          \
    -DHAVE_CONFIG_H                 \
    \
    -DG_LOG_DOMAIN=\"GLib-GRegex\" \
    -DSUPPORT_UCP \
    -DSUPPORT_UTF8 \
    -DNEWLINE=-1 \
    -DMATCH_LIMIT=10000000 \
    -DMATCH_LIMIT_RECURSION=10000000 \
    -DMAX_NAME_SIZE=32 \
    -DMAX_NAME_COUNT=10000 \
    -DMAX_DUPLENGTH=30000 \
    -DLINK_SIZE=2 \
    -DEBCDIC=0 \
    -DPOSIX_MALLOC_THRESHOLD=10 \
    -DG_DISABLE_DEPRECATED \
    -DGLIB_COMPILATION 

#This options disable some debug functions and save about 40kb of code
LOCAL_CFLAGS += \
    -DDISABLE_VISIBILITY   \
    -DG_DISABLE_CHECKS     \
    -DG_DISABLE_ASSERT

LOCAL_COPY_HEADERS_TO := glib
LOCAL_COPY_HEADERS :=	\
		glib.h			\
		galloca.h		\
		garray.h		\
		gasyncqueue.h	\
		gatomic.h		\
		gbacktrace.h	\
		gbase64.h		\
		gbookmarkfile.h	\
		gcache.h		\
		gcompletion.h	\
		gconvert.h		\
		gdataset.h		\
		gdate.h			\
		gdir.h			\
		gerror.h		\
		gfileutils.h	\
		ghash.h			\
		ghook.h			\
		giochannel.h	\
		gkeyfile.h		\
		glist.h			\
		gmacros.h		\
		gmain.h			\
		gmappedfile.h	\
		gmarkup.h		\
		gmem.h			\
		gmessages.h		\
		gnode.h			\
		goption.h		\
		gpattern.h		\
		gprimes.h		\
		gqsort.h		\
		gquark.h		\
		gqueue.h		\
		grand.h			\
		grel.h			\
		gregex.h		\
		gscanner.h		\
		gsequence.h		\
		gshell.h		\
		gslist.h		\
		gspawn.h		\
		gstrfuncs.h		\
		gstring.h		\
		gthread.h		\
		gthreadpool.h	\
		gtestutils.h \
		gtimer.h		\
		gtree.h			\
		gtypes.h		\
		gunicode.h		\
		gutils.h		\
		gwin32.h		\
		gslice.h		\
		../android/glibconfig.h

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

include $(BUILD_SHARED_LIBRARY)
