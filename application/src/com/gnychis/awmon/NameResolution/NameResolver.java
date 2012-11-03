package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.List;

import com.gnychis.awmon.Core.Radio;

abstract public class NameResolver {
	
	NameResolutionManager _nr_manager;
	
	// This is an array which will keep track of the support hardware types for each name resolver
	public List<Radio.Type> _supportedDeviceTypes;
	
	public NameResolver(NameResolutionManager nrm, List<Radio.Type> supportedDeviceTypes) {
		_supportedDeviceTypes = supportedDeviceTypes;
		_nr_manager = nrm;
	}
	
	// This method receives the incoming device list.  Go ahead and strip out the devices the
	// current name resolver does not support.  But they MUST be put back in the list before
	// it is returned.  Otherwise they will be lost forever.
	public ArrayList<Radio> resolveNames(ArrayList<Radio> devices) {
		
		ArrayList<Radio> unsupported = new ArrayList<Radio>();
		ArrayList<Radio> supported = new ArrayList<Radio>();
		ArrayList<Radio> merged = new ArrayList<Radio>();
		
		for(Radio dev : devices) {
			if(_supportedDeviceTypes.contains(dev._type))
				supported.add(dev);
			else
				unsupported.add(dev);
		}
		
		supported = resolveSupportedDevices(supported);
		
		// Merge the newly resolved supported devices with the unsupported and return.
		merged.addAll(supported);
		merged.addAll(unsupported);
		return merged;
	}
	
	abstract public ArrayList<Radio> resolveSupportedDevices(ArrayList<Radio> supportedDevices);
}
