package com.gnychis.coexisyst;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothManager extends BroadcastReceiver {
	private static final String TAG = "BTManager";
	
	CoexiSyst coexisyst;
	int scans;
	
	public BluetoothManager(CoexiSyst coexisyst) {
		super();
		scans = 0;
		this.coexisyst = coexisyst;
		Log.d(TAG, "startup of BT manager successful");
	}
	
	public void onReceive(Context context, Intent intent) {
		
		
	}
}
