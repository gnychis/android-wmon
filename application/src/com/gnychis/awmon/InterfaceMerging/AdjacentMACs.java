package com.gnychis.awmon.InterfaceMerging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

/**
 * The Purpose of this heuristic is to leverage the fact that interfaces on the same device often
 * have adjacent MAC addresses.  This simply looks for pairs that have addresses with a distance 
 * from each other specific by a parameter.
 * 
 * @author George Nychis (gnychis)
 */
@SuppressWarnings("unchecked")
public class AdjacentMACs extends MergeHeuristic {
	
	public static final int MAX_ADDRESS_DISTANCE = 1;	// The maximum distance to consider "adjacent"
	
	public AdjacentMACs(Context p) {
		super(p,Arrays.asList(Wifi.class, Bluetooth.class, LAN.class));
	}

	public Map<InterfacePair,MergeStrength> classifyInterfacePairs(List<InterfacePair> pairs) {
		
		Map<InterfacePair,MergeStrength> classifications = new HashMap<InterfacePair,MergeStrength>();
		
		
		
		return classifications;
	}
	
}
