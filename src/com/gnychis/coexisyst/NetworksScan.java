package com.gnychis.coexisyst;

import java.util.ArrayList;

/*
 * bt.enable();
 * unregisterReceiver(rcvr_BTooth);
 * wifi.setWifiEnabled(true);
 * 		_wifi_reenable = (wifi.isWifiEnabled()) ? true : false;
		_bt_reenable = (bt.isEnabled()) ? true : false;
		
registerReceiver(rcvr_BTooth, new IntentFilter(
		BluetoothDevice.ACTION_FOUND));

bt.startDiscovery();
*/

// The purpose of this class is to keep track of a scan taking place across
// all of the protocols.  That way, we can cache results and determine when
// each of the protocols has been scanned for.
public class NetworksScan {
	
	// Keep track of classes which we will interface to scan with
	Wifi _wifi;
	ZigBee _zigbee;
	
	public ArrayList<ZigBeeNetwork> _zigbee_scan_result;
	public ArrayList<WifiAP> _wifi_scan_result;
	
	public boolean _zigbee_connected;
	public boolean _wifi_connected;
	
	public boolean _is_scanning;
	
	// Set the results to null to begin with, so that we can easily check
	// when a scan of all protocols is complete.
	NetworksScan(Wifi w, ZigBee z) {
		
		// Keep a local copy of the classes
		_wifi = w;
		_zigbee = z;
		
		_zigbee_scan_result = null;
		_wifi_scan_result = null;
		_is_scanning = false;
	}
	
	public int initiateScan() {
		int max=0;	
		
		if(_wifi.isConnected()) {
			if(Wifi._native_scan)
				max += Wifi.channels.length;
			else if(!Wifi._one_shot_scan) 
				max += Wifi.SCAN_WAIT_COUNTS;
			else
				max += 1;
		}
		if(_zigbee.isConnected())
			max += ZigBee.channels.length;
		
		return 1;
	}
	
	// A method to check if we have results for each of the networks
	public boolean isScanComplete() {
		if(_zigbee_scan_result==null && _zigbee_connected)
			return false;
		if(_wifi_scan_result==null && _wifi_connected)
			return false;
		
		return true;
	}
	
	// Reset the scan
	public void resetScan() {
		_zigbee_scan_result=null;
		_wifi_scan_result=null;
		_is_scanning = false;
	}
}
