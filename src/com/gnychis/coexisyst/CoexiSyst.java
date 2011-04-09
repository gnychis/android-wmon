package com.gnychis.coexisyst;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
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
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CoexiSyst extends Activity implements OnClickListener {
	
	private static final String TAG = "WiFiDemo";
	
	SubSystem system;

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
	AtherosDev ath;
	IChart wispyGraph;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        system = new SubSystem(this);
    	system.cmd("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system\n");
    	system.cmd("mount -t usbfs -o devmode=0666 none /proc/bus/usb\n");
    	system.cmd("mkdir /data/data/com.gnychis.coexisyst/bin\n");
    	
    	system.install_bin("iwconfig", R.raw.iwconfig);
    	system.install_bin("lsusb", R.raw.lsusb);
    	system.install_bin("zd_firmware.zip", R.raw.zd_firmware);
    	system.install_bin("pcapd", R.raw.pcapd);
    	
    	// Load the libusb related libraries
    	try {
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("wispy");
    		System.loadLibrary("pcap");
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
		ath = new AtherosDev(this);
		
		// Check the pcap interfaces
		//pcapGetInterfaces();
		
		Log.d(TAG, "onCreate()");
		//stopScans();
		//startScans();
    }
    
	@Override
	public void onStop() { super.onStop(); Log.d(TAG, "onStop()");}
	public void onResume() { super.onResume(); Log.d(TAG, "onResume()");
		if(usbmon == null || usbmon.getStatus()!=Status.RUNNING){ 
			Log.d(TAG, "resuming a USB monitoring thread");
			usbmon = new USBMon();
			usbmon.execute (this);
		} else {
			Log.d(TAG, "not resuming USB monitoring, already running?");
		}
		//startScans();
	}
	public void onPause() { super.onPause(); Log.d(TAG, "onPause()"); }
	public void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy()"); }
	
	public void scanSpectrum() {
		// Disable interfaces first, and get the raw power in the spectrum from WiSpy
		stopScans();
		
		// Get the WiSpy data
		Log.d(TAG, "Waiting for results from WiSpy...");
		wispy.getResultsBlock(Wispy.PASSES);
		Log.d(TAG, "Got results from the WiSpy");
		
		startScans();
		
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
			
			if(wispyscan.getStatus()==Status.RUNNING) {
				wispyscan.cancel(true);
				wispy._is_polling=false;
				Log.d(TAG, "canceling wispy scan");
			}
			
			if(usbmon.getStatus()==Status.RUNNING) {
				if(usbmon.cancel(true))
					Log.d(TAG, "canceled USB monitor");
				else
					Log.d(TAG, "error trying to cancel USB monitor");	
				usbmon = null;
			}
			
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
				system.cmd("setprop service.adb.tcp.port 5555\n");
				system.cmd("stop adbd\n");
				system.cmd("start adbd\n");
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
	public native int pcapGetInterfaces();
	
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
				
				if(scan_res==null) {
					publishProgress(Wispy.WISPY_POLL_FAIL);
					wispy._is_polling = false;
					break;
				}
				
				//publishProgress(CoexiSyst.WISPY_POLL);		
				
				// What to do once we get a response!
				try {
					wispy._lock.acquire();
					if(scan_res.length==256 && wispy._save_scans) {
						for(int i=0; i<scan_res.length; i++)
							if(scan_res[i] > wispy._maxresults[i]) 
								wispy._maxresults[i] = scan_res[i];
						
						wispy._poll_count++;
						Log.d("wispy_thread", "saved result from wispy thread");
					}
					wispy._lock.release();
				} catch (Exception e) {
					Log.e(TAG, "exception trying to claim lock to save new results",e);
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
	
	protected class WifiMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		SubSystem wifi_subsystem;
		Socket skt;
		private int PCAPD_WIFI_PORT = 2000;
		BufferedReader skt_in;
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			Log.d(TAG, "a new Wifi monitor thread was started");
			
			// Attempt to create capture process spawned in the background
			// which we will connect to for pcap information.
			coexisyst.system.local_cmd("pcapd wlan0 " + Integer.toString(PCAPD_WIFI_PORT) + " &");
			try { Thread.sleep(100); } catch (Exception e) {} // give some time for the process
			
			// Attempt to connect to the socket via TCP for the PCAP info
			try {
				skt = new Socket("localhost", PCAPD_WIFI_PORT);
			} catch(Exception e) {
				Log.e(TAG, "exception trying to connect to wifi socket for pcap", e);
			}
			
			try {
				skt_in = new BufferedReader(new InputStreamReader(skt.getInputStream()));
			} catch(Exception e) {
				Log.e(TAG, "exception trying to get inputbuffer from socket stream");
			}
			
			//skt_in.read
			
			return "OK";
		}
	}
	
	// A class to handle USB worker like things
	protected class USBMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		
		@Override
		protected void onCancelled()
		{
			Log.d(TAG, "USB monitor thread successfully canceled");
		}
		
		@Override
		protected String doInBackground( Context... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			Log.d(TAG, "a new USB monitor was started");
			while(true) {
				try {
					
					int wispy_in_devlist=USBcheckForDevice(0x1781, 0x083f);
					int atheros_in_devlist=USBcheckForDevice(0x083a,0x4505);
					
					// Wispy related checks
					if(wispy_in_devlist==1 && wispy._device_connected==false) {
						publishProgress(Wispy.WISPY_CONNECT);
					} else if(wispy_in_devlist==0 && wispy._device_connected==true) {
						publishProgress(Wispy.WISPY_DISCONNECT);
					} else if(wispy_in_devlist==1 && wispy._device_connected==true && wispy._is_polling==false) {
						//Log.d(TAG, "determined that a re-poll is needed");
						//Thread.sleep( 1000 );
						//publishProgress(CoexiSyst.WISPY_POLL);
					}
					
					// Atheros related checks
					if(atheros_in_devlist==1 && ath._device_connected==false) {
						publishProgress(AtherosDev.ATHEROS_CONNECT);
					} else if(atheros_in_devlist==0 && ath._device_connected==true) {
						publishProgress(AtherosDev.ATHEROS_DISCONNECT);
					}
					
					
					Thread.sleep( 2000 );
					Log.d(TAG, "checking for USB devices");

				} catch (Exception e) {
					
					Log.e(TAG, "exception trying to sleep", e);
					return "OUT";
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
			
			// Handling events of Atheros device
			if(event == AtherosDev.ATHEROS_CONNECT) {
				Log.d(TAG, "got update that Atheros card was connected");
				Toast.makeText(parent, "Atheros device connected", Toast.LENGTH_LONG).show();
				ath.connected();
			}
			else if(event == AtherosDev.ATHEROS_DISCONNECT) {
				Log.d(TAG, "Atheros card now disconnected");
				Toast.makeText(parent, "Atheros device disconnected", Toast.LENGTH_LONG).show();
				ath.disconnected();
			}
		}
	}
}