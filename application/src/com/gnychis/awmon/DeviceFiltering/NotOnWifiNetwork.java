package com.gnychis.awmon.DeviceFiltering;

import android.content.Context;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Device;

public class NotOnWifiNetwork extends DeviceFilter {

	public static final String TAG = "NotOnWifiNetwork";
	public static final boolean VERBOSE = true;
	
	public NotOnWifiNetwork(Context c) {
		super(c);
	}

	public FilterStrength getFilterResult(Device device) {
		
		String IP = device.getIP();
		
		// If the device has a Wifi radio that is *not* on the user's home network (i.e., it
		// has no valid IP.  Then take it out.
		if(device.hasWifiInterface() && IP==null) {
			debugOut("... FILTER: " + device.getName() + "  Manufacturer: " + device.getManufacturer());
			return FilterStrength.FILTER_OUT;
		}
		
		debugOut("... UNDETERMINED: " + device.getName() + "  Manufacturer: " + device.getManufacturer());
		return FilterStrength.UNDETERMINED;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
