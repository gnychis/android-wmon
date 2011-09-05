package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

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
	
	public enum Scans {		// A List of possible scans to handle
		Wifi,
		ZigBee,
	}
	private Queue<Scans> _scan_list;
	private boolean _is_scanning;
	
	// Setup a handler to receive messages from the broadcast receivers
	// with the scan results.
	private Handler _coexisyst_handler;
	public Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			// An incoming message that a wifi scan was complete
			if(msg.obj == ThreadMessages.WIFI_SCAN_COMPLETE) {
				Log.d("NetworksScan", "Wifi scan is now complete");		// Log out
				_wifi_scan_result = _rcvr_80211._last_scan;				// Save the scan result
				startNextScan();										// Start next scan if there is one
			}
			if(msg.obj == ThreadMessages.ZIGBEE_SCAN_COMPLETE) {
				Log.d("NetworksScan", "ZigBee scan is now complete");	// Log out
				_zigbee_scan_result = _rcvr_ZigBee._last_scan;			// Save scan result
				startNextScan();										// Start next scan if there is one
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
		_scan_list = new LinkedList<Scans>();
		
		// Setup the receivers
		_rcvr_80211 = new WiFiScanReceiver(_handler);
		_rcvr_ZigBee = new ZigBeeScanReceiver(_handler);
		//_rcvr_BTooth = new BluetoothManager(this);
		//registerReceiver(_rcvr_80211, new IntentFilter(Wifi.WIFI_SCAN_RESULT));
		//registerReceiver(_rcvr_ZigBee, new IntentFilter(ZigBee.ZIGBEE_SCAN_RESULT));
	}

	// This method creates a list of scans that need to be completed, based
	// on the devices that are connected to the phone.  This returns a value
	// for the progress bar in the main UI to show scan progress.
	private int createScanList() {
		int max_progress = 0;
		
		_scan_list.clear();
		
		if(_wifi.isConnected()) {
			_scan_list.add(Scans.Wifi);
			if(Wifi._native_scan)
				max_progress += Wifi.channels.length;
			else if(!Wifi._one_shot_scan) 
				max_progress += Wifi.SCAN_WAIT_COUNTS;
			else
				max_progress += 1;
		}
		
		if(_zigbee.isConnected()) {
			_scan_list.add(Scans.ZigBee);
			max_progress += ZigBee.channels.length;
		}
		
		return max_progress;
	}
	
	// Start the next scan, based on what scans have already been completed.
	private void startNextScan() {
		
		// First, if the scan queue is empty, then there is nothing left to scan!
		if(_scan_list.size()==0) {
			networkScansComplete();
			return;
		}

		// Take the head of scan queue
		Scans next_scan = _scan_list.remove();
		
		switch(next_scan) {
			case Wifi:
				_wifi.APScan();
				break;
				
			case ZigBee:
				_zigbee.scanStart();
				break;
				
			default:
				Log.d("NetworksScan", "Error: should never have hit here, no next network?");
		}
	}
	
	// For other classes to determine if a scan is already going on
	public boolean isScanning() {
		return _is_scanning;
	}
	
	// A call to initiate the scan progress.  First we make sure that we
	// are not already scanning, and then we create a scan based on the
	// hardware that is connected.  This returns a value that is used
	// by a progress dialogue in the main UI to show scan progress.
	public int initiateScan() {
		int max_progress;
		
		// Just a double check
		if(_is_scanning || (!_zigbee.isConnected() && !_wifi.isConnected()))
			return -1;
		
		_is_scanning = true;
				
		_usbmon.stopUSBMon();	// We need to stop the USB monitor, otherwise it interferes
		resetScanResults();		// Clear the scan results for new results

		max_progress = createScanList();		// Create a list of scans to complete based on hardware connected
		startNextScan();						// Start the scan process
		
		return max_progress;
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
