package com.gnychis.awmon.InterfaceMerging;

import java.util.Arrays;

import android.content.Context;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;


/**
 * If we get two interfaces with the same MAC, they are truly one interface and so
 * we flatten them down to a single interface.  This happens, for example, with true
 * wireless clients.  We get a "LAN" response from them via an ARP scan, and then we
 * also see the same MAC over the wireless network.  This is mainly a Wifi/LAN thing,
 * but open to other protocols in the future if necessary.
 * 
 * @author George Nychis (gnychis)
 *
 */
public class SameMAC extends MergeHeuristic {

	private static final String TAG = "SameMAC";
	private static final boolean VERBOSE = true;
	
	@SuppressWarnings("unchecked")
	public SameMAC(Context p) {
		super(p,Arrays.asList(Wifi.class, Bluetooth.class, LAN.class));
	}
	
	public MergeStrength classifyInterfacePair(InterfacePair pair) {
		Interface left = pair.getLeft();
		Interface right = pair.getRight();
		
		debugOut("Left MAC: " + left._MAC + "  Right MAC: " + right._MAC);
		
		// If the MACs are equal, we are merging one way or the other!
		if(left._MAC.equals(right._MAC)) {
			
			if(left._type==Wifi.class && right._type==LAN.class) {
				debugOut("... FLATTEN_LEFT");
				return MergeStrength.FLATTEN_LEFT;
			}
			
			if(left._type==LAN.class && right._type==Wifi.class) {
				debugOut("... FLATTEN RIGHT");
				return MergeStrength.FLATTEN_RIGHT;
			}
			
			if(left._type==Wifi.class && right._type==Wifi.class) {
				debugOut("... Shouldn't matter, let's go LEFT");
				return MergeStrength.FLATTEN_LEFT;
			}
			
			// If for some reason we got a BT duplicate, just merge it either way
			if(left._type==Bluetooth.class && right._type==Bluetooth.class) {
				debugOut("... FLATTEN LEFT");
				return MergeStrength.FLATTEN_LEFT;
			}
		}
		debugOut("... UNDETERMINED");
		return MergeStrength.UNDETERMINED;
	}
	
	@SuppressWarnings("unused")
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
