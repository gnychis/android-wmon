package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.gnychis.awmon.DeviceAbstraction.Interface;

public class NameResolutionManager {

	Context _parent;
	List<NameResolver> _nameResolversOrdered;	// These should be kept in a heirarchy such that
												// it would be OK if the next resolver overwrote previous.
	
	public NameResolutionManager(Context parent) {
		_parent = parent;
		_nameResolversOrdered = new ArrayList<NameResolver>();
		_nameResolversOrdered.add(new OUI(this));
		_nameResolversOrdered.add(new Zeroconf(this));
	}
	
	// Takes a list of devices as a set of scan results, then goes through
	// the name resolvers and attempts to resolve the names.  This must return the
	// same set of devices, albeit updated.  Otherwise the device will be lost.
	public ArrayList<Interface> resolveDeviceNames(ArrayList<Interface> scanResults) {
		
		// Go through each resolver.  Overwrite the scan results with the new results.
		// Keep going through the heirarchy.  The most basic should be first (e.g., OUI).
		// That way if none of the higher level resolvers can find a name, it is the fallback.
		for(NameResolver resolver : _nameResolversOrdered)
			scanResults = resolver.resolveNames(scanResults); 
			
		return scanResults;
	}
}
