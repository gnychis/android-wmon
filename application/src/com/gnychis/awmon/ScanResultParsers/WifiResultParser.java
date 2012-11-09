package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.gnychis.awmon.Core.Packet;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.HardwareHandlers.Wifi;

public class WifiResultParser extends ScanResultParser {
	
	final String TAG = "WifiResultParser";
	public final boolean VERBOSE = true;
	
	WirelessInterface getInterfaceFromMAC(ArrayList<Interface> devices, String MAC) {
		for(int i=0; i<devices.size(); i++)
			if(devices.get(i)._MAC.equals(MAC))
				return (WirelessInterface) devices.get(i);
		return null;
	}

	public <T extends Object> ArrayList<Interface> returnInterfaces(ArrayList<T> scanResult) {
		
	    ArrayList<Interface> devices = new ArrayList<Interface>();
	   
	    // Go through each scan result, and get the access point information
	    Iterator<T> results = scanResult.iterator();
	    while(results.hasNext()) {
	    	Packet pkt = (Packet) results.next();
	    	
	    	// If it's a bad packet, ignore
	    	if(pkt.getField("radiotap.flags.badfcs")==null || pkt.getField("radiotap.flags.badfcs").equals("1"))
	    		continue;
	    	
	    	// For all addresses that are a part of the packet and confirmed to be true
	    	// and active wireless clients...
	    	List<String> wireless_clients = Wifi.getWirelessAddresses(pkt);
	    	int frequency = Integer.parseInt(pkt.getField("radiotap.channel.freq"));
	    	
	    	// Go through all of the wireless clients and make sure their general information is correct
	    	// Note that probe requests do not count towards frequencies, because clients
	    	// are typically just hopping at this point looking for APs in range, and not sitting on that channel.
	    	for(String wc_mac : wireless_clients) {
	    		
	    		WirelessInterface wiface = getInterfaceFromMAC(devices, wc_mac);
	    		
	    		// If we do not yet have this device in our list of wireless devices yet...
	    		if(wiface==null) {
	    			wiface = new WirelessInterface(Wifi.class);
	    			wiface._MAC=wc_mac;
	    			devices.add(wiface);
	    		}
	    		
	    		// Can save the associated BSSID and frequency if this is not a probe request/response
	    		if(pkt.getField("wlan.fc.type_subtype")!="0x04" && pkt.getField("wlan.fc.type_subtype")!="0x05") {
	    			if(pkt.getField("wlan.bssid")!=null)
	    				wiface._BSSID=pkt.getField("wlan.bssid");
	    			wiface._frequency=frequency;
	    		}
	    	}
	    }
		
		return devices;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
	
}
