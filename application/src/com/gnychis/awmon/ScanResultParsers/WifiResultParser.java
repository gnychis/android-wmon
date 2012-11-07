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
	
	WirelessInterface getInterfaceFromMAC(ArrayList<WirelessInterface> devices, String MAC) {
		for(int i=0; i<devices.size(); i++)
			if(devices.get(i)._MAC==MAC)
				return devices.get(i);
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
	    	
	    	// Go through all of the wireless clients and make sure their general information is correct
	    	// Note that probe requests and responses do not count towards frequencies, because clients
	    	// are typically just hopping at this point looking for APs in range, and not sitting on that channel.
	    	for(String wc_addresses : wireless_clients) {
	    		if(pkt.getField("wlan.fc.type_subtype")=="0x04") {
	    			
	    		}
	    	}
	    	
	    	// Pull some information from the packet.  The transmitter address is the true wireless transmitter of
	    	// the packet.  It will not resolve to the source MAC address if the AP was relaying the packet, it
	    	// will resolve to the AP address if the true transmitter was the AP.
	    	String transmitter_addr = Wifi.getTransmitterAddress(pkt);
	    	String rssi_val = pkt.getField("radiotap.dbm_antsignal");
	    	int frequency = Integer.parseInt(pkt.getField("radiotap.channel.freq"));
	    	String wlan_source_addr = pkt.getField("wlan.sa");
	    	String receiver_addr = pkt.getField("wlan.ra");
	    	String bssid_addr = pkt.getField("wlan.bssid");
	    	String ssid_val = pkt.getField("wlan_mgt.ssid");
	    	
	    	// The RSSI belongs to the true transmitter
	    	//if(rssi_val!=null && getInterfaceFromMAC(devices, transmitter_addr)!=null)
	    	//	getInterfaceFromMAC(devices, transmitter_addr)._RSSI.add(Integer.parseInt(rssi_val));
	    	
	    	// Frequency belongs to the transmitter, BSSID, or receiver address. 
	    	
	    		
	    	// If the transmitter address is null, then use the source address.
	    	/*
	    	dev._MAC = (transmitter_addr==null) ? wlan_source_addr : transmitter_addr;
	    	
	    	// Probe requests use a BSSID of ff:ff:ff:ff:ff:ff, ignore it
	    	if(bssid_addr!=null && bssid_addr.equals("ff:ff:ff:ff:ff:ff"))
	    			bssid_addr=null;
	    	
	    	if(rssi_val!=null)
	    		dev._RSSI.add(Integer.parseInt(rssi_val));
	    	if(bssid_addr!=null)
	    		dev._BSSID = bssid_addr;
	    	if(ssid_val!=null)
	    		dev._SSID = ssid_val;
	    	
	    	// Keep the device if we don't already have a record for it
	    	if(!devs_in_list.containsKey(dev._MAC)) {
	    		devs_in_list.put(dev._MAC, dev);  // mark that we've seen it
	    		devices.add(dev);
	    	} else {  // we already have it, but we can add multiple RSSI readings
	    		WirelessInterface tdev = devs_in_list.get(dev._MAC);
	    		if(rssi_val!=null)
	    			tdev._RSSI.add(Integer.parseInt(rssi_val));
	    		if(tdev._BSSID==null && bssid_addr!=null)	// if we have a BSSID
	    			tdev._BSSID=bssid_addr;
	    		if(tdev._SSID==null && ssid_val!=null) 
	    			tdev._SSID=ssid_val;
	    	}*/
	    }
		
		return devices;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
	
}
