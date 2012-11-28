package com.gnychis.awmon.DeviceFiltering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Device;

public class DeviceFilteringManager {

	private static final String TAG = "DeviceFilteringManager";
	private static final boolean VERBOSE = true;
	
	public static final String DEVICE_FILTERING_REQUEST = "awmon.devicefiltering.device_filtering_request";
	public static final String DEVICE_FILTERING_RESPONSE = "awmon.devicefiltering.device_filtering_response";
	
	Context _parent;
	Queue<Class<?>> _pendingResults;
	Stack<Class<?>> _filterQueue;
	
	State _state;
	public enum State {
		IDLE,
		FILTERING,
	}
	
	public DeviceFilteringManager(Context parent) {
		_parent = parent;
		_state = State.IDLE;
        
        _parent.registerReceiver(incomingEvent, new IntentFilter(DeviceFilter.DEVICE_FILTER_RESPONSE));
        _parent.registerReceiver(incomingEvent, new IntentFilter(DEVICE_FILTERING_REQUEST));
	}
	
	public void requestFiltering(ArrayList<Device> devices) {
		Intent i = new Intent();
		i.setAction(DeviceFilteringManager.DEVICE_FILTERING_REQUEST);
		i.putExtra("devices", devices);
		_parent.sendBroadcast(i);
	}
	
	private void registerFilter(List<? extends Class<? extends DeviceFilter>> filters) {
		_filterQueue = new Stack<Class<?>>();
		_pendingResults = new LinkedList < Class<?> >();
		for(Class<?> filter : filters) {
			debugOut("Registering the following device filter: " + filter.getName());
			_filterQueue.push(filter);
			_pendingResults.add(filter);
		}
	}
	
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
    	@SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
        	
        	switch(_state) {	// Based on the current state, decide what next to do
        	
	    		/***************************** IDLE **********************************/
	    		case IDLE:
	    			if(intent.getAction().equals(DEVICE_FILTERING_REQUEST)) {
	    				debugOut("Receiving an incoming device filtering request, triggering");
	    				ArrayList<Device> devices = (ArrayList<Device>) intent.getExtras().get("devices");
	    				
	    				registerFilter(Arrays.asList(NotOnWifiNetwork.class));
	    				
	    				_state = State.FILTERING;
	    				
	    				triggerNextFilter(devices);
	    			}
	    		break;
	    		
				/**************************** FILTERING ********************************/
	    		case FILTERING:
	    			if(intent.getAction().equals(DeviceFilter.DEVICE_FILTER_RESPONSE)) {
	    				ArrayList<Device> devices = (ArrayList<Device>) intent.getExtras().get("result");
	    				Class<?> filterType = (Class<?>) intent.getExtras().get("filter");
	    				
	    				// Remove this result as pending, then check if there are any more filters we need.
	    	        	debugOut("Received filter response from: " + filterType.getName());
	    	        	_pendingResults.remove(filterType);
	    	        	triggerNextFilter(devices);
	    	        	
	    	        	if(_pendingResults.size()==0) {
	    	        		// Broadcast out the list of devices that came from our interface scan, naming, and merging.
	    	        		Intent i = new Intent();
	    	        		i.setAction(DEVICE_FILTERING_RESPONSE);
	    	        		i.putExtra("result", devices);
	    	        		_parent.sendBroadcast(i);
	    	        		_state=State.IDLE;
	    	        		
	    	        		debugOut("Received responses from all of the heuristics, going back to idle.");
	    	        		return;
	    	        	}
	    			}
	    		break;
        	}
        }
    };
    
    @SuppressWarnings("unchecked")
	public boolean triggerNextFilter(ArrayList<Device> devices) {
		
		if(_filterQueue.size()==0)
			return false;
		
		Class<?> filterRequest = _filterQueue.pop();
		DeviceFilter filter = null;	
				
		if(filterRequest == NotOnWifiNetwork.class)
			filter = new NotOnWifiNetwork(_parent);
		
		if(filter == null)
			return false;
		
		filter.execute(devices);
		
		debugOut("Executing the filter: " + filter.getClass().getName());
		return true;		
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
