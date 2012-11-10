package com.gnychis.awmon.HardwareHandlers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

// This class is basically a wrapper around the internal Wifi radio (not the external
// radio with monitoring mode).  However, we not only want it to be on, but we want it to
// actually be associated.  This is to use it for ARP scans, etc, which will otherwise fail
// if it is not successfully associated.
public class LAN extends InternalRadio {

	BluetoothAdapter _bluetooth;
	
	public LAN(Context c) {
		super();
		_parent = c;
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
	}
	
	public boolean 		isConnected() { return false; }


}
