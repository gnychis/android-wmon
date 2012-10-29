package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;

import com.gnychis.awmon.Core.Packet;
import com.gnychis.awmon.NetDevDefinitions.Device;

public class WifiResultParser extends ScanResultParser {

	public ArrayList<Device> returnDevices(ArrayList<Object> scanResult) {
		
	    // For keeping track of the APs that we have already parsed, by MAC
	    Hashtable<String,Device> devs_in_list = new Hashtable<String,Device>();	    
	    ArrayList<Device> devices = new ArrayList<Device>();
	    
	    // Go through each scan result, and get the access point information
	    Iterator<Object> results = scanResult.iterator();
	    while(results.hasNext()) {
	    	Packet pkt = (Packet) results.next();
	    	Device dev = new Device(Device.Type.Wifi);
	    	int rssi = Integer.parseInt(pkt.getField("radiotap.dbm_antsignal"));
	    	
	    	// If it's a bad packet, ignore
	    	if(pkt.getField("radiotap.flags.fcs").equals("0"))
	    		continue;    	
	    	
	    	dev._MAC = pkt.getField("wlan.sa");
	    	dev._RSSI.add(rssi);
	    	dev._frequency = Integer.parseInt(pkt.getField("radiotap.channel.freq"));
	    	dev._BSSID = pkt.getField("wlan.bssid");
	    	
	    	// Keep the device if we don't already have a record for it
	    	if(!devs_in_list.containsKey(dev._MAC)) {
	    		devs_in_list.put(dev._MAC, dev);  // mark that we've seen it
	    		devices.add(dev);
	    	} else {  // we already have it, but we can add multiple RSSI readings
	    		Device tdev = devs_in_list.get(dev._MAC);
	    		tdev._RSSI.add(rssi);
	    	}
	    }

	    // Save this scan as our most current scan
	    Collections.sort(devices, Device.compareRSSI);
		
		return devices;
	}
	
}
