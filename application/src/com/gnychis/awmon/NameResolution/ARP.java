package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

import com.gnychis.awmon.Core.Device;

public class ARP extends NameResolver {

	public static final String TAG = "ARP";
	public static final boolean VERBOSE = true;
	
	public ARP(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Device.Type.Wifi));
	}
	
	public ArrayList<Device> resolveSupportedDevices(ArrayList<Device> supportedDevices) {
		for(Device dev : supportedDevices) {
			
		}
		return supportedDevices;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}

}
