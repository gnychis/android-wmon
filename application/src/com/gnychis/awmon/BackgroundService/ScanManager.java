package com.gnychis.awmon.BackgroundService;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.Core.ScanRequest;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.HardwareHandler;
import com.gnychis.awmon.InterfaceMerging.InterfaceMergingManager;
import com.gnychis.awmon.InterfaceScanners.InterfaceScanManager;
import com.gnychis.awmon.NameResolution.NameResolutionManager;

@SuppressWarnings("unchecked")
public class ScanManager {
	
	private static final String TAG = "ScanManager";
	private static final boolean VERBOSE = true;
	
	Context _parent;								// Access to the parent class for broadcasts
	HardwareHandler _hardwareHandler;				// To have access to the internal radios
	NameResolutionManager _nameResolutionManager;	// For resolving the names of interfaces
	ScanRequest _workingRequest;					// The most recent scan request we are working on
	InterfaceScanManager _ifaceScanManager;			// Scan for interfaces.
	
	public static final String SCAN_REQUEST = "awmon.scanmanager.scan_request";
	public static final String SCAN_RESPONSE = "awmon.scanmanager.scan_response";

	State _state;
	public enum State {
		IDLE,
		SCANNING,
		NAME_RESOLUTION,
		INTERFACE_MERGING,
	}
	
	public enum ResultType {
		INTERFACES,
		DEVICES,
	}
	
	/** Parent is anything we can send a broadcast from.  HardwareHandler is needed to access the
	 * internal radios.  This allows us to see if the radio is connected and request a scan from them.
	  * 
	  * @param p  Any parent context.
	  * @param dh A device handler
	  *
	*/
	public ScanManager(Context p, HardwareHandler dh) {
		_state=State.IDLE;
		_parent=p;
		
		_hardwareHandler=dh;
		_nameResolutionManager = new NameResolutionManager(_parent);
		_ifaceScanManager = new InterfaceScanManager(dh);
		
		_parent.registerReceiver(incomingEvent, new IntentFilter(ScanManager.SCAN_REQUEST));
		_parent.registerReceiver(incomingEvent, new IntentFilter(InterfaceScanManager.INTERFACE_SCAN_RESULT));
		_parent.registerReceiver(incomingEvent, new IntentFilter(NameResolutionManager.NAME_RESOLUTION_RESPONSE));
		_parent.registerReceiver(incomingEvent, new IntentFilter(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE));

	}
	
	private void broadcastResults(ScanManager.ResultType type, ArrayList<?> results) {
		Intent i = new Intent();
		i.setAction(InterfaceScanManager.INTERFACE_SCAN_REQUEST);
		i.putExtra("type", type);
		i.putExtra("result", results);
		_parent.sendBroadcast(i);
	}
	
	/** This is an incoming scan request.  A ScanRequest must be passed with it so we know what kind of
	 * scan is being requested.*/
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	
        	switch(_state) {	// Based on the current state, decide what next to do
        	
        		/***************************** IDLE **********************************/
        		case IDLE:
        			
        			if(intent.getAction().equals(ScanManager.SCAN_REQUEST)) {
        				
        				debugOut("Got an incoming scan request in the idle state");
        				
        				// Get the type of scan request, then cache it as our active request
        				ScanRequest request = null;
        				if((request = (ScanRequest) intent.getExtras().get("request"))==null)
        					return;
        				_workingRequest = request;
        				debugOut("... doNameResolution: " + _workingRequest.doNameResolution()
        						 	+ "   doMerging: " + _workingRequest.doMerging());
        				
        				// Go ahead and switch out state to scanning, then send out the request
        				// for an interface scan.
        				Intent i = new Intent();
        				i.setAction(InterfaceScanManager.INTERFACE_SCAN_REQUEST);
        				_parent.sendBroadcast(i);
        				
        				_state=State.SCANNING;       // We are scanning now!	
        				debugOut("Sent the scan request to scan on the hardware");
        			}
        
    			break;
    			
    			/*************************** SCANNING ********************************/
        		case SCANNING:
        			
        			if(intent.getAction().equals(InterfaceScanManager.INTERFACE_SCAN_RESULT)) {
        				ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("result");
        				
        				debugOut("Received the results from the interface scan");
        				
        				// Now, we decide to do name resolution and merging.  If we do not do name resolution,
        				// then we do NOT merge.  This is because a significant portion of merging uses names.
        				if(!_workingRequest.doNameResolution()) {
        					broadcastResults(ScanManager.ResultType.INTERFACES, interfaces);
        					_state=State.IDLE;
        					debugOut("Name resolution was not set, returning to idle");
        					return;
        				}
        				
        				// Send the request to do name resolution on the interfaces, passing them along
        				Intent i = new Intent();
        				i.setAction(NameResolutionManager.NAME_RESOLUTION_REQUEST);
        				i.putExtra("interfaces", interfaces);
        				_parent.sendBroadcast(i);
        				
        				_state=ScanManager.State.NAME_RESOLUTION;
        				debugOut("Name resolution was set, we are now resolving!");
        			}
        			
    			break;
    			
    			/*************************** RESOLVING ********************************/
        		case NAME_RESOLUTION:
        			
        			if(intent.getAction().equals(NameResolutionManager.NAME_RESOLUTION_RESPONSE)) {
        				ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("result");
        				
        				debugOut("Receveived the interfaces from the name resolution manager");
        				
        				// If we are not doing merging (OK), then we just return the interfaces with names.
        				if(!_workingRequest.doMerging()) {
        					broadcastResults(ScanManager.ResultType.INTERFACES, interfaces);
        					_state=State.IDLE;
        					debugOut("Merging was not set, returning to the idle state");
        					return;
        				}
        				
        				// Send the request to do interface merging, passing them along
        				Intent i = new Intent();
        				i.setAction(InterfaceMergingManager.INTERFACE_MERGING_REQUEST);
        				i.putExtra("interfaces", interfaces);
        				_parent.sendBroadcast(i);
        				
    					_state=ScanManager.State.INTERFACE_MERGING;
    					debugOut("Merging was set, let's try to merge the devices in to interfaces");
        			}
        			
        		break;
        		
        		/**************************** MERGING *********************************/
        		case INTERFACE_MERGING:
        			
        			if(intent.getAction().equals(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE)) {
        				ArrayList<Device> devices = (ArrayList<Device>) intent.getExtras().get("result");
        				
        				debugOut("Receveived the devices from interface merging manager");
        				
        				// Finally, send out the result which is devices after merging the interfaces together
        				broadcastResults(ScanManager.ResultType.DEVICES, devices);
        				_state = State.IDLE;
        				return;
        			}
        			
        		break;
        		
        	}
        	
    	}
    };
    
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
