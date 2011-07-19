package com.gnychis.coexisyst;

import java.util.Comparator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;

// Can pass a handler that will perform a callback when a scan
// is received.  This is helpful for alerting the parent class
// of the incoming scan.
public class WiFiScanReceiver extends BroadcastReceiver {
  private static final String TAG = "WiFiScanReceiver";
  public String nets_str[];
  private Handler _handler;

  // If the handler is not null, callbacks will be made
  public WiFiScanReceiver(Handler h) {
    super();
    _handler = h;
  }
  
  public String[] get_nets() {
	  return nets_str;
  }
  
  Comparator<Object> comp = new Comparator<Object>() {
	public int compare(Object arg0, Object arg1) {
		if(((ScanResult)arg0).level < ((ScanResult)arg1).level)
			return 1;
		else if( ((ScanResult)arg0).level > ((ScanResult)arg1).level)
			return -1;
		else
			return 0;
	}
  };

  @Override
  public void onReceive(Context c, Intent intent) {
	ScanResult bestSignal = null;  
    int i=0;
    
    Log.d(TAG, "Received incoming scan complete message");
    
    if(_handler != null) {
		// Send a message to stop the spinner if it is running
		Message msg = new Message();
		msg.obj = ThreadMessages.WIFI_SCAN_COMPLETE;
		_handler.sendMessage(msg);
    }
  }

}