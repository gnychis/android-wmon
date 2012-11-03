package com.gnychis.awmon.DeviceScanners;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.Core.Radio;
import com.gnychis.awmon.DeviceHandlers.InternalRadio;

public class BluetoothDeviceScanner extends DeviceScanner {
	
	private static final String TAG = "BluetoothDeviceScanner";
	
	public BluetoothAdapter _bluetooth;
	
	boolean _bt_scan_complete;
	ArrayList<Radio> _scanResult;
	
	public BluetoothDeviceScanner() {
		super(Radio.Type.Bluetooth);
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
	}
	
	@Override
	protected ArrayList<Radio> doInBackground( InternalRadio ... params )
	{
		Log.d(TAG, "Running a Bluetooth device scan");
		_hw_device = params[0];
		_bt_scan_complete=false;
		
		// Register the receivers and then start the discovery
		_hw_device._parent.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		_hw_device._parent.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		_bluetooth.startDiscovery();
		
		_scanResult = new ArrayList<Radio>();
		
		while(!_bt_scan_complete) 
			try { Thread.sleep(100); } catch(Exception e) {}
		
		_hw_device._parent.unregisterReceiver(bluetoothReceiver);
			
		return _result_parser.returnDevices(_scanResult);
	}
	
    // This receives updates when the phone either enters the home or leaves the home
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
    	public void onReceive(Context c, Intent intent) {
    		
    		if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
    			// Create a BluetoothDev which is a wrapper for BluetoothDevice where we can save
    			// the RSSI value of when we discovered the device.  Otherwise, you lose it because
    			// it's simply the last value of RSSI at the card.
    			BluetoothDevice bt_dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    			Radio dev = new Radio(Radio.Type.Bluetooth);
    			short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
    			dev._RSSI.add((int)rssi);
    			dev._MAC=bt_dev.getAddress();
    			dev._name=bt_dev.getName();
    			_scanResult.add(dev);
    		}
    		
    		if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
    			_bt_scan_complete=true;
    		}
    	}
    };  

}
