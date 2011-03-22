package com.gnychis.coexisyst;

import java.io.DataOutputStream;
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
	public static final int WISPY_POLL = 2;

	// For root
	Process proc;
	DataOutputStream os;

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
	Button buttonViewSpectrum;
	Button buttonADB;
	
	// Network and Device lists
	ArrayList<ScanResult> netlist_80211;
	
	// USB device related
	boolean wispy_connected;
	boolean wispy_polling;
	IChart wispyGraph;
	int maxresults[];
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Request root
        try {
        	proc = Runtime.getRuntime().exec("su");
        	os = new DataOutputStream(proc.getOutputStream());  
        	os.writeBytes("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system\n");
        	os.writeBytes("mount -t usbfs -o devmode=0666 none /proc/bus/usb\n");
        	//os.writeBytes("setprop service.adb.tcp.port 5555\n");
        	//os.writeBytes("stop adbd");
        	//os.writeBytes("start adbd");
        } catch(Exception e) {
        	Log.e(TAG, "failure gaining root access", e);
			Toast.makeText(this, "Failure gaining root access...",
					Toast.LENGTH_LONG).show();		
        }
        
        // USB device initialization
        wispy_connected=false;
        wispy_polling=false;
        maxresults = new int[256];
        for(int i=0; i<256; i++)
        	maxresults[i]=-200;
        
        // Setup the database
    	db = new DBAdapter(this);
    	db.open();
    	
    	// Load the libusb related libraries
    	try {
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("wispy");
    		System.loadLibrary("coexisyst");
    	} catch (Exception e) {
    		Log.e(TAG, "error trying to load a USB related library", e);
    	}
      
		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		textStatus.setText("");
		buttonScan = (Button) findViewById(R.id.buttonAddNetwork);
		buttonViewSpectrum = (Button) findViewById(R.id.buttonViewSpectrum);
		//buttonManageNets = (Button) findViewById(R.id.buttonManageNets);
		buttonManageDevs = (Button) findViewById(R.id.buttonManageDevs);
		buttonADB = (Button) findViewById(R.id.buttonAdb);
		buttonScan.setOnClickListener(this);
//		buttonManageNets.setOnClickListener(this);
		buttonManageDevs.setOnClickListener(this);
		buttonViewSpectrum.setOnClickListener(this);
		buttonADB.setOnClickListener(this);
		
		wispyGraph = new GraphWispy();

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
	
	public void clickViewSpectrum() {
		try {
			Intent i = null;
			usbmon.cancel(true);
			i = wispyGraph.execute(this);
			i.putExtra("com.gnychis.coexisyst.results", maxresults);
			startActivity(i);
		} catch(Exception e) {
			Log.e(TAG, "error trying to load spectrum view", e);
		}
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
		if(view.getId() == R.id.buttonViewSpectrum) {
			clickViewSpectrum();
		}
		if(view.getId() == R.id.buttonAdb) {
			try {
				os.writeBytes("setprop service.adb.tcp.port 5555\n");
				os.writeBytes("stop adbd\n");
				os.writeBytes("start adbd\n");
			} catch(Exception e) {
				Log.e(TAG, "failured to switch ADB to TCP", e);
			}
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
	public native String[] getWiSpyList();
	public native int initWiSpyDevices();
	public native int[] pollWiSpy();
	//public native int pollWiSpy();
	
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
						Thread.sleep( 2000 );
					}
					if(USBcheckForDevice(0x1781, 0x083f)==0 && coexisyst.wispy_connected==true) {
						publishProgress(CoexiSyst.WISPY_DISCONNECT);
						Thread.sleep( 2000 );
					}
					if(coexisyst.wispy_connected==true && coexisyst.wispy_polling==true) {
						publishProgress(CoexiSyst.WISPY_POLL);
					}
					
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
				
				// List the wispy devices
				coexisyst.textStatus.append("\n\nWiSpy Devices:\n");
				String devices[] = getWiSpyList();
				for (int i=0; i<devices.length; i++)
					textStatus.append(devices[i] + "\n");
				
				// Init the wispy devices
				if(initWiSpyDevices()==1) {
					coexisyst.textStatus.append("... initialized devices\n");
				} else {
					coexisyst.textStatus.append("... failed to initialize devices\n");
				}
				

				if(pollWiSpy()!=null) {
					Toast.makeText(parent, "WiSpy poll successful",
							Toast.LENGTH_SHORT).show();
					coexisyst.wispy_polling = true;
				} else {
					Toast.makeText(parent, "WiSpy poll was not successful",
							Toast.LENGTH_SHORT).show();
					coexisyst.wispy_polling = false;
				}
			}
			else if(event == CoexiSyst.WISPY_DISCONNECT) {
				Log.d(TAG, "got update that WiSpy was connected");
				Toast.makeText(parent, "WiSpy device has been disconnected",
						Toast.LENGTH_LONG).show();
				coexisyst.wispy_connected=false;
			}
			else if(event == CoexiSyst.WISPY_POLL){
				int[] res = pollWiSpy();
				//int res = pollWiSpy();
				if(res!=null) {
					coexisyst.wispy_polling = true;
					
					// What to do once we get a response!
					if(res.length==256) {
						for(int i=0; i<res.length; i++)
							if(res[i] > maxresults[i])
								maxresults[i] = res[i];
						//Log.d(TAG, "have a set of results!");
					}
				} else {
					coexisyst.wispy_polling = false;
				}				
			}
		}
	}
}