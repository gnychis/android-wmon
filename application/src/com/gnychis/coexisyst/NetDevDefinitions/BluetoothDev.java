package com.gnychis.coexisyst.NetDevDefinitions;

import android.bluetooth.BluetoothDevice;

// NOTE:  This is a very basic class, mainly just a wrapper around an already pretty rich class for Bluetooth
//        devices provided by the Android API. Some calls are just a little more simplified.

public class BluetoothDev {

	private BluetoothDevice _raw_device;
	private short _rssi;
	
	public BluetoothDev(BluetoothDevice d) {
		_raw_device = d;
	}
	
	public String name() {
		return _raw_device.getName();
	}
	
	public String mac() {
		return _raw_device.getAddress();
	}
	
	public void set_rssi(short r) { _rssi = r; }
	
	public short rssi() {
		return _rssi;
	}
	
}
