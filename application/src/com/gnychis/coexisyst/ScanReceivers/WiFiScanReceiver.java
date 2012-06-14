package com.gnychis.coexisyst.ScanReceivers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;
import com.gnychis.coexisyst.Core.Packet;
import com.gnychis.coexisyst.DeviceHandlers.Wifi;
import com.gnychis.coexisyst.NetDevDefinitions.WifiAP;

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
		if(((WifiAP)arg0).rssi() < ((WifiAP)arg1).rssi())
			return 1;
		else if( ((WifiAP)arg0).rssi() > ((WifiAP)arg1).rssi())
			return -1;
		else
			return 0;
	}
  };
  
  // The purpose of this function is to search through the already discovered
  // APs, and determine if the newly scanned "AP" is simply another band on
  // a single physical AP.  If so, we merge them into a single dualband AP.
  public boolean mergeDualband(ArrayList<WifiAP> aps_in_list, WifiAP ap) {
	  
	  // The hashtable is indexed by MAC addresses.  We check to see if there
	  // is an AP with an adjacent MAC address on the opposite band (i.e., 2.4/5GHz).
	  BigInteger orig_ap_addr = Wifi.parseMacStringToBigInteger(ap._mac);
	  
	  // Compare with each access point
	  Iterator<WifiAP> aps = aps_in_list.iterator();
	  while(aps.hasNext()) {
		  WifiAP curr_ap = aps.next();
		  BigInteger this_ap_addr = Wifi.parseMacStringToBigInteger(curr_ap._mac);
		  BigInteger difference = orig_ap_addr.subtract(this_ap_addr);
		  
		  // The addresses are apart by one, save this access point information to the
		  // current access point.  Since we scan 2.4GHz before 5GHz, we are guaranteed
		  // to have 2.4GHz before 5GHz in the list of bands.
		  if(difference.equals(new BigInteger("-1")) || difference.equals(new BigInteger("1"))) {
			  
			  // Also, they have to be on separate bands (2.4GHz vs 5GHz)
			  if(curr_ap._band<5000 && ap._band > 5000) {
				  curr_ap._mac2 = ap._mac;
				  curr_ap._band2 = ap._band;
				  curr_ap._dualband = true;
				  return true;
			  }
		  }
	  }
	  return false;
  }

  @Override @SuppressWarnings("unchecked")
  public void onReceive(Context c, Intent intent) {
	   
	Log.d(TAG, "Received incoming scan complete message");
	  
	// The raw pcap packets from the scan result, for parsing
	ArrayList<Packet> scan_result = (ArrayList<Packet>) intent.getExtras().get("packets");
	  
    // For keeping track of the APs that we have already parsed, by MAC
    Hashtable<String,WifiAP> aps_in_list = new Hashtable<String,WifiAP>();
    
    // To return, a list of WifiAPs
    ArrayList<WifiAP> parsed_result = new ArrayList<WifiAP>();
    
    // Go through each scan result, and get the access point information
    Iterator<Packet> results = scan_result.iterator();
    while(results.hasNext()) {
    	Packet pkt = results.next();
    	WifiAP ap = new WifiAP();
    	int rssi = Integer.parseInt(pkt.getField("radiotap.dbm_antsignal"));
    	
    	// If it's a bad packet, ignore
    	if(pkt.getField("radiotap.flags.badfcs").equals("1"))
    		continue;
    	
    	// Kind of like caching the important stuff to be readily accessible
    	// tag 3 is the channel, and it's not reliable to use radiotap.channel.freq, since
    	// it is possible to capture a beacon on an adjacent channel to the actual channel
    	// that the AP is on.
    	String channel_s = pkt.getField("wlan_mgt.ds.current_channel");
    	if(channel_s!=null)
    		ap._band = Wifi.channelToFreq(Integer.parseInt(channel_s));  
    	else 
    		ap._band = Integer.parseInt(pkt.getField("radiotap.channel.freq"));
    	
    	ap._mac = pkt.getField("wlan.sa");
    	ap._ssid = pkt.getField("wlan_mgt.ssid");
    	ap._rssis.add(rssi);
    	ap._beacon = pkt;
    	
    	// Keep the AP if we don't already have a record for it (a single scan
    	// might catch multiple beacons from the AP).
    	if(!aps_in_list.containsKey(ap._mac)) {
    		aps_in_list.put(ap._mac, ap);  // mark that we've seen it
    		
    		// Before we add it to our list as a new AP, see if we can merge
    		// it with another AP as a dual-band access point (one network).
    		if(!mergeDualband(parsed_result, ap))
    			parsed_result.add(ap);
    	} else {  // we already have it, but we can add multiple RSSI readings
    		WifiAP tap = aps_in_list.get(ap._mac);
    		tap._rssis.add(rssi);
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