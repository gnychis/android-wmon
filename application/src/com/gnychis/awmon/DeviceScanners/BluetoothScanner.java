package com.gnychis.awmon.DeviceScanners;

import android.os.AsyncTask;

import com.gnychis.awmon.DeviceHandlers.HardwareDevice;

public class BluetoothScanner extends AsyncTask<HardwareDevice, Integer, String> {
	
	HardwareDevice _hw_device;
	//_bluetooth.startDiscovery();
	
	@Override
	protected String doInBackground( HardwareDevice ... params )
	{
	
		return "OK";
	}

}
