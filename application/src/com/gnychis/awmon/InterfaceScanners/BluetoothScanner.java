package com.gnychis.awmon.InterfaceScanners;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;

public class BluetoothScanner extends InterfaceScanner {
	
	private static final String TAG = "BluetoothDeviceScanner";
	private static boolean VERBOSE=true;
	
	private static final int NUM_SCANS=3;
	
	public BluetoothAdapter _bluetooth;
	
	boolean _bt_scan_complete;
	int _scansLeft;
	ArrayList<Interface> _scanResult;
	
	public BluetoothScanner(Context c) {
		super(c, Bluetooth.class);
		_bluetooth = BluetoothAdapter.getDefaultAdapter();
	}
	
	@Override
	protected ArrayList<Interface> doInBackground( InternalRadio ... params )
	{
		Log.d(TAG, "Running a Bluetooth device scan");
		_hw_device = params[0];
		_bt_scan_complete=false;
		
		// Register the receivers and then start the discovery
		_hw_device._parent.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		_hw_device._parent.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		_bluetooth.startDiscovery();
		_scansLeft=NUM_SCANS-1;
		
		_scanResult = new ArrayList<Interface>();
		
		while(!_bt_scan_complete) 
			try { Thread.sleep(100); } catch(Exception e) {}
		
		_hw_device._parent.unregisterReceiver(bluetoothReceiver);
			
		return _result_parser.returnInterfaces(_scanResult);
	}
	
    // This receives updates when the phone either enters the home or leaves the home
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
    	public void onReceive(Context c, Intent intent) {
    		
    		if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
    			// Create a BluetoothDev which is a wrapper for BluetoothDevice where we can save
    			// the RSSI value of when we discovered the device.  Otherwise, you lose it because
    			// it's simply the last value of RSSI at the card.
    			BluetoothDevice bt_dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    			WirelessInterface dev = new WirelessInterface(Bluetooth.class);
    			short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
    			debugOut("Device: " + bt_dev.getAddress() + " " + bt_dev.getName() + " " + rssi);
    			dev._RSSI.add((int)rssi);
    			dev._MAC=bt_dev.getAddress();
    			dev._ifaceName=bt_dev.getName();
    			_scanResult.add(dev);
    		}
    		
    		if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {	
    			if(_scansLeft>0) {
    				_bluetooth.startDiscovery();
    				_scansLeft--;
    			} else {
    				_bt_scan_complete=true;
    			}
    		}
    	}
    };  
    public static void debugOut(String msg) {
    	if(VERBOSE)
    		Log.d(TAG, msg);
    }
}
