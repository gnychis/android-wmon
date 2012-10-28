package com.gnychis.awmon.NetDevDefinitions;

// This is the baseline device class from which other devices can be derived.
public class Device {
	public int _RSSI;		// The RSSI of the device at the phone
	public String _MAC;		// The MAC address
	public int _frequency;	// The frequency it operates on
	public String _name;	// Something human readable
	
}
