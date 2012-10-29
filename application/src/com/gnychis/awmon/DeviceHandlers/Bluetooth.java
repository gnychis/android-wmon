package com.gnychis.awmon.DeviceHandlers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class Bluetooth extends HardwareDevice {
	
	BluetoothAdapter _bluetooth;
	Context _parent;
	
	public Bluetooth(Context c) {
		super(HardwareDevice.Type.Bluetooth);
		_parent = c;
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
	}
	
	public boolean 		isConnected() { return _bluetooth.isEnabled(); }
}
