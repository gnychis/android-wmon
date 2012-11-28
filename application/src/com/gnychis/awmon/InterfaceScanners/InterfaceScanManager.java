package com.gnychis.awmon.InterfaceScanners;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.HardwareHandler;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;

// The purpose of this class is to keep track of a scan taking place across
// all of the protocols.  That way, we can cache results and determine when
// each of the protocols has been scanned for.
public class InterfaceScanManager extends Activity { 
	
	private static final String TAG = "InterfaceScanManager";
	private static final boolean VERBOSE = true;
	
	private static final boolean OVERLAP_SCANS = false;
	
	public static final String INTERFACE_SCAN_REQUEST = "awmon.scanmanager.interface_scan_request";
	public static final String INTERFACE_SCAN_RESULT = "awmon.scanmanager.interface_scan_result";

	HardwareHandler _hardwareHandler;
	ArrayList<Interface> _interfaceScanResults;
	Queue<InternalRadio> _scanQueue;
	Queue<Class<?>> _pendingResults;
	
	State _state;
	public enum State {
		IDLE,
		SCANNING,
	}

	public InterfaceScanManager(HardwareHandler dh) {
		_hardwareHandler=dh;
		_state = State.IDLE;

        _hardwareHandler._parent.registerReceiver(incomingEvent, new IntentFilter(InterfaceScanner.HW_SCAN_RESULT));
        _hardwareHandler._parent.registerReceiver(incomingEvent, new IntentFilter(InterfaceScanManager.INTERFACE_SCAN_REQUEST));

	}
	
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	
        	switch(_state) {	// Based on the current state, decide what next to do
        	
        		/***************************** IDLE **********************************/
        		case IDLE:
        			
        			if(intent.getAction().equals(INTERFACE_SCAN_REQUEST)) {
        				
        				debugOut("Got an incoming scan request in the idle state");
	        			_interfaceScanResults = new ArrayList<Interface>();
	
	        			// Put all of the devices in a queue that we will scan devices on
	        			_scanQueue = new LinkedList < InternalRadio >();
	        			_pendingResults = new LinkedList < Class<?> >();
	        			for (InternalRadio hwDev : _hardwareHandler._internalRadios) {
	        				if(hwDev.isConnected()) { 
	        					_scanQueue.add(hwDev);
	        					_pendingResults.add(hwDev.getClass());
	        				}
	        			}
	        			
	        			// Start the chain of device scans by triggering one of them
	        			triggerNextInterfaceScan();
	        			
	        			_state = State.SCANNING;
        				debugOut("State is now scanning");

        			}
        		break;
        		
    			/*************************** SCANNING ********************************/
        		case SCANNING:
        			
        			if(intent.getAction().equals(InterfaceScanner.HW_SCAN_RESULT)) {
        				
        	        	InterfaceScanResult scanResult = (InterfaceScanResult) intent.getExtras().get("result");
        	        	Class<?> ifaceType = InternalRadio.deviceType((String)intent.getExtras().get("hwType")); 
        				
        	        	debugOut("Got a hardware interface scan result from " + ifaceType.getName());
        	        	
        	        	for(Interface iface : scanResult._interfaces) 
        	        		_interfaceScanResults.add(iface);
        	        	
        	        	if(!OVERLAP_SCANS)				// If we are not overlapping scans, we do it when we get
        	        		triggerNextInterfaceScan();	// results of the previous scan
        	        	
        	        	// If we have all of the results we need, we can set it to complete
        	        	_pendingResults.remove(ifaceType);
        	        	
        	        	// Now check if we are done scanning, and if so we send out the results
        	        	if(_pendingResults.size()==0) {
        	        		Intent i = new Intent();
        	        		i.setAction(INTERFACE_SCAN_RESULT);
        	        		i.putExtra("result", _interfaceScanResults);
        	        		_hardwareHandler._parent.sendBroadcast(i);
        	        		_state=State.IDLE;
            	        	debugOut("Finished scanning on all hardware interfaces, broadcasting the results");
        	        	}
        			}
        		break;
        	}
        }
    };
	
	// To trigger the next scan, we pull the next device from the queue.  If there are no
	// devices left, the scan is complete.
	public void triggerNextInterfaceScan() {
		if(_scanQueue.isEmpty())
			return;
		
		InternalRadio dev = _scanQueue.remove();
		dev.startDeviceScan();
		debugOut("Triggering the next scan on: " + dev.deviceType().getName());
		
		if(OVERLAP_SCANS)				// If we are overlapping scans, just go ahead and
			triggerNextInterfaceScan();	// trigger the next one.
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}