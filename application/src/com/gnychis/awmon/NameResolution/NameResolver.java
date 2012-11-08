package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.List;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WiredInterface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

abstract public class NameResolver {
	
	NameResolutionManager _nr_manager;
	
	// This is an array which will keep track of the support hardware types for each name resolver
	public List<WirelessInterface.Type> _supportedRadioTypes;
	public List<WiredInterface.Type> _supportedWiredTypes; 
	
	public NameResolver(NameResolutionManager nrm, List<WirelessInterface.Type> supportedRadioTypes, List<WiredInterface.Type> supportedWiredTypes) {
		_supportedRadioTypes = supportedRadioTypes;
		_supportedWiredTypes = supportedWiredTypes;
		_nr_manager = nrm;
	}
	
	// This method receives the incoming device list.  Go ahead and strip out the devices the
	// current name resolver does not support.  But they MUST be put back in the list before
	// it is returned.  Otherwise they will be lost forever.
	public ArrayList<Interface> resolveNames(ArrayList<Interface> interfaces) {
		
		ArrayList<Interface> supported = new ArrayList<Interface>();
		ArrayList<Interface> unsupported = new ArrayList<Interface>();
		ArrayList<Interface> merged = new ArrayList<Interface>();
		
		for(Interface iface : interfaces) {
			if(iface.getClass() == WirelessInterface.class && _supportedRadioTypes.contains(((WirelessInterface)iface)._radioType))
				supported.add(iface);
			else if(iface.getClass() == WiredInterface.class && _supportedWiredTypes.contains(((WiredInterface)iface)._wiredType))
				supported.add(iface);
			else
				unsupported.add(iface);
		}
		
		supported = resolveSupportedInterfaces(supported);
		
		// Merge the newly resolved supported devices with the unsupported and return.
		merged.addAll(supported);
		merged.addAll(unsupported);
		return merged;
	}
	
	abstract public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedRadios);
}
