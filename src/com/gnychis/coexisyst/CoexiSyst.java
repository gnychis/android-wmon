package com.gnychis.coexisyst;

// do a random port number for pcapd

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;

public class CoexiSyst extends Activity implements OnClickListener {
	
	private static final String TAG = "WiFiDemo";
	
	// Make instances of our helper classes
	DBAdapter db;
	WifiManager wifi;
	BluetoothAdapter bt;
	protected USBMon usbmon;
	protected Wispy.WispyThread wispyscan;
	
	private ProgressDialog pd;
	
	// Receivers
	WiFiScanReceiver rcvr_80211;
	BroadcastReceiver rcvr_BTooth;
	
	TextView textStatus;
	
	Button buttonAddNetwork; 
	Button buttonManageNets; 
	Button buttonManageDevs;
	Button buttonScanSpectrum;
	Button buttonViewSpectrum;
	Button buttonADB;
	
	// USB device related
	Wispy wispy;
	Wifi ath;
	IChart wispyGraph;
	
	// For remembering whether to renable interfaces
	boolean _wifi_reenable;
	boolean _bt_reenable;
	
	public enum ThreadMessages {
		WIFI_SCAN_COMPLETE,
		WISPY_SCAN_COMPLETE,
		ATHEROS_CONNECTED,
		ATHEROS_INITIALIZED,
		ATHEROS_FAILED,
	}
	
	public Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			if(msg.obj == ThreadMessages.WIFI_SCAN_COMPLETE)
				wifiScanComplete();
			
			if(msg.obj == ThreadMessages.ATHEROS_CONNECTED) {
				atherosSettling();
				ath.connected();
			}
			
			if(msg.obj == ThreadMessages.ATHEROS_INITIALIZED) {
				atherosInitialized();
				Toast.makeText(getApplicationContext(), "Successfully initialized Atheros card", Toast.LENGTH_LONG).show();	
			}
			
			if(msg.obj == ThreadMessages.ATHEROS_FAILED) {
				Toast.makeText(getApplicationContext(), "Failed to initialize Atheros card", Toast.LENGTH_LONG).show();	
			}

		}
	};
	
	public void atherosSettling() {
		pd = ProgressDialog.show(this, "", "Initializing Atheros card...", true, false);  
	}
	
	public void atherosInitialized() {
		pd.dismiss();
	}
	
	public void showProgressUpdate(String s) {
		pd = ProgressDialog.show(this, "", s, true, false);  
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
	    	RootTools.sendShell("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system");
	    	RootTools.sendShell("mount -t usbfs -o devmode=0666 none /proc/bus/usb");
	    	RootTools.sendShell("mount -o remount,rw rootfs /");
	    	RootTools.sendShell("ln -s /mnt/sdcard /tmp");
	    	
	    	// Disable in releases
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libgmodule-2.0.so /system/lib/");
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libusb.so /system/lib/");
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libusb-compat.so /system/lib/");
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libpcap.so /system/lib/");
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libnl.so /system/lib/");
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libglib-2.0.so /system/lib/");
	    	
	    	// WARNING: these files do NOT get overwritten if they already exist on the file
	    	// system with RootTools.  If you are updating ANY of these, you need to do:
	    	//   adb uninstall com.gnychis.coexisyst
	    	// And then any updates to these files will be installed on the next build/run.
	    	RootTools.installBinary(this, R.raw.disabled_protos, "disabled_protos");
	    	RootTools.installBinary(this, R.raw.iwconfig, "iwconfig", "755");
	    	RootTools.installBinary(this, R.raw.lsusb, "lsusb", "755");
	    	RootTools.installBinary(this, R.raw.pcapd, "pcapd", "755");
	    	RootTools.installBinary(this, R.raw.htc_7010, "htc_7010.fw");
	    	
	    			
        } catch(Exception e) {
        	Log.e(TAG, "error running RootTools commands for init", e);
        }


    	// Load the libusb related libraries
    	try {
    		System.loadLibrary("glib-2.0");
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("wispy");
    		System.loadLibrary("pcap");
    		System.loadLibrary("tshark");
    		System.loadLibrary("wireshark_helper");
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
		
		// Check the states of the interfaces
		_wifi_reenable = (wifi.isWifiEnabled()) ? true : false;
		_bt_reenable = (bt.isEnabled()) ? true : false;

		// Register Broadcast Receiver
		if (rcvr_80211 == null) {
			rcvr_80211 = new WiFiScanReceiver(_handler);
			registerReceiver(rcvr_80211, new IntentFilter(Wifi.WIFI_SCAN_RESULT));
		}
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
		ath = new Wifi(this);
		
		// Check the pcap interfaces
		//pcapGetInterfaces();
		
		Log.d(TAG, "onCreate()");

		if(wiresharkInit()==1)
			Log.d(TAG, "success with wireshark library");
		else
			Log.d(TAG, "error with wireshark library");
		
		// Uncomment to test wireshark parsing using /sdcard/test.pcap (must be radiotap)
		//wiresharkTest("/sdcard/test.pcap");
		//wiresharkTestGetAll("/sdcard/test.pcap");
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
			if(_wifi_reenable)
				wifi.setWifiEnabled(true);
			if(_bt_reenable)
				bt.enable();
			
			registerReceiver(rcvr_80211, new IntentFilter(
					Wifi.WIFI_SCAN_RESULT));	
			registerReceiver(rcvr_BTooth, new IntentFilter(
					BluetoothDevice.ACTION_FOUND));
			
			bt.startDiscovery();
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to register scan receivers");
		}
	}
	
	public void stopScans() {
		try {
		//unregisterReceiver(rcvr_80211);	
		unregisterReceiver(rcvr_BTooth);
		
		bt.cancelDiscovery();
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to unregister scan receivers",e);
		}
		
		// Disable interfaces, store history to renable if they were enabled
		_wifi_reenable = (wifi.isWifiEnabled()) ? true : false;
		_bt_reenable = (bt.isEnabled()) ? true : false;
		bt.disable();
		wifi.setWifiEnabled(false);
	}
	
	public void wifiScanComplete() {
		Log.d(TAG, "Wifi scan is now complete");
		pd.dismiss();
	}
	
	public void clickAddNetwork() {
		pd = ProgressDialog.show(this, "", "Scanning, please wait...", true, false);    
		
		// start the scanning process, which happens in another thread
		ath.APScan();
		
		try {
			Log.d(TAG,"Trying to load add networks window");
			Intent i = new Intent(CoexiSyst.this, AddNetwork.class);
			
			// Hopefully this is not broken, using it as a WifiScanReceiver rather
			// than BroadcastReceiver type.
			i.putExtra("com.gnychis.coexisyst.80211", rcvr_80211._last_scan);
			
			startActivity(i);
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to load network add window",e);
			return;
		}
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

	
	public native String  initUSB();
	public native String[] getDeviceNames();
	public native int getWiSpy();
	public native int USBcheckForDevice(int vid, int pid);
	public native String[] getWiSpyList();
	public native int initWiSpyDevices();
	public native int[] pollWiSpy();
	public native int pcapGetInterfaces();
	public native int wiresharkInit();
	public native int dissectPacket(byte[] header, byte[] data, int encap);
	public native void dissectCleanup(int dissect_ptr);
	public native String wiresharkGet(int dissect_ptr, String param);
	public native void wiresharkTest(String filename);
	public native void wiresharkTestGetAll(String filename);
	public native String[] wiresharkGetAll(int dissect_ptr);
	public native void wiresharkGetAllTest(int dissect_ptr);
	
		
}