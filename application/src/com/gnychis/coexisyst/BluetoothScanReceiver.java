package com.gnychis.coexisyst;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothScanReceiver extends BroadcastReceiver {

	  private static final String TAG = "BluetoothScanReceiver";
	  public String devs_str[];
	  private Handler _handler;
	  //public ArrayList<BluetoothDevice> _last_scan;
	  
	  // If the handler is not null, callbacks will be made
	  public BluetoothScanReceiver(Handler h) {
	    super();
	    _handler = h;
	  }
	
	  public String[] get_devs() {
		  return devs_str;
	  }
	  
	  @Override @SuppressWarnings("unchecked")
	  public void onReceive(Context c, Intent intent) {
		  Log.d(TAG, "Received incoming scan complete message");
		  
	    if(_handler != null) {
			// Send a message to stop the spinner if it is running
			Message msg = new Message();
			msg.obj = ThreadMessages.BLUETOOTH_SCAN_COMPLETE;
			_handler.sendMessage(msg);
	    }
	  }
}
