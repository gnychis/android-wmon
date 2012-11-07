package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

public class ARP extends NameResolver {

	public static final String TAG = "ARP";
	public static final boolean VERBOSE = true;
	
	public ARP(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(WirelessInterface.Type.Wifi));
	}
	
	public ArrayList<WirelessInterface> resolveSupportedRadios(ArrayList<WirelessInterface> supportedRadios) {
		
		ArrayList<String> arpRaw = BackgroundService.runCommand("arp_scan --interface=wlan0 -l -q 2> /dev/null");
		Map<String,String> arpResults = new HashMap<String,String>();
		
		for(String arpResponse : arpRaw) {  // Maps this as Map<MAC,IP>
			debugOut("Arp Response: " + arpResponse);
			arpResults.put(arpResponse.split("\t")[1].toLowerCase(), arpResponse.split("\t")[0]);
		}
		
		for(WirelessInterface radio : supportedRadios) {
			String IP = arpResults.get(radio._MAC.toLowerCase());	// Will return an IP or null
			debugOut("Checking ARP responses for " + radio._MAC.toLowerCase() + " .... (" + IP + ")");
			if(IP != null) {
				radio._IP = IP;
				debugOut("...." + radio._MAC + " --> " + radio._IP);
			}
		}
		return supportedRadios;
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
