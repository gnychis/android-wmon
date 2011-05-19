package com.gnychis.coexisyst;

import java.util.Hashtable;
import java.util.Map;

import com.stericson.RootTools.RootTools;

import android.os.AsyncTask;
import android.util.Log;

public class WiFiChannelManager {
	
	// http://en.wikipedia.org/wiki/List_of_WLAN_channels
	int[] channels24 = {1,2,3,4,5,6,7,8,9,10,11};
	int[] channels5 = {36,40,44,48,52,56,60,64,100,104,108,112,116,136,140,149,153,157,161,165};
	int scan_period = 110; // time to sit on each channel, in milliseconds
						   // 110 is to catch the 100ms beacon interval
	
	protected class WifiMon extends AsyncTask<Integer, Integer, String>
	{
		private static final String TAG = "WiFiChannelManager";

		
		@Override
		protected String doInBackground( Integer ... params )
		{
			Log.d(TAG, "a new Wifi channel manager thread was started");
			
			try {
				// For each of the channels, go through and scan
				for(int i=0; i<channels24.length; i++) {
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel " + Integer.toString(i));
					Thread.sleep(scan_period);
				}
				
				for(int i=0; i<channels5.length; i++) {
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel " + Integer.toString(i));
					Thread.sleep(scan_period);
				}
			} catch(Exception e) {
				Log.e(TAG, "error trying to scan channels", e);
			}
			
			return "OK";
		}
	}
}
