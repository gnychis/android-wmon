package com.gnychis.awmon.Scanners;

import java.util.ArrayList;

import android.os.AsyncTask;

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

	
	//**********************************************************************************************//
	// The purpose of this function is to create a background ARP scan that generates traffic to all
	// Wifi devices that are connected to the home network.  Useful to get some quick RSSI values.
	public static void backgroundARPScan(int num_scans) {
		BackgroundARPScan arp_scanner = new BackgroundARPScan();
		arp_scanner.execute(num_scans);
	}
	
	static final class BackgroundARPScan extends AsyncTask<Integer, String, String> {
		@Override
		protected String doInBackground(Integer... params) {
			int i = params[0];
			while(i-- > 0)
				BackgroundService.runCommand("arp_scan --interface=wlan0 -l -q");
			return "OK";
		}
	}
	//**********************************************************************************************//
}
