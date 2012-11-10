package com.gnychis.awmon.Scanners;

import java.util.ArrayList;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;
import com.gnychis.awmon.HardwareHandlers.LAN;

// Scanning the LAN is doing an active ARP scan, and then seeing which devices
// are active on the LAN.
public class LANScanner extends Scanner {

	public LANScanner() {
		super(LAN.class);
	}
	
	@Override
	protected ArrayList<Interface> doInBackground( InternalRadio ... params )
	{
		_hw_device = params[0];		
		ArrayList<String> scanResult = BackgroundService.runCommand("arp_scan --interface=wlan0 -l -q 2> /dev/null");
		return _result_parser.returnInterfaces(scanResult);
	}

}
