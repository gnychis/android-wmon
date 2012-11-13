package com.gnychis.awmon.HardwareHandlers;

import java.util.ArrayList;

import android.content.Context;

import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.InterfaceScanners.InterfaceScanManager;

// The handlers to the devices must reside in the background service, because there is
// not guarantee the main activity (AWMon) is actually active or in use.  But, it is
// guaranteed that the background service is always running.  Therefore, this class
// should be instantiated in the BackgroundService.
public class DeviceHandler {
		
	public Context _parent;
	
	// Our devices that are accessible
	public ArrayList<InternalRadio> _internalRadios;
	
	public InterfaceScanManager _deviceScanManager;
	protected USBMon _usbmon;
	
	public DeviceHandler(Context parent) {
		_parent=parent;
		
		// Setup the USB monitor thread
		_usbmon = new USBMon(parent);
		_deviceScanManager = new InterfaceScanManager(this);
		
		// Initialize the device handles and add them all to an ArrayList.  This makes
		// scanning easy by iterating through this list.
		_internalRadios = new ArrayList<InternalRadio>();
		_internalRadios.add(new Wifi(_parent));
		_internalRadios.add(new ZigBee(_parent));
		_internalRadios.add(new Bluetooth(_parent));
		_internalRadios.add(new LAN(_parent));
	}
}
