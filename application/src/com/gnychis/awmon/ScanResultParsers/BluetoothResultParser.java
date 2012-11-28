package com.gnychis.awmon.ScanResultParsers;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;

public class BluetoothResultParser extends ScanResultParser {
	
	public BluetoothResultParser(Context c) { super(c); }

	public <T extends Object> ArrayList<Interface> returnInterfaces(ArrayList<T> scanResult) {
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		Hashtable<String,WirelessInterface> devs_in_list = new Hashtable<String,WirelessInterface>();	  
		
	    // Go through each scan result, and get the access point information
	    Iterator<T> results = scanResult.iterator();
	    while(results.hasNext()) {
	    	WirelessInterface iface = (WirelessInterface) results.next();	    	
	    	
	    	// Keep the device if we don't already have a record for it
	    	if(!devs_in_list.containsKey(iface._MAC)) {
	    		devs_in_list.put(iface._MAC, iface);  // mark that we've seen it
	    		interfaces.add(iface);
	    	} else {  // we already have it, but we can add multiple RSSI readings
	    		WirelessInterface tdev = devs_in_list.get(iface._MAC);
	    		tdev._RSSI.add(iface.averageRSSI());	// There is only 1 in the average list for BT
	    	}
	    }
	    
	    /*
	    // Now, create a Bluetooth interface for our local phone
	    BluetoothAdapter btAdapt= null; 
    	btAdapt = BluetoothAdapter.getDefaultAdapter();
    	String address= btAdapt.getAddress();
    	Interface localIface = new WirelessInterface(Bluetooth.class);
    	localIface._MAC = address;
    	try {
    		String localhostname = InetAddress.getLocalHost().getHostName();
    		localIface._ifaceName=localhostname;
    	} catch(Exception e) { }
		interfaces.add(localIface);
		*/
		return interfaces;
	}
}
