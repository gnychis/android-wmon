package com.gnychis.awmon.DeviceScanners;

import java.util.ArrayList;

import com.gnychis.awmon.Core.Device;
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
		
		ArrayList<Device> scanResult = new ArrayList<Device>();
	
		return _result_parser.returnDevices(scanResult);
	}

}
