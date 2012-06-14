package com.gnychis.coexisyst.ScanReceivers;

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
import com.gnychis.coexisyst.NetDevDefinitions.ZigBeeDev;
import com.gnychis.coexisyst.NetDevDefinitions.ZigBeeNetwork;

// Can pass a handler that will perform a callback when a scan
// is received.  This is helpful for alerting the parent class
// of the incoming scan.
public class ZigBeeScanReceiver extends BroadcastReceiver {
  private static final String TAG = "ZigBeeScanReceiver";
  public String nets_str[];
  private Handler _handler;
  public ArrayList<ZigBeeNetwork> _last_scan;

  // If the handler is not null, callbacks will be made
  public ZigBeeScanReceiver(Handler h) {
    super();
    _handler = h;
  }
  
  public String[] get_nets() {
	  return nets_str;
  }
  
  Comparator<Object> comp = new Comparator<Object>() {
	public int compare(Object arg0, Object arg1) {
		if(((ZigBeeNetwork)arg0).lqi() < ((ZigBeeNetwork)arg1).lqi())
			return 1;
		else if( ((ZigBeeNetwork)arg0).lqi() > ((ZigBeeNetwork)arg1).lqi())
			return -1;
		else
			return 0;
	}
  };
  
  @Override @SuppressWarnings("unchecked")
  public void onReceive(Context c, Intent intent) {
	   
	Log.d(TAG, "Received incoming scan complete message");
	  
	// The raw pcap packets from the scan result, for parsing
	ArrayList<Packet> scan_result = (ArrayList<Packet>) intent.getExtras().get("packets");
	  
    // For keeping track of the APs that we have already parsed, by MAC
    Hashtable<String,ZigBeeDev> devs_in_list = new Hashtable<String,ZigBeeDev>();
    Hashtable<String,ZigBeeNetwork> networks_in_list = new Hashtable<String,ZigBeeNetwork>();
    
    // To return, a list of WifiAPs
    ArrayList<ZigBeeDev> parsed_devices = new ArrayList<ZigBeeDev>();
    ArrayList<ZigBeeNetwork> parsed_networks = new ArrayList<ZigBeeNetwork>();
    
    // Go through each scan result, and get the access point information
    Iterator<Packet> results = scan_result.iterator();
    while(results.hasNext()) {
    	Packet pkt = results.next();
    	ZigBeeDev dev = new ZigBeeDev();
    	
    	// If it's a bad packet, ignore
    	if(pkt.getField("wpan.fcs_ok").equals("0"))
    		continue;    	
    	
    	dev._mac = pkt.getField("wpan.src16");
    	dev._pan = pkt.getField("wpan.src_pan");
    	dev._lqis.add(pkt._lqi);
    	dev._beacon = pkt;
    	dev._band = pkt._band;
    	
    	// Keep the AP if we don't already have a record for it (a single scan
    	// might catch multiple beacons from the AP).
    	if(!devs_in_list.containsKey(dev._mac)) {
    		devs_in_list.put(dev._mac, dev);  // mark that we've seen it
    		parsed_devices.add(dev);
    	} else {  // we already have it, but we can add multiple RSSI readings
    		ZigBeeDev tdev = devs_in_list.get(dev._mac);
    		tdev._lqis.add(pkt._lqi);
    	}
    }
    
    // Now, go through all of the devices and create networks
    Iterator<ZigBeeDev> devs = parsed_devices.iterator();
    while(devs.hasNext()) {
    	ZigBeeDev dev = devs.next();
    	
    	// First, see if it is a new network
    	if(!networks_in_list.containsKey(dev._pan)) {
    		ZigBeeNetwork net = new ZigBeeNetwork();
    		net._mac = "0x0000";
    		net._pan = dev._pan;
    		net._band = dev._band;
    		net._lqis.addAll(dev._lqis);
    		net._devices.add(dev);
    		networks_in_list.put(net._pan, net);
    		parsed_networks.add(net);
    	} else { // it is not a new network
    		ZigBeeNetwork net = networks_in_list.get(dev._pan);
    		net._devices.add(dev);
    		net._lqis.addAll(dev._lqis);
    	}
    }
    
    // Save this scan as our most current scan
    Collections.sort(parsed_networks, comp);
    _last_scan = parsed_networks;
    
    if(_handler != null) {
		// Send a message to stop the spinner if it is running
		Message msg = new Message();
		msg.obj = ThreadMessages.ZIGBEE_SCAN_COMPLETE;
		_handler.sendMessage(msg);
    }
  }

}