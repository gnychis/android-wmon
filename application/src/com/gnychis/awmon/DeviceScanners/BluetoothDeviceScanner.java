package com.gnychis.awmon.DeviceScanners;

import com.gnychis.awmon.DeviceHandlers.HardwareDevice;

public class BluetoothDeviceScanner extends DeviceScanner {
	
	//_bluetooth.startDiscovery();
	
	public BluetoothDeviceScanner() {
		super(HardwareDevice.Type.Bluetooth);
	}
	
	@Override
	protected ArrayList<Device> doInBackground( HardwareDevice ... params )
	{
		_hw_device = params[0];
	
		return "OK";
	}

}
