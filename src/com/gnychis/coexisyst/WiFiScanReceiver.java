package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

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
  public ArrayList<WifiAP> _last_scan;

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
		if(((WifiAP)arg0)._rssi < ((WifiAP)arg1)._rssi)
			return 1;
		else if( ((WifiAP)arg0)._rssi > ((WifiAP)arg1)._rssi)
			return -1;
		else
			return 0;
	}
  };

  @Override
  public void onReceive(Context c, Intent intent) {
    ArrayList<WifiAP> parsed_result = new ArrayList<WifiAP>();
    Hashtable<String,String> aps_in_list = new Hashtable<String,String>();
    ArrayList<Packet> scan_result = (ArrayList<Packet>) intent.getExtras().get("packets");
    
    Log.d(TAG, "Received incoming scan complete message");
    
    // Go through each scan result, and get the access point information
    Iterator<Packet> results = scan_result.iterator();
    while(results.hasNext()) {
    	Packet pkt = results.next();
    	WifiAP ap = new WifiAP();
    	
    	// Kind of like caching the important stuff to be readily accessible
    	ap._frequency = Integer.parseInt(pkt.getField("radiotap.channel.freq"));
    	ap._mac = pkt.getField("wlan.sa");
    	ap._ssid = pkt.getField("wlan_mgt.ssid");
    	ap._rssi = Integer.parseInt(pkt.getField("radiotap.dbm_antsignal"));
    	ap._beacon = pkt;
    	
    	// Keep the AP if we don't already have a record for it (a single scan
    	// might catch multiple beacons from the AP).
    	if(!aps_in_list.containsKey(ap._mac)) {
    		aps_in_list.put(ap._mac, "");  // mark that we've seen it
    		parsed_result.add(ap);
    	}
    }
    
    // Save this scan as our most current scan
    Collections.sort(parsed_result, comp);
    _last_scan = parsed_result;
    
    if(_handler != null) {
		// Send a message to stop the spinner if it is running
		Message msg = new Message();
		msg.obj = ThreadMessages.WIFI_SCAN_COMPLETE;
		_handler.sendMessage(msg);
    }
  }

}