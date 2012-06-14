package com.gnychis.coexisyst.ScanReceivers;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothScanReceiver extends BroadcastReceiver {
	
	private static final String TAG = "BluetoothScanReceiver";
	public String devs_str[];
	private Handler _handler;
	//public ArrayList<BluetoothDevice> _last_scan;
	  
	// If the handler is not null, callbacks will be made
	public BluetoothScanReceiver(Handler h) {
	  super();
	  _handler = h;
	}
	
	public String[] get_devs() {
		return devs_str;
	}
	  
	@Override @SuppressWarnings("unchecked")
	public void onReceive(Context c, Intent intent) {
		
		if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			Log.d("BluetoothDev", "Got a device: " + device.getName());
		}
		
		if(_handler != null && BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
			Log.d("BluetoothDev", "Got an action that device discovery has finished");
			Message msg = new Message();
			msg.obj = ThreadMessages.BLUETOOTH_SCAN_COMPLETE;
			_handler.sendMessage(msg);
		}
	}
}
