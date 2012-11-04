package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import com.gnychis.awmon.DeviceAbstraction.WirelessRadio;

public class BluetoothResultParser extends ScanResultParser {

	public <T extends Object> ArrayList<WirelessRadio> returnDevices(ArrayList<T> scanResult) {
		ArrayList<WirelessRadio> devices = new ArrayList<WirelessRadio>();
		Hashtable<String,WirelessRadio> devs_in_list = new Hashtable<String,WirelessRadio>();	  
		
	    // Go through each scan result, and get the access point information
	    Iterator<T> results = scanResult.iterator();
	    while(results.hasNext()) {
	    	WirelessRadio dev = (WirelessRadio) results.next();	    	
	    	
	    	// Keep the device if we don't already have a record for it
	    	if(!devs_in_list.containsKey(dev._MAC)) {
	    		devs_in_list.put(dev._MAC, dev);  // mark that we've seen it
	    		devices.add(dev);
	    	} else {  // we already have it, but we can add multiple RSSI readings
	    		WirelessRadio tdev = devs_in_list.get(dev._MAC);
	    		tdev._RSSI.add(dev.averageRSSI());	// There is only 1 in the average list for BT
	    	}
	    }
		
		return devices;
	}
}
