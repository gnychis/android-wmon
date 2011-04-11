package com.gnychis.coexisyst;

import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

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

import com.stericson.RootTools.RootTools;

public class CoexiSyst extends Activity implements OnClickListener {
	
	private static final String TAG = "WiFiDemo";
	
	SubSystem system;

	// Make instances of our helper classes
	DBAdapter db;
	WifiManager wifi;
	BluetoothAdapter bt;
	protected USBMon usbmon;
	protected Wispy.WispyThread wispyscan;
	
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
        try {
    	RootTools.sendShell("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system");
    	RootTools.sendShell("mount -t usbfs -o devmode=0666 none /proc/bus/usb");
    	RootTools.sendShell("mkdir /data/data/com.gnychis.coexisyst/bin");
    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/*.so /system/lib/");
        } catch(Exception e) {
        	Log.e(TAG, "error running RootTools commands for init", e);
        }
    	
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
    		System.loadLibrary("jnetpcap");
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
		try {
			wispyscan = wispy.new WispyThread();
		} catch (Exception e) { Log.e(TAG, "exception trying to start wispy thread", e); }
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
			String[] adb_cmds = { 	"setprop service.adb.tcp.port 5555",
									"stop adbd",
									"start adbd"};
			try {
				RootTools.sendShell(adb_cmds,0);
			} catch(Exception e) {
				Log.e(TAG, "error trying to set ADB over TCP");
				return;
			}
			Log.d(TAG,"ADB set for TCP");
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
		
	protected class Pcapd extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		private int PCAPD_WIFI_PORT = 2000; // be careful this is consistent with WifiMon
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			
			try {
				Log.d("Pcapd", "launching instance of pcapd");
				RootTools.sendShell("/data/data/com.gnychis.coexisyst/bin/pcapd wlan0 " + Integer.toString(PCAPD_WIFI_PORT) + " &");
			} catch(Exception e) {
				Log.e(TAG, "error trying to start pcap daemon",e);
				return "FAIL";
			}
			
			return "OK";
		}
	}
	
	protected class WifiMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		SubSystem wifi_subsystem;
		Socket skt;
		private int PCAPD_WIFI_PORT = 2000;
		InputStream skt_in;
		private static final String WIMON_TAG = "WiFiMonitor";
		private int PCAP_HDR_SIZE = 16;
		Pcapd pcap_thread;
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			Log.d(WIMON_TAG, "a new Wifi monitor thread was started");
			
			// Attempt to create capture process spawned in the background
			// which we will connect to for pcap information.
			//RootTools.sendShell("pcapd wlan0 " + Integer.toString(PCAPD_WIFI_PORT) + " &");
			pcap_thread = new Pcapd();
			pcap_thread.execute(coexisyst);
			
			try { Thread.sleep(1000); } catch (Exception e) {} // give some time for the process
			Log.d(WIMON_TAG, "launched pcapd");
			
			// Attempt to connect to the socket via TCP for the PCAP info
			try {
				skt = new Socket("localhost", PCAPD_WIFI_PORT);
			} catch(Exception e) {
				Log.e(WIMON_TAG, "exception trying to connect to wifi socket for pcap", e);
				return "FAIL";
			}
			
			try {
				skt_in = skt.getInputStream();
			} catch(Exception e) {
				Log.e(WIMON_TAG, "exception trying to get inputbuffer from socket stream");
				return "FAIL";
			}
			Log.d(WIMON_TAG, "successfully connected to pcapd");
			
			while(true) {
				byte[] rawph = new byte[PCAP_HDR_SIZE];
				int v=0;
				try {
					int total=0;
					while(total < PCAP_HDR_SIZE) {
						v = skt_in.read(rawph, total, PCAP_HDR_SIZE-total);
						Log.d(TAG, "Read in " + Integer.toString(v));
						if(v==-1)
							return "DONE";
						total+=v;
					}
				} catch(Exception e) { Log.e(TAG, "unable to read from pcapd buffer",e); }
				/*Log.d(TAG, "got a pcap header!");
				for(int l=0; l < v; l++) {
					try {
						String curr = String.format("buff[%d]: 0x%x", l, rawph[l]);
						Log.d(TAG, curr);
					} catch(Exception e) {
						Log.e(TAG, "Exception trying to format string...",e);
					}
				}*/
				try {
					PcapHeader header = new PcapHeader();
					JBuffer headerBuffer = new JBuffer(rawph);  
					header.peer(headerBuffer, 0);
					Log.d(TAG, "PCAP Header size: " + Integer.toString(header.wirelen()));
					//Log.d(TAG, "PCAP Header size: " + Integer.toString(header.wirelen()));
				} catch(Exception e) {
					Log.e(TAG, "exception trying to read pcap header",e);
				}
			}
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
				coexisyst.wispyscan = wispy.new WispyThread();
				coexisyst.wispyscan.execute(coexisyst);
				wispy._is_polling = true;
			}
			
			// Handling events of Atheros device
			if(event == AtherosDev.ATHEROS_CONNECT) {
				Log.d(TAG, "got update that Atheros card was connected");
				Toast.makeText(parent, "Atheros device connected", Toast.LENGTH_LONG).show();
				ath.connected();
				ath._monitor_thread = new WifiMon();
				ath._monitor_thread.execute(coexisyst);
				
			}
			else if(event == AtherosDev.ATHEROS_DISCONNECT) {
				Log.d(TAG, "Atheros card now disconnected");
				Toast.makeText(parent, "Atheros device disconnected", Toast.LENGTH_LONG).show();
				ath.disconnected();
				ath._monitor_thread.cancel(true);
			}
		}
	}
}