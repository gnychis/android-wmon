package com.gnychis.coexisyst;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;

/*
 * Ditch thread messages, use the broadcast as the message of the
 * scan complete, otherwise it's redundant functionality.  Setup
 * the broadcast receivers in this class.
 * 
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
public class NetworksScan extends Activity {
	
	// Keep track of classes which we will interface to scan with
	Wifi _wifi;
	ZigBee _zigbee;
	USBMon _usbmon;
	
	// Scan receivers for incoming broadcasts (which include results)
	public WiFiScanReceiver _rcvr_80211;
	public ZigBeeScanReceiver _rcvr_ZigBee;
	//BroadcastReceiver _rcvr_BTooth;
	
	public ArrayList<ZigBeeNetwork> _zigbee_scan_result;
	public ArrayList<WifiAP> _wifi_scan_result;
	
	public boolean _is_scanning;  // to ensure we don't double-scan
	
	// Setup a handler to receive messages from the broadcast receivers
	// with the scan results.
	private Handler _coexisyst_handler;
	public Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if(msg.obj == ThreadMessages.WIFI_SCAN_COMPLETE) {
				Log.d("NetworksScan", "Wifi scan is now complete");
				_wifi_scan_result = _rcvr_80211._last_scan;
				if(isScanComplete())
					networkScansComplete();
			}
			if(msg.obj == ThreadMessages.ZIGBEE_SCAN_COMPLETE) {
				Log.d("NetworksScan", "ZigBee scan is now complete");
				_zigbee_scan_result = _rcvr_ZigBee._last_scan;
				if(isScanComplete())
					networkScansComplete();
			}
		}
	};
	
	// Set the results to null to begin with, so that we can easily check
	// when a scan of all protocols is complete.
	NetworksScan(Handler h, USBMon m, Wifi w, ZigBee z) {
		
		_coexisyst_handler = h;
		
		// Keep a local copy of the classes
		_usbmon = m;
		_wifi = w;
		_zigbee = z;
		
		_is_scanning = false;		
		_zigbee_scan_result = null;
		_wifi_scan_result = null;
		
		// Setup the receivers
		_rcvr_80211 = new WiFiScanReceiver(_handler);
		_rcvr_ZigBee = new ZigBeeScanReceiver(_handler);
		//_rcvr_BTooth = new BluetoothManager(this);
		//registerReceiver(_rcvr_80211, new IntentFilter(Wifi.WIFI_SCAN_RESULT));
		//registerReceiver(_rcvr_ZigBee, new IntentFilter(ZigBee.ZIGBEE_SCAN_RESULT));
	}
	
	// For other classes to determine if a scan is already going on
	public boolean isScanning() {
		return _is_scanning;
	}
	
	public int initiateScan() {
		int max=0;	
		
		// Just a double check
		if(_is_scanning || (!_zigbee.isConnected() && !_wifi.isConnected()))
			return -1;
		
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
		
		_is_scanning=true;
		
		_usbmon.stopUSBMon();	// We need to stop the USB monitor, otherwise it interferes
		resetScanResults();		// Clear the scan results for new results
		
		// start the scanning process, which happens in another thread
		if(_wifi.isConnected())
			_wifi.APScan();
		if(_zigbee.isConnected())
			_zigbee.scanStart();
		
		return max;
	}
	
	// A method to check if we have results for each of the networks
	public boolean isScanComplete() {
		if(_zigbee_scan_result==null && _zigbee.isConnected())
			return false;
		if(_wifi_scan_result==null && _wifi.isConnected())
			return false;
		
		return true;
	}
	
	private void networkScansComplete() {
		_usbmon.startUSBMon();
		_is_scanning=false;

		// Send a message to CoexiSyst that the network scans are complete
		Message msg = new Message();
		msg.obj = ThreadMessages.NETWORK_SCANS_COMPLETE;
		_coexisyst_handler.sendMessage(msg);
	}
	
	// Reset the scan
	public void resetScanResults() {
		_zigbee_scan_result=null;
		_wifi_scan_result=null;
	}
}
