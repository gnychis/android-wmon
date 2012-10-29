package com.gnychis.awmon.DeviceHandlers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class Bluetooth extends HardwareDevice {
	
	BluetoothAdapter _bluetooth;
	Context _parent;
	
	public Bluetooth(Context c) {
		super(HardwareDevice.Type.Bluetooth);
		_parent = c;
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
		_parent.registerReceiver(_messageReceiver, new IntentFilter());
	}
	
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver _messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	 	
        }
    }; 
	
	public boolean 		isConnected() { return _bluetooth.isEnabled(); }
}
