package com.gnychis.awmon.HardwareHandlers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

public class Bluetooth extends InternalRadio {
	
	BluetoothAdapter _bluetooth;
	
	public Bluetooth(Context c) {
		super(WirelessInterface.Type.Bluetooth);
		_parent = c;
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
	}
	
	public boolean 		isConnected() { return _bluetooth.isEnabled(); }
}
