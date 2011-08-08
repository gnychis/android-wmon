package com.gnychis.coexisyst;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.stericson.RootTools.RootTools;

public class Zigcapd extends AsyncTask<Context, Integer, String>
{
	Context parent;
	int _port;
	
	public Zigcapd(int p) {
		_port = p;
	}
	
	@Override
	protected String doInBackground( Context ... params )
	{
		parent = params[0];
		String launch_s = "/data/data/com.gnychis.coexisyst/files/zigcapd " + Integer.toString(_port) + " &";
		
		try {
			Log.d("Zigcapd", "launching instance of zigcapd on port " + Integer.toString(_port));
			Log.d("Zigcapd", "launch command: " + launch_s);
			RootTools.sendShell(launch_s);
		} catch(Exception e) {
			Log.e("Zigcapd", "error trying to start zigcapd daemon",e);
			return "FAIL";
		}
		
		return "OK";
	}
}
