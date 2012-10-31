package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;

import com.gnychis.awmon.Core.Device;

// Bonjour 
public class Zeroconf extends NameResolver {

	public Zeroconf(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Device.Type.Wifi));
	}
	
	public ArrayList<Device> resolveSupportedDevices(ArrayList<Device> supportedDevices) {
		return supportedDevices;
	}
	
}
