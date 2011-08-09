package com.gnychis.coexisyst;

import java.util.ArrayList;

// The purpose of this class is to keep track of a scan taking place across
// all of the protocols.  That way, we can cache results and determine when
// each of the protocols has been scanned for.
public class NetworksScan {
	public ArrayList<ZigBeeDev> _zigbee_scan_result;
	public ArrayList<WifiAP> _wifi_scan_result;
	
	// Set the results to null to begin with, so that we can easily check
	// when a scan of all protocols is complete.
	NetworksScan() {
		_zigbee_scan_result = null;
		_wifi_scan_result = null;
	}
	
	// A method to check if we have results for each of the networks
	public boolean isScanComplete() {
		if(_zigbee_scan_result==null)
			return false;
		if(_wifi_scan_result==null)
			return false;
		
		return true;
	}
	
	// Reset the scan
	public void resetScan() {
		_zigbee_scan_result=null;
		_wifi_scan_result=null;
	}
}
