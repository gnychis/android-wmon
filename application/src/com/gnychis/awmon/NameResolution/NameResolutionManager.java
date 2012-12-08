package com.gnychis.awmon.NameResolution;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;

@SuppressWarnings("unchecked")
public class NameResolutionManager {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 

	private static final String TAG = "NameResolutionManager";
	private static final boolean VERBOSE = true;
	
	public static final String NAME_RESOLUTION_REQUEST = "awmon.nameresolution.name_resolution_request";
	public static final String NAME_RESOLUTION_RESPONSE = "awmon.nameresolution.name_resolution_response";
	
	Context _parent;
	Queue<Class<?>> _pendingResults;
	Stack<Class<?>> _nameResolverQueue;	// These should be kept in a heirarchy such that
										// it would be OK if the next resolver overwrote previous.
	
	FileOutputStream _data_ostream;
	JSONArray _resolutionStats;
	JSONObject _overallStats;
	
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
	
	public void requestNameResolution(ArrayList<Interface> interfaces) {
		Intent i = new Intent();
		i.setAction(NameResolutionManager.NAME_RESOLUTION_REQUEST);
		i.putExtra("interfaces", interfaces);
		_parent.sendBroadcast(i);
	}
	
	private void registerNameResolver(List<Class<? extends NameResolver>> resolvers) {
		_nameResolverQueue = new Stack<Class<?>>();
		_pendingResults = new LinkedList < Class<?> >();
		for(Class<?> resolver : resolvers) {
			debugOut("Registering the following name resolver: " + resolver.getName());
			_nameResolverQueue.push(resolver);
			_pendingResults.add(resolver);
		}
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
        				registerNameResolver(Arrays.asList(Zeroconf.class, 
        													SSDP.class, 
        													DNSHostName.class,
        													OUI.class));
        				
        				try {
        					_resolutionStats = new JSONArray(); 
    	    				_overallStats = new JSONObject();
    	    				_overallStats.put("interfaces", interfaces.size());
        				} catch(Exception e) {}
        				
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
        	        	
        	        	// Add the stats for this merge
        	        	try {
    	    	        	JSONObject mergeStat = new JSONObject();
    	    	        	mergeStat.put("name", resolverType.toString());
    	    	        	mergeStat.put("resolved", (Integer)intent.getExtras().get("resolved"));
    	    	        	mergeStat.put("given", (Integer)intent.getExtras().get("given"));
    	    	        	mergeStat.put("supported", (Integer)intent.getExtras().get("supported"));
    	    	        	ArrayList<String> manufacturers = (ArrayList<String>)intent.getExtras().get("manufacturers");
    	    	        	JSONArray jsonManus = new JSONArray();
    	    	        	for(String manu : manufacturers)
    	    	        		if(manu!=null)
    	    	        			jsonManus.put(manu);
    	    	        	mergeStat.put("manufacturers", jsonManus);
    	    	        	_resolutionStats.put(mergeStat);
        	        	} catch(Exception e) { }
        	        	
        	        	if(_pendingResults.size()==0) {
        	        		
            				try {
            					_data_ostream = _parent.openFileOutput("naming_activity.json", Context.MODE_WORLD_READABLE | Context.MODE_APPEND);
            					_overallStats.put("date", dateFormat.format(new Date()));
            					_overallStats.put("resolvers", _resolutionStats);
            					_data_ostream.write(_overallStats.toString().getBytes());
            					_data_ostream.write("\n".getBytes()); 
            					_data_ostream.close();
            				} catch(Exception e) {  }	
        	        		
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
		
		if(_nameResolverQueue.size()==0)
			return false;
		
		Class<?> resolverRequest = _nameResolverQueue.pop();
		NameResolver resolver = null;
		
		if(resolverRequest == Zeroconf.class)
			resolver = new Zeroconf(this);
		if(resolverRequest == OUI.class)
			resolver = new OUI(this);
		if(resolverRequest == SSDP.class)
			resolver = new SSDP(this);
		if(resolverRequest == DNSHostName.class)
			resolver = new DNSHostName(this);
		
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
