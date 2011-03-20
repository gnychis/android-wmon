package com.gnychis.coexisyst;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CoexiSyst extends Activity implements OnClickListener {
	
	private static final String TAG = "WiFiDemo";
	public static final int WISPY_CONNECT = 0;
	public static final int WISPY_DISCONNECT = 1;

	// Make instances of our helper classes
	DBAdapter db;
	WifiManager wifi;
	BluetoothAdapter bt;
	protected USBMon usbmon;
	
	// Receivers
	BroadcastReceiver rcvr_80211;
	BroadcastReceiver rcvr_BTooth;
	
	TextView textStatus;
	Button buttonScan; 
	//Button buttonManageNets; 
	Button buttonManageDevs;
	
	// Network and Device lists
	ArrayList<ScanResult> netlist_80211;
	
	// USB device related
	boolean wispy_connected;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // USB device initialization
        wispy_connected=false;
        
        // Setup the database
    	db = new DBAdapter(this);
    	db.open();
    	
    	// Load the libusb related libraries
    	try {
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("coexisyst");
    	} catch (Exception e) {
    		Log.e(TAG, "error trying to load a USB related library", e);
    	}
      
		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		textStatus.setText("");
		buttonScan = (Button) findViewById(R.id.buttonAddNetwork);
		//buttonManageNets = (Button) findViewById(R.id.buttonManageNets);
		buttonManageDevs = (Button) findViewById(R.id.buttonManageDevs);
		buttonScan.setOnClickListener(this);
//		buttonManageNets.setOnClickListener(this);
		buttonManageDevs.setOnClickListener(this);

		// Setup wireless devices
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		bt = BluetoothAdapter.getDefaultAdapter();
		
		if (!bt.isEnabled()) {
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, 1);
		}

		// Register Broadcast Receiver
		if (rcvr_80211 == null)
			rcvr_80211 = new WiFiScanReceiver(this);
		if (rcvr_BTooth == null)
			rcvr_BTooth = new BluetoothManager(this);

		textStatus.append(initUSB());
		
		// Print out the USB device names
		textStatus.append("\n\nUSB Devices:\n");
		String devices[] = getDeviceNames();
		for (int i=0; i<devices.length; i++)
			textStatus.append("\t* " + devices[i] + "\n");
			
		usbmon = new USBMon();
		usbmon.execute (this);
		Log.d(TAG, "onCreate()");
		startScans();
    }
    
	@Override
	public void onStop() {
		super.onStop();
		stopScans();
	}
	
	public void onResume() {
		super.onResume();
		startScans();
	}
	
	public void onPause() {
		super.onPause();
		stopScans();
	}
	public void onDestroy() {
		super.onDestroy();
		stopScans();
	}
	
	public void startScans() {
		try {
		registerReceiver(rcvr_80211, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));	
		registerReceiver(rcvr_BTooth, new IntentFilter(
				BluetoothDevice.ACTION_FOUND));
		
		wifi.startScan();
		bt.startDiscovery();
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to register scan receivers");
		}
	}
	
	public void stopScans() {
		try {
		unregisterReceiver(rcvr_80211);	
		unregisterReceiver(rcvr_BTooth);
		
		bt.cancelDiscovery();
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to unregister scan receivers",e);
		}
	}
	
	public void clickAddNetwork() {
		
		// Bail ship if there is no 802.11 networks within range, there is nothing to do!
		if(netlist_80211 == null || netlist_80211.size() == 0) {
			Toast.makeText(this, "No 802.11 networks in range...",
					Toast.LENGTH_LONG).show();					
			return;
		}
		
		stopScans();	// Make sure that this is atomic, networks are not changing

		try {
			Log.d(TAG,"Trying to load add networks window");
			Intent i = new Intent(CoexiSyst.this, AddNetwork.class);
			
			//ArrayList<ScanResult> n80211 = new ArrayList(netlist_80211);
			i.putExtra("com.gnychis.coexisyst.80211", netlist_80211);
			
			startActivity(i);
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to load network add window",e);
			return;
		}
        
        startScans();	// We can start scanning again
        
	}
	
	public void getUserText() {
	
	}
	
	public void clickManageNets() {
		
	}
	
	public void clickManageDevs() {
		Log.d(TAG,"Trying to load manage networks window");
        Intent i = new Intent(CoexiSyst.this, ManageNetworks.class);
        startActivity(i);
	}

	public void onClick(View view) {
		
		Log.d(TAG,"Got a click");
		
		if (view.getId() == R.id.buttonAddNetwork) {
			clickAddNetwork();
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
	
	public native String  initUSB();
	public native String[] getDeviceNames();
	public native int getWiSpy();
	public native int USBcheckForDevice(int vid, int pid);
	
	// A class to handle USB worker like things
	protected class USBMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		
		@Override
		protected String doInBackground( Context... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			while(true) {
				try {
					
					if(USBcheckForDevice(0x1781, 0x083f)==1 && coexisyst.wispy_connected==false) {
						publishProgress(CoexiSyst.WISPY_CONNECT);
					}
					if(USBcheckForDevice(0x1781, 0x083f)==0 && coexisyst.wispy_connected==true) {
						publishProgress(CoexiSyst.WISPY_DISCONNECT);
					}
					
					Thread.sleep( 2000 );
					Log.d(TAG, "in background USB thread");
				} catch (Exception e) {
					
					Log.e(TAG, "exception trying to sleep", e);
				}
			}
		}
		
		@Override
		protected void onProgressUpdate(Integer... values)
		{
			super.onProgressUpdate(values);
			int event = values[0];
			
			if(event == CoexiSyst.WISPY_CONNECT) {
				Log.d(TAG, "got update that WiSpy was connected");
				Toast.makeText(parent, "WiSpy device connected",
						Toast.LENGTH_LONG).show();	
				coexisyst.wispy_connected=true;
			}
			else if(event == CoexiSyst.WISPY_DISCONNECT) {
				Log.d(TAG, "got update that WiSpy was connected");
				Toast.makeText(parent, "WiSpy device has been disconnected",
						Toast.LENGTH_LONG).show();
				coexisyst.wispy_connected=false;
			}
		}
	}
}