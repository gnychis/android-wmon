package com.gnychis.coexisyst;

import java.util.Collections;
import java.util.Comparator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

public class WiFiScanReceiver extends BroadcastReceiver {
  private static final String TAG = "WiFiScanReceiver";
  CoexiSyst coexisyst;
  int scans;
  public String nets_str[];
  

  public WiFiScanReceiver(CoexiSyst coexisyst) {
    super();
    scans = 0;
    this.coexisyst = coexisyst;
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
    scans++;
	
	// Pull the results in
    coexisyst.netlist_80211 = coexisyst.wifi.getScanResults();
    Collections.sort(coexisyst.netlist_80211, comp);
    
    String ts = String.format("Results (%d)\n", scans);
    coexisyst.textStatus.setText(ts);

    for (ScanResult result : coexisyst.netlist_80211) {
      if (bestSignal == null
          || WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0)
        bestSignal = result;
      String curr = String.format("(%d) %s, %s MHz, %d dBm, %s\n", i, result.SSID, result.frequency, result.level, result.BSSID);
      coexisyst.textStatus.append(curr);
      i++;
    }

    if(bestSignal != null) {
	    String message = String.format("%s networks found. %s is the strongest with %d dBm.",
	    		coexisyst.netlist_80211.size(), bestSignal.SSID, bestSignal.level);
	    Toast.makeText(coexisyst, message, Toast.LENGTH_LONG).show();
	
	    Log.d(TAG, "onReceive() message: " + message);
    }
  }

}