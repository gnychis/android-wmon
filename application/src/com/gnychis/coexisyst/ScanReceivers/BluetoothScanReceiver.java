package com.gnychis.coexisyst.ScanReceivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;
import com.gnychis.coexisyst.NetDevDefinitions.BluetoothDev;
import com.gnychis.coexisyst.NetDevDefinitions.ZigBeeNetwork;

public class BluetoothScanReceiver extends BroadcastReceiver {
	
	private static final String TAG = "BluetoothScanReceiver";
	public String devs_str[];
	private Handler _handler;
	public ArrayList<BluetoothDev> _last_scan;
	  
	// If the handler is not null, callbacks will be made
	public BluetoothScanReceiver(Handler h) {
	  super();
	  _handler = h;
	  _last_scan = new ArrayList<BluetoothDev>();
	}
	
	public String[] get_devs() {
		return devs_str;
	}
	
	public void reset() {
		_last_scan.clear();
	}
	
  Comparator<Object> comp = new Comparator<Object>() {
		public int compare(Object arg0, Object arg1) {
			if(((BluetoothDev)arg0).rssi() < ((BluetoothDev)arg1).rssi())
				return 1;
			else if( ((BluetoothDev)arg0).rssi() > ((BluetoothDev)arg1).rssi())
				return -1;
			else
				return 0;
		}
	  };
	  
//	@Override @SuppressWarnings("unchecked")
	public void onReceive(Context c, Intent intent) {
		
		if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			Log.d(TAG, "Got a device: " + device.getName());
			
			// Create a BluetoothDev which is a wrapper for BluetoothDevice where we can save
			// the RSSI value of when we discovered the device.  Otherwise, you lose it because
			// it's simply the last value of RSSI at the card.
			BluetoothDev d = new BluetoothDev(device);
			short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
			d.set_rssi(rssi);
			_last_scan.add(d);
			Collections.sort(_last_scan, comp);	// Keep the list sorted
		}
		
		if(_handler != null && BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
			Log.d(TAG, "Got an action that device discovery has finished");
			Message msg = new Message();
			msg.obj = ThreadMessages.BLUETOOTH_SCAN_COMPLETE;
			_handler.sendMessage(msg);
		}
	}
}
