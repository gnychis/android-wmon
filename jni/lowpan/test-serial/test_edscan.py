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

if len(sys.argv) < 2:
	print "Bad arguments"
	sys.exit(2)

cn = DQ(sys.argv[1])
print 'Result of open ' + hex(cn.open())
for i in range(1, 12):
	print 'Result of set_channel ' + hex(cn.set_channel(i))
	time.sleep(1)
	m = 0
	res = 5
	while res != 0 or m > 60:
		res = cn.set_state(RX_MODE)
		print "Got res %d" %(res)
		m = m + 1
		time.sleep(1)
	if res == 5 or res == 8:
		print "Unable to set RX mode :("
		cn.close()
		sys.exit(2)
	print 'Result of ed for ' + str(i) + ' is ' + hex(cn.ed()) + ' ' + hex(ord(cn.data))
#	print 'Result of cca ' + str(i) + ' is ' + hex(cn.cca()) + ' ' + cn.strstatus
print 'Result of close ' + hex(cn.close())
sys.exit(2)

