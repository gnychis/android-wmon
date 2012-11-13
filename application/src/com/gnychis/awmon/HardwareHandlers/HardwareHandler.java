package com.gnychis.awmon.HardwareHandlers;

import java.util.ArrayList;

import android.content.Context;

import com.gnychis.awmon.Core.USBMon;

// The handlers to the devices must reside in the background service, because there is
// not guarantee the main activity (AWMon) is actually active or in use.  But, it is
// guaranteed that the background service is always running.  Therefore, this class
// should be instantiated in the BackgroundService.
public class HardwareHandler {
		
	public Context _parent;
	
	// Our devices that are accessible
	public ArrayList<InternalRadio> _internalRadios;
	
	protected USBMon _usbmon;
	
	public HardwareHandler(Context parent) {
		_parent=parent;
		
		// Setup the USB monitor thread
		_usbmon = new USBMon(parent);
		
		// Initialize the device handles and add them all to an ArrayList.  This makes
		// scanning easy by iterating through this list.
		_internalRadios = new ArrayList<InternalRadio>();
		_internalRadios.add(new Wifi(_parent));
		_internalRadios.add(new ZigBee(_parent));
		_internalRadios.add(new Bluetooth(_parent));
		_internalRadios.add(new LAN(_parent));
	}
}
