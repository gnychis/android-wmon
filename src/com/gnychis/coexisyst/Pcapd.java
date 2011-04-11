package com.gnychis.coexisyst;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.stericson.RootTools.RootTools;

public class Pcapd extends AsyncTask<Context, Integer, String>
{
	Context parent;
	CoexiSyst coexisyst;
	private int PCAPD_WIFI_PORT = 2000; // be careful this is consistent with WifiMon
	
	@Override
	protected String doInBackground( Context ... params )
	{
		parent = params[0];
		coexisyst = (CoexiSyst) params[0];
		
		try {
			Log.d("Pcapd", "launching instance of pcapd");
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/bin/pcapd wlan0 " + Integer.toString(PCAPD_WIFI_PORT) + " &");
		} catch(Exception e) {
			Log.e("Pcapd", "error trying to start pcap daemon",e);
			return "FAIL";
		}
		
		return "OK";
	}
}
