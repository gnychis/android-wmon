package com.gnychis.awmon.DeviceHandlers;

import java.util.Comparator;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.gnychis.awmon.Scanners.BluetoothScanner;

public class Bluetooth extends HardwareDevice {
	
	BluetoothAdapter _bluetooth;
	Context _parent;
	BluetoothScanner _monitor_thread;
	
	public Bluetooth(Context c) {
		super(HardwareDevice.Type.Bluetooth);
		_parent = c;
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
		_parent.registerReceiver(_messageReceiver, new IntentFilter());
	}

	public boolean startScan() {
		if(!stateChange(State.SCANNING))
			return false;
		
		_monitor_thread = new BluetoothScanner();
		_monitor_thread.execute(this);
		
		return true;
	}
	
	public void scanComplete() {
		stateChange(State.IDLE);
	}
	
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver _messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	 	
        }
    }; 
	
	public boolean 		isConnected() { return _bluetooth.isEnabled(); }
}
