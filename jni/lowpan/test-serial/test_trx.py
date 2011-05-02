#!/usr/bin/env python

# Linux IEEE 802.15.4 userspace tools
#
# Copyright (C) 2008, 2009 Siemens AG
#
# Written-by: Dmitry Eremin-Solenikov
# Written-by: Sergey Lapin
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; version 2 of the License.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License along
#  with this program; if not, write to the Free Software Foundation, Inc.,
#  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

import sys,os,time
from termios import *
from test_DQ import *
saddr = "\xde\xad\xbe\xaf\xca\xfe\xba\xbe"
daddr = "\x6D\x77\x70\x61\x6E\x31\x00\x00"
panaddr = "\xff\xff"

packet = "\x21\xcc\xa5"
packet += panaddr
packet += daddr
packet += saddr
packet += "\x01\x80\xa5\x5a\x42\x7c"

if len(sys.argv) < 3:
	print "Bad arguments."
	print "Usage: %s tty channel" %(sys.argv[0])
	sys.exit(2)

cn = DQ(sys.argv[1])
print 'Result of close ' + hex(cn.close())
print 'Result of open ' + hex(cn.open())
try:
	while 1:
		print 'Result of set_channel' +hex(cn.set_channel(int(sys.argv[2])))
		print 'Result of set_state' +hex(cn.set_state(TX_MODE))
		print 'Result of send_block' +hex(cn.send_block(packet))
		print 'Result of set_state' +hex(cn.set_state(RX_MODE))
		time.sleep(1.0);
except KeyboardInterrupt:
		cn.close()
		sys.exit(2)
