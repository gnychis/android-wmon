# Note that this is NOT a relocatable package
# $Id: wireshark.spec.in 34996 2010-11-21 19:03:17Z wmeier $
%define ver      1.5.2
%define rel      2
%define prefix   /usr

Summary:	Network protocol analyzer
Name:		wireshark
Version:	%ver
Release:	%rel
License:	GPL
Group:		Networking/Utilities
Source:		http://wireshark.org/download/automated/src/%{name}-%{version}.tar.bz2
Source1:	%{name}.desktop
Source2:	%{name}.png
URL:		http://www.wireshark.org/
BuildRoot:	/tmp/wireshark-%{PACKAGE_VERSION}-root
Packager:	Gerald Combs <gerald[AT]wireshark.org>
Requires:	gtk2
Requires:	libpcap openssl

%description
Wireshark is a free network protocol analyzer for Unix and Windows. It
allows you to examine data from a live network or from a capture file
on disk. You can interactively browse the capture data, viewing summary
and detail information for each packet. Wireshark has several powerful
features, including a rich display filter language and the ability to
view the reconstructed stream of a TCP session.

%prep
%setup
CFLAGS="$RPM_OPT_FLAGS" ./configure --prefix=%prefix --with-ssl=/usr --with-krb5
make

%install
rm -rf $RPM_BUILD_ROOT
make DESTDIR=$RPM_BUILD_ROOT install

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-, root, root)
%doc AUTHORS COPYING ChangeLog INSTALL NEWS README*
%prefix/bin/*
%prefix/lib/*
%prefix/share/wireshark/*
%prefix/share/wireshark/*/*
%prefix/share/man/*/*

%changelog
* Thu Aug 10 2006 Joerg Mayer
- Starting with X.org 7.x X11R6 is being phased out. Install wireshark
  and manpage into the standard path.

* Mon Aug 01 2005 Gerald Combs
- Add a desktop file and icon for future use

- Take over the role of packager

- Update descriptions and source locations

* Thu Oct 28 2004 Joerg Mayer
- Add openssl requirement (heimdal and net-snmp are still automatic)

* Tue Jul 20 2004 Joerg Mayer
- Redo install and files section to actually work with normal builds

* Sat Feb 07 2004 Joerg Mayer
- in case there are shared libs: include them

* Tue Aug 24 1999 Gilbert Ramirez
- changed to ethereal.spec.in so that 'configure' can update
  the version automatically

* Tue Aug 03 1999 Gilbert Ramirez <gram@xiexie.org>
- updated to 0.7.0 and changed gtk+ requirement

* Sun Jan 01 1999 Gerald Combs <gerald@zing.org>
- updated to 0.5.1

* Fri Nov 20 1998 FastJack <fastjack@i-s-o.net>
- updated to 0.5.0

* Sun Nov 15 1998 FastJack <fastjack@i-s-o.net>
- created .spec file

