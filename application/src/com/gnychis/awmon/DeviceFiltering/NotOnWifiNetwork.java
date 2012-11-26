package com.gnychis.awmon.DeviceFiltering;

import android.content.Context;

import com.gnychis.awmon.DeviceAbstraction.Device;

public class NotOnWifiNetwork extends DeviceFilter {
	
	public NotOnWifiNetwork(Context c) {
		super(c);
	}

	public FilterStrength getFilterResult(Device device) {
		
		
		return FilterStrength.UNDETERMINED;
	}
}
