package com.gnychis.awmon.BackgroundService;

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
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.HardwareHandlers.DeviceHandler;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;
import com.gnychis.awmon.NameResolution.NameResolutionManager;
import com.gnychis.awmon.Scanners.ScanResult;
import com.gnychis.awmon.Scanners.Scanner;

// The purpose of this class is to keep track of a scan taking place across
// all of the protocols.  That way, we can cache results and determine when
// each of the protocols has been scanned for.
public class DeviceScanManager extends Activity { 
	
	private static final String TAG = "DeviceScanManager";
	
	private static final boolean OVERLAP_SCANS = true;
	private static final boolean NAME_RESOLUTION_ENABLED = true;
	
	public static final String DEVICE_SCAN_REQUEST = "awmon.scanmanager.request_scan";
	public static final String DEVICE_SCAN_RESULT = "awmon.scanmanager.scan_result";

	DeviceHandler _device_handler;
	NameResolutionManager _nameResolutionManager;
	ArrayList<Interface> _deviceScanResults;
	Queue<InternalRadio> _scanQueue;
	Queue<Class<?>> _pendingResults;
	
	State _state;
	public enum State {
		IDLE,
		SCANNING,
	}

	public DeviceScanManager(DeviceHandler dh) {
		_device_handler=dh;
		_nameResolutionManager = new NameResolutionManager(_device_handler._parent);
		_state = State.IDLE;
		
		// Register a receiver to handle the incoming scan requests
        _device_handler._parent.registerReceiver(new BroadcastReceiver()
        { @Override public void onReceive(Context context, Intent intent) { scanRequest(); }
        }, new IntentFilter(DEVICE_SCAN_REQUEST));
        
        _device_handler._parent.registerReceiver(incomingInterfaceScan, new IntentFilter(Scanner.DEVICE_SCAN_RESULT));
	}
	
	// On a scan request, we check for the hardware devices connected and then
	// put them in a queue which we will trigger scans on.
	public void scanRequest() {
		Log.d(TAG, "Receiving an incoming scanRequest()");
		if(_state==State.SCANNING)
			return;
		
		// Set the state to scanning, then clear the scan results.
		_state = State.SCANNING;
		_deviceScanResults = new ArrayList<Interface>();
		
		// Put all of the devices in a queue that we will scan devices on
		_scanQueue = new LinkedList < InternalRadio >();
		_pendingResults = new LinkedList < Class<?> >();
		for (InternalRadio hwDev : _device_handler._internalRadios) {
			if(hwDev.isConnected()) { 
				_scanQueue.add(hwDev);
				_pendingResults.add(hwDev.getClass());
			}
		}
		
		// Start the chain of device scans by triggering one of them
		triggerNextDeviceScan();
	}
	
	// To trigger the next scan, we pull the next device from the queue.  If there are no
	// devices left, the scan is complete.
	public void triggerNextDeviceScan() {
		if(_scanQueue.isEmpty())
			return;
		
		InternalRadio dev = _scanQueue.remove();
		dev.startDeviceScan();
		
		if(OVERLAP_SCANS)				// If we are overlapping scans, just go ahead and
			triggerNextDeviceScan();	// trigger the next one.
	}
	
	// When the scan is complete, we send out a broadcast with the results.
	public void deviceScanComplete() {
		
		if(NAME_RESOLUTION_ENABLED)		// Try to get user recognizable identifiers
			_nameResolutionManager.resolveDeviceNames(_deviceScanResults);
		
		_state=State.IDLE;
		Intent i = new Intent();
		i.setAction(DEVICE_SCAN_RESULT);
		i.putExtra("result", _deviceScanResults);
		_device_handler._parent.sendBroadcast(i);
	}
	
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver incomingInterfaceScan = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	ScanResult scanResult = (ScanResult) intent.getExtras().get("result");
        	WirelessInterface.Type hwType = (WirelessInterface.Type) intent.getExtras().get("hwType"); 
        	for(Interface iface : scanResult._interfaces) 
        		_deviceScanResults.add(iface);
        	
        	if(!OVERLAP_SCANS)				// If we are not overlapping scans, we do it when we get
        		triggerNextDeviceScan();	// results of the previous scan
        	
        	// If we have all of the results we need, we can set it to complete
        	_pendingResults.remove(hwType.getClass());
        	if(_pendingResults.size()==0)
        		deviceScanComplete();
        }
    }; 
}