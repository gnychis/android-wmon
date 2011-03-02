package com.gnychis.coexisyst;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BluetoothManager extends BroadcastReceiver {
	private static final String TAG = "BTManager";
	
	CoexiSyst coexisyst;
	int scans;
	
	public BluetoothManager(CoexiSyst coexisyst) {
		super();
		scans = 0;
		this.coexisyst = coexisyst;
		Log.d(TAG, "startup of BT manager successful");
        String ts = String.format("Results (%d)\n", scans);
        coexisyst.textStatus.setText(ts);
	}
	
	public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // When discovery finds a device
        Toast.makeText(coexisyst, "got a callback", Toast.LENGTH_LONG).show();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // Add the name and address to an array adapter to show in a ListView
            //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            String str = String.format("%s - %s", device.getName(), device.getAddress());
            coexisyst.textStatus.append(str);
            
    	    String message = String.format("Found Bluetooth device %s with address %s.",
    	    		device.getName(), device.getAddress());
    	    Toast.makeText(coexisyst, message, Toast.LENGTH_LONG).show();
        }
		
	}
}
