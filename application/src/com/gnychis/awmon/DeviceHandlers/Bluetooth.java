package com.gnychis.awmon.DeviceHandlers;

import java.util.Comparator;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.gnychis.awmon.DeviceHandlers.HardwareDevice.State;
import com.gnychis.awmon.NetDevDefinitions.BluetoothDev;
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
}
