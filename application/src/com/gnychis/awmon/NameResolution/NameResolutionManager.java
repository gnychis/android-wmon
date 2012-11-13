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

@SuppressWarnings("unchecked")
public class NameResolutionManager {

	private static final String TAG = "DeviceScanManager";
	private static final boolean VERBOSE = true;
	
	public static final String NAME_RESOLUTION_REQUEST = "awmon.scanmanager.name_resolution_request";
	public static final String NAME_RESOLUTION_RESPONSE = "awmon.scanmanager.name_resolution_response";
	
	Context _parent;
	Queue<Class<?>> _pendingResults;
	Stack<Class<?>> _nameResolverQueue;	// These should be kept in a heirarchy such that
										// it would be OK if the next resolver overwrote previous.
	
	State _state;
	public enum State {
		IDLE,
		RESOLVING,
	}
	
	public NameResolutionManager(Context parent) {
		_parent = parent;
		_state = State.IDLE;
        
        _parent.registerReceiver(incomingEvent, new IntentFilter(NameResolver.NAME_RESOLVER_RESPONSE));
        _parent.registerReceiver(incomingEvent, new IntentFilter(NAME_RESOLUTION_REQUEST));

	}
	
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	
        	switch(_state) {	// Based on the current state, decide what next to do
        	
        		/***************************** IDLE **********************************/
        		case IDLE:
        			if(intent.getAction().equals(NAME_RESOLUTION_REQUEST)) {
        				debugOut("Receiving an incoming name resolution request, triggering");
        				ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("interfaces");
        				
        				// Set the state to scanning, then clear the scan results.
        				_state = State.RESOLVING;
        				
        				// Put all of the name resolves on to a stack.  Push last the one that you want to go first.
        				_nameResolverQueue = new Stack<Class<?>>();
        				_nameResolverQueue.push(Zeroconf.class);
        				_nameResolverQueue.push(OUI.class);
        				
        				triggerNextNameResolver(interfaces);
        			}
        		break;
        		
    			/*************************** RESOLVING ********************************/
        		case RESOLVING:
        			if(intent.getAction().equals(NameResolver.NAME_RESOLVER_RESPONSE)) {
        	        	ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("result");
        	        	Class<?> resolverType = (Class<?>) intent.getExtras().get("resolver");
        	        	
        	        	// Remove this result as pending, then check if there are any more resolutions we need.
        	        	debugOut("Received name resolution from: " + resolverType.getName());
        	        	_pendingResults.remove(resolverType);
        	        	triggerNextNameResolver(interfaces);
        	        	
        	        	if(_pendingResults.size()==0) {
        	        		Intent i = new Intent();
        	        		i.setAction(NAME_RESOLUTION_RESPONSE);
        	        		i.putExtra("result", interfaces);
        	        		_parent.sendBroadcast(i);
        	        		_state=State.IDLE;
        	        		debugOut("Received responses from all the name resolvers, going back to idle.");
        	        		return;
        	        	}
        			}
        		break;
        	}
        }
    };

	
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
		
		debugOut("Executing the name resolver: " + resolver.getClass().getName());
		return true;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
