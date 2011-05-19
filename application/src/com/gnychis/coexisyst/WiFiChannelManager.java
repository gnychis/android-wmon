package com.gnychis.coexisyst;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class WiFiChannelManager {
	protected class WifiMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		private static final String TAG = "WiFiScanner";

		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			Log.d(TAG, "a new Wifi channel manager thread was started");
			
			return "OK";
		}
	}
}
