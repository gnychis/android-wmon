package com.gnychis.awmon.DeviceHandlers;

import java.util.Comparator;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import com.gnychis.awmon.AWMon.ThreadMessages;
import com.gnychis.awmon.NetDevDefinitions.BluetoothDev;

public class Bluetooth extends HardwareDevice {
	
	BluetoothAdapter _bluetooth;
	Context _parent;
	
	public Bluetooth(Context c) {
		_parent = c;
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
		_parent.registerReceiver(_messageReceiver, new IntentFilter());
	}

	public void startScan() {
		_bluetooth.startDiscovery();
	}
	
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver _messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	 	
        }
    }; 
	
	Comparator<Object> comp = new Comparator<Object>() {
		public int compare(Object arg0, Object arg1) {
			if(((BluetoothDev)arg0).rssi() < ((BluetoothDev)arg1).rssi())
				return 1;
			else if( ((BluetoothDev)arg0).rssi() > ((BluetoothDev)arg1).rssi())
				return -1;
			else
				return 0;
		}
  	};
	
	public boolean 		isConnected() { return _bluetooth.isEnabled(); }
	public DeviceType 	deviceType() { return DeviceType.Bluetooth; }
}
