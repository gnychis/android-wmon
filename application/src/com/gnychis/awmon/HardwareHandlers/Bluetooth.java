package com.gnychis.awmon.HardwareHandlers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class Bluetooth extends InternalRadio {
	
	BluetoothAdapter _bluetooth;
	
	public Bluetooth(Context c) {
		super();
		_parent = c;
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
	}
	
	public boolean 		isConnected() { return _bluetooth.isEnabled(); }
}
