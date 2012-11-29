package com.gnychis.awmon.InterfaceMerging;

import java.util.Arrays;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.Wifi;

/**
 * This heuristic is simple.  Merge the Wifi and Bluetooth interfaces on this phone together.
 * This is the phone that is in your hand, currently in use.
 * 
 * @author George Nychis (gnychis)
 */
public class ThisPhone extends MergeHeuristic {
	
	private static final String TAG = "ThisPhone";
	private static final boolean VERBOSE = false;
	
	private String _btMAC;
	private String _wifiMAC;
	
	@SuppressWarnings("unchecked")
	public ThisPhone(Context p) {
		super(p,Arrays.asList(Wifi.class, Bluetooth.class));
		
	    BluetoothAdapter btAdapt= null; 
    	btAdapt = BluetoothAdapter.getDefaultAdapter();
    	_btMAC = btAdapt.getAddress();
    	
		WifiManager wifi = (WifiManager) _parent.getSystemService(Context.WIFI_SERVICE);
		_wifiMAC=wifi.getConnectionInfo().getMacAddress();
	}
	
	public MergeStrength classifyInterfacePair(InterfacePair pair) {
		Interface left = pair.getLeft();
		Interface right = pair.getRight();
		
		if(left._MAC.equals(_btMAC) && right._MAC.equals(_wifiMAC))
			return MergeStrength.LIKELY;
		
		if(left._MAC.equals(_wifiMAC) && right._MAC.equals(_btMAC))
			return MergeStrength.LIKELY;
		
		return MergeStrength.UNDETERMINED;
	}
	
	@SuppressWarnings("unused")
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
