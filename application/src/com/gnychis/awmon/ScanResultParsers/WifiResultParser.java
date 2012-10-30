package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import android.util.Log;

import com.gnychis.awmon.Core.Device;
import com.gnychis.awmon.Core.Packet;

public class WifiResultParser extends ScanResultParser {
	
	final String TAG = "WifiResultParser";
	public final boolean VERBOSE = true;

	public <T extends Object> ArrayList<Device> returnDevices(ArrayList<T> scanResult) {
		
	    // For keeping track of the APs that we have already parsed, by MAC
	    Hashtable<String,Device> devs_in_list = new Hashtable<String,Device>();	    
	    ArrayList<Device> devices = new ArrayList<Device>();
	    
	    // Go through each scan result, and get the access point information
	    Iterator<T> results = scanResult.iterator();
	    while(results.hasNext()) {
	    	Packet pkt = (Packet) results.next();
	    	Device dev = new Device(Device.Type.Wifi);
	    	
	    	// If it's a bad packet, ignore
	    	if(pkt.getField("radiotap.flags.badfcs")==null || pkt.getField("radiotap.flags.badfcs").equals("1"))
	    		continue;
	    	
	    	// Pull some information from the packet
	    	String transmitter_addr = pkt.getField("wlan.ta");
	    	String source_addr = pkt.getField("wlan.sa");
	    	String bssid_addr = pkt.getField("wlan.bssid");
	    	String rssi_val = pkt.getField("radiotap.dbm_antsignal");
	    	
	    	// The transmitter address will either be the wlan.sa or wlan.ta.  If
	    	// both are null, let's just skip this packet
	    	if(transmitter_addr==null && source_addr==null)
	    		continue;
	    		
	    	// If the transmitter address is null, then use the source address.
	    	dev._MAC = (transmitter_addr==null) ? source_addr : transmitter_addr;
	    	if(rssi_val!=null)
	    		dev._RSSI.add(Integer.parseInt(rssi_val));
	    	dev._frequency = Integer.parseInt(pkt.getField("radiotap.channel.freq"));
	    	if(bssid_addr!=null)
	    		dev._BSSID = bssid_addr;
	    	
	    	// Keep the device if we don't already have a record for it
	    	if(!devs_in_list.containsKey(dev._MAC)) {
	    		devs_in_list.put(dev._MAC, dev);  // mark that we've seen it
	    		devices.add(dev);
	    	} else {  // we already have it, but we can add multiple RSSI readings
	    		Device tdev = devs_in_list.get(dev._MAC);
	    		if(rssi_val!=null)
	    			tdev._RSSI.add(Integer.parseInt(rssi_val));
	    		if(tdev._BSSID==null && bssid_addr!=null)	// if we have a BSSID
	    			tdev._BSSID=bssid_addr;
	    	}
	    }

	    // Save this scan as our most current scan
	    Collections.sort(devices, Device.compareRSSI);
		
		return devices;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
	
}
