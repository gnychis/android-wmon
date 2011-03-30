package com.gnychis.coexisyst;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
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
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CoexiSyst extends Activity implements OnClickListener {
	
	private static final String TAG = "WiFiDemo";

	// For root
	Process proc;
	DataOutputStream os;


	// Make instances of our helper classes
	DBAdapter db;
	WifiManager wifi;
	BluetoothAdapter bt;
	protected USBMon usbmon;
	protected WiSpyScan wispyscan;
	
	// Receivers
	BroadcastReceiver rcvr_80211;
	BroadcastReceiver rcvr_BTooth;
	
	TextView textStatus;
	
	Button buttonAddNetwork; 
	Button buttonManageNets; 
	Button buttonManageDevs;
	Button buttonScanSpectrum;
	Button buttonViewSpectrum;
	Button buttonADB;
	
	// Network and Device lists
	ArrayList<ScanResult> netlist_80211;
	
	// USB device related
	Wispy wispy;
	IChart wispyGraph;
	
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
        } catch(Exception e) {
        	Log.e(TAG, "failure gaining root access", e);
			Toast.makeText(this, "Failure gaining root access...",
					Toast.LENGTH_LONG).show();		
        }
    	
    	// Load the libusb related libraries
    	try {
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("wispy");
    		System.loadLibrary("coexisyst");
    	} catch (Exception e) {
    		Log.e(TAG, "error trying to load a USB related library", e);
    	}
       
    	wispy = new Wispy();
        
        // Setup the database
    	db = new DBAdapter(this);
    	db.open();
      
		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		textStatus.setText("");
		buttonAddNetwork = (Button) findViewById(R.id.buttonAddNetwork); buttonAddNetwork.setOnClickListener(this);
		buttonViewSpectrum = (Button) findViewById(R.id.buttonViewSpectrum); buttonViewSpectrum.setOnClickListener(this);
		//buttonManageNets = (Button) findViewById(R.id.buttonManageNets); buttonManageNets.setOnClickListener(this);
		buttonManageDevs = (Button) findViewById(R.id.buttonManageDevs); buttonManageDevs.setOnClickListener(this);
		buttonADB = (Button) findViewById(R.id.buttonAdb); buttonADB.setOnClickListener(this);
		buttonScanSpectrum = (Button) findViewById(R.id.buttonScan); buttonScanSpectrum.setOnClickListener(this);
		
		wispyGraph = new GraphWispy();

		// Setup wireless devices
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		bt = BluetoothAdapter.getDefaultAdapter();

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
			
		// Start the USB monitor thread, but only instantiate the wispy scan
		wispyscan = new WiSpyScan();
		usbmon = new USBMon();
		usbmon.execute (this);
		
		Log.d(TAG, "onCreate()");
		stopScans();
    }
    
	@Override
	public void onStop() { super.onStop();  }
	public void onResume() { super.onResume();  }
	public void onPause() { super.onPause();  }
	public void onDestroy() { super.onDestroy();  }
	
	public void scanSpectrum() {
		// Disable interfaces first, and get the raw power in the spectrum from WiSpy
		wifi.setWifiEnabled(false);
		bt.disable();
		
		// Get the WiSpy data

		
	}
	
	public void startScans() {
		try {
		
			// Enable interfaces
			bt.enable();
			wifi.setWifiEnabled(true);
			
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
		
		// Disable interfaces
		wifi.setWifiEnabled(false);
		bt.disable();
		
		// Need to disable wispy also?
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
			i.putExtra("com.gnychis.coexisyst.results", wispy._maxresults);
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
		if(view.getId() == R.id.buttonScan) {
			scanSpectrum();
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
	
	// A class to handle USB worker like things
	protected class WiSpyScan extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;	
		
		@Override
		protected String doInBackground( Context... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			
			//publishProgress(CoexiSyst.WISPY_POLL_THREAD);
			
			if(initWiSpyDevices()==1) {
				publishProgress(Wispy.WISPY_POLL);
			} else {
				publishProgress(Wispy.WISPY_POLL_FAIL);
				wispy._is_polling = false;
				return "FAIL";
			}
			
			while(true) {
				int[] scan_res = pollWiSpy();
				
				// If main thread is signaling to reset the max results
				if(wispy._reset_max) {
					wispy._poll_count=0;
					wispy._reset_max=false;
					for(int i=0; i<256; i++)
			        	wispy._maxresults[i]=-200;
				}
				
				if(scan_res==null) {
					publishProgress(Wispy.WISPY_POLL_FAIL);
					wispy._is_polling = false;
					break;
				}
				
				//publishProgress(CoexiSyst.WISPY_POLL);		
				
				// What to do once we get a response!
				if(scan_res.length==256 && wispy._save_scans) {
					for(int i=0; i<scan_res.length; i++)
						if(scan_res[i] > wispy._maxresults[i]) 
							wispy._maxresults[i] = scan_res[i];
					
					wispy._poll_count++;
					try {	
						if(false) {
							for(int i=0; i<scan_res.length; i++) {
								wispy._wispyPrint.print(scan_res[i]);
								wispy._wispyPrint.print(" ");
							}
							wispy._wispyPrint.print("\n");
							wispy._wispyPrint.flush();
							wispy._wispyOut.flush();
							//Log.d(TAG, "got new results");
						}
					} catch(Exception e) {
						Log.e(TAG, "error writing to SD card", e);
					}
				}
			}
			
			return "OK";
		}
		
		@Override
		protected void onProgressUpdate(Integer... values)
		{
			super.onProgressUpdate(values);
			int event = values[0];
			
			if(event==Wispy.WISPY_POLL_THREAD) {
				//Toast.makeText(parent, "In WiSpy poll thread...",
				//		Toast.LENGTH_LONG).show();
			}
			else if(event==Wispy.WISPY_POLL) {
				//Toast.makeText(parent, "WiSpy started polling...",
				//		Toast.LENGTH_LONG).show();
				//textStatus.append(".");
			}
			else if(event==Wispy.WISPY_POLL_FAIL) {
				Toast.makeText(parent, "--- WiSpy poll failed ---",
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
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
					
					int wispy_in_devlist=USBcheckForDevice(0x1781, 0x083f);
					
					if(wispy_in_devlist==1 && wispy._device_connected==false) {
						publishProgress(Wispy.WISPY_CONNECT);
					} else if(wispy_in_devlist==0 && wispy._device_connected==true) {
						publishProgress(Wispy.WISPY_DISCONNECT);
					} else if(wispy_in_devlist==1 && wispy._device_connected==true && wispy._is_polling==false) {
						//Log.d(TAG, "determined that a re-poll is needed");
						//Thread.sleep( 1000 );
						//publishProgress(CoexiSyst.WISPY_POLL);
					}
					
					Thread.sleep( 2000 );
					//Log.d(TAG, "checking for USB devices");

				} catch (Exception e) {
					
					Log.e(TAG, "exception trying to sleep", e);
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(Integer... values)
		{
			super.onProgressUpdate(values);
			int event = values[0];
			
			if(event == Wispy.WISPY_CONNECT) {
				Log.d(TAG, "got update that WiSpy was connected");
				Toast.makeText(parent, "WiSpy device connected",
						Toast.LENGTH_LONG).show();	
				wispy._device_connected=true;
				
				// List the wispy devices
				coexisyst.textStatus.append("\n\nWiSpy Devices:\n");
				String devices[] = getWiSpyList();
				for (int i=0; i<devices.length; i++)
					textStatus.append(devices[i] + "\n");
				
				// Start the poll thread now
				coexisyst.wispyscan.execute(coexisyst);
				wispy._is_polling = true;

			}
			else if(event == Wispy.WISPY_DISCONNECT) {
				Log.d(TAG, "got update that WiSpy was connected");
				Toast.makeText(parent, "WiSpy device has been disconnected",
						Toast.LENGTH_LONG).show();
				wispy._device_connected=false;
				coexisyst.wispyscan.cancel(true);  // make sure to stop polling thread
			}
			else if(event == Wispy.WISPY_POLL) {
				Log.d(TAG, "trying to re-poll the WiSpy device");
				Toast.makeText(parent, "Re-trying polling",
						Toast.LENGTH_LONG).show();
				coexisyst.wispyscan.cancel(true);
				coexisyst.wispyscan = new WiSpyScan();
				coexisyst.wispyscan.execute(coexisyst);
				wispy._is_polling = true;
			}
		}
	}
}