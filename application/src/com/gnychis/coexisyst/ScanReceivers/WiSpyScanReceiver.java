package com.gnychis.coexisyst.ScanReceivers;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;

public class WiSpyScanReceiver extends BroadcastReceiver {
	
	private static final String TAG = "WiSpyScanReceiver";
	private Handler _handler;
	public ArrayList<Integer> _last_scan;

	// If the handler is not null, callbacks will be made
	public WiSpyScanReceiver(Handler h) {
	  super();
	  _handler = h;
	}
	  
	// Unlike some of the other receivers, we do not have any post-processing
	// of these results yet.  So the callback is simple.
	@Override @SuppressWarnings("unchecked")
	public void onReceive(Context c, Intent intent) {
		Log.d(TAG, "Received incoming scan complete message");
		ArrayList<Integer> scan_result = (ArrayList<Integer>) intent.getExtras().get("spectrum-bins");
		
		_last_scan = scan_result;
		
	    if(_handler != null) {
			// Send a message to stop the spinner if it is running
			Message msg = new Message();
			msg.what = ThreadMessages.WISPY_SCAN_COMPLETE.ordinal();
			_handler.sendMessage(msg);
	    }
	}
}
