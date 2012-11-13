package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Stack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.BackgroundService.InterfaceScanManager.State;
import com.gnychis.awmon.DeviceAbstraction.Interface;

public class NameResolutionManager {

	private static final String TAG = "DeviceScanManager";
	
	public static final String NAME_RESOLUTION_REQUEST = "awmon.scanmanager.name_resolution_request";
	public static final String NAME_RESOLUTION_RESPONSE = "awmon.scanmanager.name_resolution_response";
	
	Context _parent;
	Queue<Class<?>> _pendingResults;
	Stack<NameResolver> _nameResolversOrdered;	// These should be kept in a heirarchy such that
												// it would be OK if the next resolver overwrote previous.
	
	State _state;
	public enum State {
		IDLE,
		SCANNING,
	}
	
	public NameResolutionManager(Context parent) {
		_parent = parent;
		_state = State.IDLE;
		
		// Register a receiver to handle the incoming resolution requests
        _parent.registerReceiver(new BroadcastReceiver()
        { @Override public void onReceive(Context context, Intent intent) { resolutionRequest(intent); }
        }, new IntentFilter(NAME_RESOLUTION_REQUEST));
	}
	
	// On a scan request, we check for the hardware devices connected and then
	// put them in a queue which we will trigger scans on.
	public void resolutionRequest(Intent intent) {
		Log.d(TAG, "Receiving an incoming name resolution request");
		ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("interfaces");
		
		_nameResolversOrdered = new Stack<NameResolver>();
		_nameResolversOrdered.push(new Zeroconf(this));
		_nameResolversOrdered.push(new OUI(this));
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
