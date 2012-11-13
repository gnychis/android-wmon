package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Stack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;

public class NameResolutionManager {

	private static final String TAG = "DeviceScanManager";
	static final boolean VERBOSE = true;
	
	public static final String NAME_RESOLUTION_REQUEST = "awmon.scanmanager.name_resolution_request";
	public static final String NAME_RESOLUTION_RESPONSE = "awmon.scanmanager.name_resolution_response";
	
	Context _parent;
	Queue<Class<?>> _pendingResults;
	Stack<Class<?>> _nameResolverQueue;	// These should be kept in a heirarchy such that
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
        { @Override public void onReceive(Context context, Intent intent) { nameResolutionRequest(intent); }
        }, new IntentFilter(NAME_RESOLUTION_REQUEST));
        
        _parent.registerReceiver(incomingResolverResponse, new IntentFilter(NameResolver.NAME_RESOLVER_RESPONSE));
	}
	
	// On a scan request, we check for the hardware devices connected and then
	// put them in a queue which we will trigger scans on.
	@SuppressWarnings("unchecked")
	public void nameResolutionRequest(Intent intent) {
		debugOut("Receiving an incoming name resolution request");
		
		if(_state!=State.IDLE)
			return;
		
		ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("interfaces");
		
		// Set the state to scanning, then clear the scan results.
		_state = State.SCANNING;
		
		// Put all of the name resolves on to a stack.  Push last the one that you want to go first.
		_nameResolverQueue = new Stack<Class<?>>();
		_nameResolverQueue.push(Zeroconf.class);
		_nameResolverQueue.push(OUI.class);
		
		triggerNextNameResolver(interfaces);
	}
	
	@SuppressWarnings("unchecked")
	public boolean triggerNextNameResolver(ArrayList<Interface> interfaces) {
		Class<?> resolverRequest = _nameResolverQueue.pop();
		NameResolver resolver = null;
		
		if(resolverRequest==null)
			return false;
		
		if(resolverRequest == Zeroconf.class)
			resolver = new Zeroconf(this);
		if(resolverRequest == OUI.class)
			resolver = new OUI(this);
		
		if(resolver == null)
			return false;
		
		resolver.execute(interfaces);
		return true;
	}
	
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver incomingResolverResponse = new BroadcastReceiver() {
    	@SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
        	ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("result");
        	Class<?> resolverType = (Class<?>) intent.getExtras().get("resolver");
        	
        	// Remove this result as pending, then check if there are any more resolutions we need.
        	_pendingResults.remove(resolverType);
        	if(_pendingResults.size()==0)
        		nameResolutionComplete(interfaces);
        	else
        		triggerNextNameResolver(interfaces);
        }
    };
    
	private void nameResolutionComplete(ArrayList<Interface> interfaces) {
		Intent i = new Intent();
		i.setAction(NAME_RESOLUTION_RESPONSE);
		i.putExtra("result", interfaces);
		_parent.sendBroadcast(i);
	}

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
