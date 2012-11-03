package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.List;

import com.gnychis.awmon.Core.Radio;

abstract public class NameResolver {
	
	NameResolutionManager _nr_manager;
	
	// This is an array which will keep track of the support hardware types for each name resolver
	public List<Radio.Type> _supportedRadios;
	
	public NameResolver(NameResolutionManager nrm, List<Radio.Type> supportedRadioTypes) {
		_supportedRadios = supportedRadioTypes;
		_nr_manager = nrm;
	}
	
	// This method receives the incoming device list.  Go ahead and strip out the devices the
	// current name resolver does not support.  But they MUST be put back in the list before
	// it is returned.  Otherwise they will be lost forever.
	public ArrayList<Radio> resolveNames(ArrayList<Radio> radios) {
		
		ArrayList<Radio> unsupported = new ArrayList<Radio>();
		ArrayList<Radio> supported = new ArrayList<Radio>();
		ArrayList<Radio> merged = new ArrayList<Radio>();
		
		for(Radio radio : radios) {
			if(_supportedRadios.contains(radio._type))
				supported.add(radio);
			else
				unsupported.add(radio);
		}
		
		supported = resolveSupportedRadios(supported);
		
		// Merge the newly resolved supported devices with the unsupported and return.
		merged.addAll(supported);
		merged.addAll(unsupported);
		return merged;
	}
	
	abstract public ArrayList<Radio> resolveSupportedRadios(ArrayList<Radio> supportedRadios);
}
