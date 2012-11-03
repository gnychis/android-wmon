package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.Core.Radio;

public class ARP extends NameResolver {

	public static final String TAG = "ARP";
	public static final boolean VERBOSE = true;
	
	public ARP(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Radio.Type.Wifi));
	}
	
	public ArrayList<Radio> resolveSupportedRadios(ArrayList<Radio> supportedRadios) {
		
		ArrayList<String> arpRaw = BackgroundService.runCommand("arp_scan --interface=wlan0 -l -q");
		Map<String,String> arpResults = new HashMap<String,String>();
		
		for(String arpResponse : arpRaw) {  // Maps this as Map<MAC,IP>
			debugOut("Arp Response: " + arpResponse);
			arpResults.put(arpResponse.split("\t")[1].toLowerCase(), arpResponse.split("\t")[0]);
		}
		
		for(Radio radio : supportedRadios) {
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
}
