package com.gnychis.coexisyst;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.stericson.RootTools.RootTools;

public class Pcapd extends AsyncTask<Context, Integer, String>
{
	Context parent;
	int _port;
	
	public Pcapd(int p) {
		_port = p;
	}
	
	@Override
	protected String doInBackground( Context ... params )
	{
		parent = params[0];
		String launch_s = "/data/data/com.gnychis.coexisyst/files/pcapd wlan0 " + Integer.toString(_port) + " &";
		
		try {
			Log.d("Pcapd", "launching instance of pcapd on port " + Integer.toString(_port));
			Log.d("Pcapd", "launch command: " + launch_s);
			RootTools.sendShell(launch_s);
		} catch(Exception e) {
			Log.e("Pcapd", "error trying to start pcap daemon",e);
			return "FAIL";
		}
		
		return "OK";
	}
}
