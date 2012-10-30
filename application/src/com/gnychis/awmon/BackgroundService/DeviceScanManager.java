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

import com.gnychis.awmon.Core.Device;
import com.gnychis.awmon.DeviceHandlers.HardwareDevice;
import com.gnychis.awmon.DeviceScanners.DeviceScanResult;
import com.gnychis.awmon.DeviceScanners.DeviceScanner;

// The purpose of this class is to keep track of a scan taking place across
// all of the protocols.  That way, we can cache results and determine when
// each of the protocols has been scanned for.
public class DeviceScanManager extends Activity { 
	
	private static final String TAG = "DeviceScanManager";
	
	public static final String DEVICE_SCAN_REQUEST = "awmon.scanmanager.request_scan";
	public static final String DEVICE_SCAN_RESULT = "awmon.scanmanager.scan_result";

	DeviceHandler _device_handler;
	ArrayList<Device> _deviceScanResults;
	Queue<HardwareDevice> _scanQueue;
	
	State _state;
	public enum State {
		IDLE,
		SCANNING,
	}

	public DeviceScanManager(DeviceHandler dh) {
		_device_handler=dh;
		_state = State.IDLE;
		
		// Register a receiver to handle the incoming scan requests
        _device_handler._parent.registerReceiver(new BroadcastReceiver()
        { @Override public void onReceive(Context context, Intent intent) { scanRequest(); }
        }, new IntentFilter(DEVICE_SCAN_REQUEST));
        
        _device_handler._parent.registerReceiver(incomingDeviceScan, new IntentFilter(DeviceScanner.DEVICE_SCAN_RESULT));
	}
	
	// On a scan request, we check for the hardware devices connected and then
	// put them in a queue which we will trigger scans on.
	public void scanRequest() {
		Log.d(TAG, "Receiving an incoming scanRequest()");
		if(_state==State.SCANNING)
			return;
		
		// Set the state to scanning, then clear the scan results.
		_state = State.SCANNING;
		_deviceScanResults = new ArrayList<Device>();
		
		// Put all of the devices in a queue that we will scan devices on
		_scanQueue = new LinkedList < HardwareDevice >();
		for (HardwareDevice hwDev : _device_handler._hardwareDevices) {
			if(hwDev.isConnected()) { 
				_scanQueue.add(hwDev);
				Log.d(TAG, "... adding hardware device type: " + hwDev.deviceType());
			}
		}
		
		// Start the chain of device scans by triggering one of them
		triggerNextDeviceScan();
	}
	
	// To trigger the next scan, we pull the next device from the queue.  If there are no
	// devices left, the scan is complete.
	public void triggerNextDeviceScan() {
		if(_scanQueue.isEmpty()) {
			deviceScanComplete();
			return;
		}
		
		HardwareDevice dev = _scanQueue.remove();
		dev.startDeviceScan();
	}
	
	// When the scan is complete, we send out a broadcast with the results.
	public void deviceScanComplete() {
		Intent i = new Intent();
		i.setAction(DEVICE_SCAN_RESULT);
		i.putExtra("result", _deviceScanResults);
		_device_handler._parent.sendBroadcast(i);
	}
	
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver incomingDeviceScan = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	DeviceScanResult scanResult = (DeviceScanResult) intent.getExtras().get("result");
        	for(Device dev : scanResult.devices)
        		_deviceScanResults.add(dev);
        	triggerNextDeviceScan();	// Now, trigger the next scan (if one is left)
        }
    }; 
}