package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.gnychis.awmon.Core.Radio;
import com.gnychis.awmon.Interfaces.MainMenu;

public class ARP extends NameResolver {

	public static final String TAG = "ARP";
	public static final boolean VERBOSE = true;
	
	public ARP(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Radio.Type.Wifi));
	}
	
	public ArrayList<Radio> resolveSupportedDevices(ArrayList<Radio> supportedDevices) {
		
		ArrayList<String> arpRaw = MainMenu.runCommand("arp_scan --interface=wlan0 -l -q");
		Map<String,String> arpResults = new HashMap<String,String>();
		
		for(String arpResponse : arpRaw) {  // Maps this as Map<MAC,IP>
			debugOut("Arp Response: " + arpResponse);
			arpResults.put(arpResponse.split("\t")[1].toLowerCase(), arpResponse.split("\t")[0]);
		}
		
		for(Radio dev : supportedDevices) {
			String IP = arpResults.get(dev._MAC.toLowerCase());	// Will return an IP or null
			debugOut("Checking ARP responses for " + dev._MAC.toLowerCase() + " .... (" + IP + ")");
			if(IP != null) {
				dev._IP = IP;
				debugOut("...." + dev._MAC + " --> " + dev._IP);
			}
		}
		return supportedDevices;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
