package com.gnychis.awmon.BackgroundService;

import com.gnychis.awmon.DeviceHandlers.Wifi;

// The handlers to the devices must reside in the background service, because there is
// not guarantee the main activity (AWMon) is actually active or in use.  But, it is
// guaranteed that the background service is always running.  Therefore, this class
// should be instantiated in the BackgroundService.
public class DeviceHandler {
	
	public Wifi _wifi;
	
	public DeviceHandler() {
		//_wifi = new Wifi(awmon);

	}

}
