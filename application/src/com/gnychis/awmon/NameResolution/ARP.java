package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

public class ARP extends NameResolver {

	public static final String TAG = "ARP";
	public static final boolean VERBOSE = true;
	
	@SuppressWarnings("unchecked")
	public ARP(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Wifi.class,LAN.class));
	}
	
	public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedInterfaces) {
		
		ArrayList<String> arpRaw = BackgroundService.runCommand("arp_scan --interface=wlan0 -l -q 2> /dev/null");
		Map<String,String> arpResults = new HashMap<String,String>();
		
		for(String arpResponse : arpRaw) {  // Maps this as Map<MAC,IP>
			debugOut("Arp Response: " + arpResponse);
			arpResults.put(arpResponse.split("\t")[1].toLowerCase(), arpResponse.split("\t")[0]);
		}
		
		for(Interface iface : supportedInterfaces) {
			String IP = arpResults.get(iface._MAC.toLowerCase());	// Will return an IP or null
			debugOut("Checking ARP responses for " + iface._MAC.toLowerCase() + " .... (" + IP + ")");
			if(IP != null) {
				iface._IP = IP;
				debugOut("...." + iface._MAC + " --> " + iface._IP);
			}
		}
		return supportedInterfaces;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
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
