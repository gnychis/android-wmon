package com.gnychis.awmon.NetDevDefinitions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

// This is the baseline device class from which other devices can be derived.
public class Device {
	
	public enum Type {		// A List of possible scans to handle
		Wifi,
		ZigBee,
		Bluetooth,
	}
	
	public ArrayList<Integer> _RSSI;		// The RSSI of the device at the phone
	public String _MAC;						// The MAC address of the device
	public int _frequency;					// The frequency it operates on
	public String _name;					// Something human readable (e.g., "Bill's iPad)
	public Device.Type _type;				// The type of device
	public String _SSID;					// If the device belongs to a SSID (e.g., "The Smith's Wifi")
	public String _BSSID;					// The BSSID (MAC) of the coordinator
	
	public Device(Device.Type type) {
		_RSSI = new ArrayList<Integer>();
		_type=type;
		_SSID = null;
		_BSSID = null;
		_MAC=null;
		_frequency = -1;
	}
	
	// Report the average RSSI
	public int averageRSSI() {
		
		Iterator<Integer> rssis = _RSSI.iterator();
		int sum=0;
		while(rssis.hasNext()) {
			int i = rssis.next().intValue();
			sum += i;
		}
		
		return sum / _RSSI.size();
	}
	
	  static public Comparator<Object> compareRSSI = new Comparator<Object>() {
		public int compare(Object arg0, Object arg1) {
			if(((Device)arg0).averageRSSI() < ((Device)arg1).averageRSSI())
				return 1;
			else if( ((Device)arg0).averageRSSI() > ((Device)arg1).averageRSSI())
				return -1;
			else
				return 0;
		}
	  };
	
}
