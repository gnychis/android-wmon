package com.gnychis.coexisyst;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CoexiSyst extends Activity implements OnClickListener {
	
	private static final String TAG = "WiFiDemo";

	// Make instances of our helper classes
	WifiManager wifi;
	BroadcastReceiver receiver;
	DBAdapter db;
	//SelectNetDev snetdev;
	
	TextView textStatus;
	Button buttonScan; 
	//Button buttonManageNets; 
	Button buttonManageDevs;
	
	// Network and Device lists
	List<ScanResult> netlist_80211;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Setup the database
    	db = new DBAdapter(this);
    	db.open();
      
		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		buttonScan = (Button) findViewById(R.id.buttonAdd80211);
		//buttonManageNets = (Button) findViewById(R.id.buttonManageNets);
		buttonManageDevs = (Button) findViewById(R.id.buttonManageDevs);
		buttonScan.setOnClickListener(this);
//		buttonManageNets.setOnClickListener(this);
		buttonManageDevs.setOnClickListener(this);

		// Setup WiFi
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		// Register Broadcast Receiver
		if (receiver == null)
			receiver = new WiFiScanReceiver(this);

		Log.d(TAG, "onCreate()");
		startScans();
    }
    
	@Override
	public void onStop() {
		super.onStop();
		stopScans();
		//db.close();
	}
	
	public void onResume() {
		super.onResume();
		startScans();
		//db.open();
	}
	
	public void startScans() {
		registerReceiver(receiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));	
	}
	
	public void stopScans() {
		unregisterReceiver(receiver);	
	}
	
	public void clickAdd80211() {
		
		// Bail ship if there is no 802.11 networks within range, there is nothing to do!
		if(netlist_80211 == null || netlist_80211.size() == 0) {
			Toast.makeText(this, "No 802.11 networks in range...",
					Toast.LENGTH_LONG).show();					
			return;
		}
		
		stopScans();	// Make sure that this is atomic, networks are not changing
		
		// Create a string representation of the network names and present them to the user
		final String[] netl80211 = netlts_80211();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select your 802.11 network");
		builder.setItems(netl80211, new DialogInterface.OnClickListener() {
			
			// Wait for a user to click on an item in the list
		    public void onClick(DialogInterface dialog, int item) {
		    			    	
		    	// First, do a lookup on the table to see if it exists
		    	ScanResult sr = netlist_80211.get(item);
		    	int managed = db.getNetwork(sr.BSSID, sr.SSID);
		    	
		    	// TODO: add the ability to remanage the master by adding the network
		    	
		    	if(managed!=-1) {	// Cannot add a network that is already managed
		    		Toast.makeText(getApplicationContext(), sr.SSID + " is already managed.", Toast.LENGTH_SHORT).show();
		    	} else {		// Add the network the list of managed networks 
		    		
		    		long res = db.insertNetwork(null, sr.BSSID, sr.SSID, DBAdapter.PTYPE_80211, 0);
		    		if(res == -1) {
		    			Toast.makeText(getApplicationContext(), "Error inserting " + sr.SSID + " in to the database.", Toast.LENGTH_SHORT).show();
		    		} else {
		    			Toast.makeText(getApplicationContext(), "CoexiSyst is now managing " + sr.SSID, Toast.LENGTH_SHORT).show();
		    			// Since we successfully added the network, let's add the access point as a device
		    			int netid = db.getNetwork(sr.BSSID, sr.SSID);
		    			if(db.insertNetDev(netid, sr.BSSID, "Access Point", DBAdapter.PTYPE_80211, 0)==-1) {
		    				Toast.makeText(getApplicationContext(), "Error inserting access point", Toast.LENGTH_SHORT).show();
		    			}
		    		}
		    	}
		        
		        startScans();	// We can start scanning again
		    }
		});

		AlertDialog alert = builder.create();
		alert.show();
		
		
	}
	
	public void getUserText() {
	
	}
	
	public void clickManageNets() {
		
	}
	
	public void clickManageDevs() {
		Log.d(TAG,"Trying to load manage devices window");
        Intent i = new Intent(CoexiSyst.this, ManageDevices.class);
        startActivity(i);
	}

	public void onClick(View view) {
		
		Log.d(TAG,"Got a click");
		
		if (view.getId() == R.id.buttonAdd80211) {
			clickAdd80211();
		}
		//if(view.getId() == R.id.buttonManageNets) {
		//	clickManageNets();
		//}
		if(view.getId() == R.id.buttonManageDevs) {
			clickManageDevs();
		}
	}
	
	public String[] netlts_80211() {
		int i=0;
		String curr;
		String[] nets_str = new String[netlist_80211.size()];
		for(ScanResult result : netlist_80211) {
	      curr = String.format("%s (%d dBm)", result.SSID, result.level);
	      nets_str[i] = curr;
	      i++;
		}
		return nets_str;
	}
	
	/* 
	 	AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Title");
		alert.setMessage("Message");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();
		  // Do something with value!
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});

		alert.show();
		*/
}