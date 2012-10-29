package com.gnychis.awmon.DeviceScanners;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gnychis.awmon.Core.Device;
import com.gnychis.awmon.DeviceHandlers.HardwareDevice;

public class BluetoothDeviceScanner extends DeviceScanner {
	
	//_bluetooth.startDiscovery();
	boolean _bt_scan_complete;
	ArrayList<Device> _scanResult;
	
	public BluetoothDeviceScanner() {
		super(HardwareDevice.Type.Bluetooth);
	}
	
	@Override
	protected ArrayList<Device> doInBackground( HardwareDevice ... params )
	{
		_hw_device = params[0];
		_bt_scan_complete=false;
		
		_scanResult = new ArrayList<Device>();
	
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
    			Device dev = new Device(Device.Type.Bluetooth);
    			short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
    			dev._RSSI.add((int)rssi);
    			dev._MAC=bt_dev.getAddress();
    			dev._name=bt_dev.getName();
    			_scanResult.add(dev);
    		}
    		
    		if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
    			_bt_scan_complete=true;
    	}
    };  

}
