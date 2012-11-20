package com.gnychis.awmon.HardwareHandlers;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.gnychis.awmon.Core.UserSettings;

// This class is basically a wrapper around the internal Wifi radio (not the external
// radio with monitoring mode).  However, we not only want it to be on, but we want it to
// actually be associated.  This is to use it for ARP scans, etc, which will otherwise fail
// if it is not successfully associated.
public class LAN extends InternalRadio {
	
	WifiManager _wifi;
	UserSettings _settings;
	
	public LAN(Context c) {
		super(c);
		_wifi = (WifiManager) _parent.getSystemService(Context.WIFI_SERVICE);
		_settings = new UserSettings(_parent);
	}
	
	// We consider the "LAN" to be associated to a Wifi access point.  It should be the home AP
	// also to scan their LAN.
	public boolean isConnected() { 
		
        WifiInfo wifiinfo = _wifi.getConnectionInfo();
        
        if(wifiinfo.getIpAddress()==0)  // No IP address if this returns 0
        	return false;
        
        // We don't have their home MAC address, so we would skip also
        if(_settings.getHomeWifiMAC()==null)
        	return false;

        // Check if the MAC we have as their home AP is the associated one.
        if(!wifiinfo.getBSSID().equals(_settings.getHomeWifiMAC()))
        	return false;
        
		return true; 
	}


}
