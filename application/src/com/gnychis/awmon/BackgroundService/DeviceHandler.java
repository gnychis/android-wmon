package com.gnychis.awmon.BackgroundService;

import java.util.ArrayList;

import android.content.Context;

import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.DeviceHandlers.Bluetooth;
import com.gnychis.awmon.DeviceHandlers.HardwareDevice;
import com.gnychis.awmon.DeviceHandlers.Wifi;
import com.gnychis.awmon.DeviceHandlers.ZigBee;

// The handlers to the devices must reside in the background service, because there is
// not guarantee the main activity (AWMon) is actually active or in use.  But, it is
// guaranteed that the background service is always running.  Therefore, this class
// should be instantiated in the BackgroundService.
public class DeviceHandler {
		
	public Context _parent;
	
	// Our devices that are accessible
	ArrayList<HardwareDevice> _hardwareDevices;
	
	public DeviceScanManager _dev_scan_manager;
	protected USBMon _usbmon;
	
	public DeviceHandler(Context parent) {
		_parent=parent;
		
		// Setup the USB monitor thread
		_usbmon = new USBMon(parent);
		_dev_scan_manager = new DeviceScanManager(this);
		
		// Initialize the device handles and add them all to an ArrayList.  This makes
		// scanning easy by iterating through this list.
		_hardwareDevices = new ArrayList<HardwareDevice>();
		for (HardwareDevice.Type type : HardwareDevice.Type.values()) {
			switch(type) {
				case Wifi:
					_hardwareDevices.add(new Wifi(_parent));
					break;
				case ZigBee:
					_hardwareDevices.add(new ZigBee(_parent));
					break;
				case Bluetooth:
					_hardwareDevices.add(new Bluetooth(_parent));
					break;
			}
		}

	}
}
