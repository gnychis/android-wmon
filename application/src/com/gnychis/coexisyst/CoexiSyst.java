package com.gnychis.coexisyst;

// do a random port number for pcapd

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
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
	ZigBee zigbee;
	IChart wispyGraph;
	
	NetworksScan _networks_scan;
	
	// For remembering whether to renable interfaces
	boolean _wifi_reenable;
	boolean _bt_reenable;
	
	public BlockingQueue<String> toastMessages;
	
	public enum ThreadMessages {
		WIFI_SCAN_START,
		WIFI_SCAN_COMPLETE,
		WISPY_SCAN_COMPLETE,
		WIFIDEV_CONNECTED,
		WIFIDEV_INITIALIZED,
		WIFIDEV_FAILED,
		ZIGBEE_CONNECTED,
		ZIGBEE_INITIALIZED,
		ZIGBEE_FAILED,
		ZIGBEE_WAIT_RESET,
		ZIGBEE_SCAN_COMPLETE,
		SHOW_TOAST,
		INCREMENT_SCAN_PROGRESS,
		NETWORK_SCANS_COMPLETE,
	}
	
	public Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			///////////////////////////////////////////////////////////////////////
			// A set of messages referring to scans goes to the scan class
			if(msg.obj == ThreadMessages.INCREMENT_SCAN_PROGRESS)
				pd.incrementProgressBy(1);
			if(msg.obj == ThreadMessages.NETWORK_SCANS_COMPLETE)
				networkScansComplete();
						
			///////////////////////////////////////////////////////////////////////
			// A set of messages that that deal with hardware connections
			if(msg.obj == ThreadMessages.WIFIDEV_CONNECTED)
				wifidevSettling();
			if(msg.obj == ThreadMessages.WIFIDEV_INITIALIZED)
				wifidevInitialized();
			if(msg.obj == ThreadMessages.WIFIDEV_FAILED)
				Toast.makeText(getApplicationContext(), "Failed to initialize Wifi device", Toast.LENGTH_LONG).show();	
			if(msg.obj == ThreadMessages.ZIGBEE_CONNECTED)
				zigbeeSettling();
			if(msg.obj == ThreadMessages.ZIGBEE_WAIT_RESET) 
				zigbeeWaiting();
			if(msg.obj == ThreadMessages.ZIGBEE_INITIALIZED) 
				zigbeeInitialized();
			
			///////////////////////////////////////////////////////////////////////
			// A set of messages that that deal with hardware connections
			if(msg.obj == ThreadMessages.SHOW_TOAST) {
				try {
					String m = toastMessages.remove();
					Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show();	
				} catch(Exception e) { }
			}
		}
	};
	
	// This works by putting a bunch of Toast messages in a queue
	// for the main thread to take out and show.
	public void sendToastMessage(Handler h, String msg) {
		try {
			toastMessages.put(msg);
			Message m = new Message();
			m.obj = ThreadMessages.SHOW_TOAST;
			h.sendMessage(m);
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to put toast msg in queue:", e);
		}
	}
	
	public void zigbeeSettling() {
		pd = ProgressDialog.show(this, "", "Initializing ZigBee device...", true, false);  
		usbmon.stopUSBMon();
		zigbee.connected();
	}
	
	public void zigbeeInitialized() {
		pd.dismiss();
		usbmon.startUSBMon();
	}
	
	public void zigbeeWaiting() {
		pd.dismiss();
		pd = ProgressDialog.show(this, "", "Press ZigBee reset button...", true, false); 
	}
	
	public void wifidevSettling() {
		pd = ProgressDialog.show(this, "", "Initializing Wifi device...", true, false); 
		usbmon.stopUSBMon();
		ath.connected();
	}
	
	public void wifidevInitialized() {
		Toast.makeText(getApplicationContext(), "Successfully initialized Wifi device", Toast.LENGTH_LONG).show();	
		pd.dismiss();
		usbmon.startUSBMon();
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
	    	RootTools.sendShell("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system",0);
	    	RootTools.sendShell("mount -t usbfs -o devmode=0666 none /proc/bus/usb",0);
	    	RootTools.sendShell("mount -o remount,rw rootfs /",0);
	    	RootTools.sendShell("ln -s /mnt/sdcard /tmp",0);
	    	
	    	// Disable in releases
	    	ArrayList<String> lib_list = runCommand("ls /data/data/com.gnychis.coexisyst/lib/ | grep \".so\"");
	    	for(int i=0; i<lib_list.size(); i++)
	    		runCommand("ln -s /data/data/com.gnychis.coexisyst/lib/" + lib_list.get(i) + " /system/lib/" + lib_list.get(i));

	    	
	    	/*RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libgmodule-2.0.so /system/lib/",0);
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libusb.so /system/lib/",0);
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libusb-compat.so /system/lib/",0);
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libpcap.so /system/lib/",0);
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libnl.so /system/lib/",0);
	    	RootTools.sendShell("busybox cp /data/data/com.gnychis.coexisyst/lib/libglib-2.0.so /system/lib/",0);*/
	    	
	    	// WARNING: these files do NOT get overwritten if they already exist on the file
	    	// system with RootTools.  If you are updating ANY of these, you need to do:
	    	//   adb uninstall com.gnychis.coexisyst
	    	// And then any updates to these files will be installed on the next build/run.
	    	RootTools.installBinary(this, R.raw.disabled_protos, "disabled_protos");
	    	RootTools.installBinary(this, R.raw.iwconfig, "iwconfig", "755");
	    	RootTools.installBinary(this, R.raw.lsusb, "lsusb", "755");
	    	RootTools.installBinary(this, R.raw.lsusb_core, "lsusb_core", "755");
	    	RootTools.installBinary(this, R.raw.testlibusb, "testlibusb", "755");
	    	RootTools.installBinary(this, R.raw.htc_7010, "htc_7010.fw");
	    	RootTools.installBinary(this, R.raw.iwlist, "iwlist", "755");
	    	RootTools.installBinary(this, R.raw.iw, "iw", "755");
	    	
	    	// Kick the phone in to USB host mode
	    	//RootTools.sendShell("echo a > /sys/devices/platform/s3c-usbgadget/opmode", 0);
	    			
        } catch(Exception e) {
        	Log.e(TAG, "error running RootTools commands for init", e);
        }


    	// Load the libusb related libraries
    	try {
    		System.loadLibrary("glib-2.0");
    		System.loadLibrary("nl");
    		System.loadLibrary("gmodule-2.0");
    		System.loadLibrary("usb");
    		System.loadLibrary("usb-compat");
    		System.loadLibrary("wispy");
    		System.loadLibrary("pcap");
    		System.loadLibrary("gpg-error");
    		System.loadLibrary("gcrypt");
    		System.loadLibrary("tshark");
    		System.loadLibrary("wireshark_helper");
    		System.loadLibrary("coexisyst");
    	} catch (Exception e) {
    		Log.e(TAG, "error trying to load a USB related library", e);
    	}
       
    	wispy = new Wispy();
    	
    	toastMessages = new ArrayBlockingQueue<String>(20);
        
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

		textStatus.append(initUSB());
		
		// Start the USB monitor thread, but only instantiate the wispy scan
		try {
			wispyscan = wispy.new WispyThread();
		} catch (Exception e) { Log.e(TAG, "exception trying to start wispy thread", e); }
		ath = new Wifi(this);
		zigbee = new ZigBee(this);
		
		// Check the pcap interfaces
		//pcapGetInterfaces();
		
		Log.d(TAG, "onCreate()");
		
		if(wiresharkInit()==1)
			Log.d(TAG, "success with wireshark library");
		else
			Log.d(TAG, "error with wireshark library");
		
		usbmon = new USBMon(this, _handler);
		
    	_networks_scan = new NetworksScan(_handler, usbmon, ath, zigbee);
		registerReceiver(_networks_scan._rcvr_80211, new IntentFilter(Wifi.WIFI_SCAN_RESULT));
		registerReceiver(_networks_scan._rcvr_ZigBee, new IntentFilter(ZigBee.ZIGBEE_SCAN_RESULT));
		
		// Uncomment to test wireshark parsing using /sdcard/test.pcap (must be radiotap)
		//wiresharkTest("/sdcard/test.pcap");
		//wiresharkTestGetAll("/sdcard/test.pcap");
		//Log.d(TAG, "Successfully run wireshark test!");
				
    }
    
	public ArrayList<String> runCommand(String c) {
		ArrayList<String> res = new ArrayList<String>();
		try {
			// First, run the command push the result to an ArrayList
			List<String> res_list = RootTools.sendShell(c,0);
			Iterator<String> it=res_list.iterator();
			while(it.hasNext()) 
				res.add((String)it.next());
			
			res.remove(res.size()-1);
			
			// Trim the ArrayList of an extra blank lines at the end
			while(true) {
				int index = res.size()-1;
				if(index>=0 && res.get(index).length()==0)
					res.remove(index);
				else
					break;
			}
			return res;
			
		} catch(Exception e) {
			Log.e("WifiDev", "error writing to RootTools the command: " + c, e);
			return null;
		}
	}
    
    public String getAppUser() {
    	try {
    		List<String> res = RootTools.sendShell("ls -l /data/data/com.gnychis.coexisyst",0);
    		return res.get(0).split(" ")[1];
    	} catch(Exception e) {
    		return "FAIL";
    	}
    }
    
	@Override
 	public void onStop() { super.onStop(); Log.d(TAG, "onStop()");}
	public void onResume() { super.onResume(); Log.d(TAG, "onResume()");

	}
	public void onPause() { super.onPause(); Log.d(TAG, "onPause()"); }
	public void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy()"); }
	
	public void scanSpectrum() {		
		// Get the WiSpy data
		Log.d(TAG, "Waiting for results from WiSpy...");
		wispy.getResultsBlock(Wispy.PASSES);
		Log.d(TAG, "Got results from the WiSpy");
	}

	// This triggers a scan through the networks to return a list of
	// networks and devices for a user to add for management.
	public void clickAddNetwork() {
		int max_progress;
		
		// Do not start another scan, if we already are
		if(_networks_scan.isScanning())
			return;
		
		// Create a progress dialog to show progress of the scan
		// to the user.
		pd = new ProgressDialog(this);
		pd.setCancelable(false);
		pd.setMessage("Scanning for networks...");
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setProgress(0);
		
		// Call the networks scan class to initiate a new scan
		// which, based on the devices connected for scanning,
		// will return a maximum value for the progress bar
		max_progress = _networks_scan.initiateScan();
		pd.setMax(max_progress);
		pd.show();
	}
	
	// Invoked once we have received the results from all of the network scans.
	public void networkScansComplete() {
		pd.dismiss();
		
		try {
			Log.d(TAG,"Trying to load add networks window");
			Intent i = new Intent(CoexiSyst.this, AddNetwork.class);
			
			// Hopefully this is not broken, using it as a WifiScanReceiver rather
			// than BroadcastReceiver type.
			i.putExtra("com.gnychis.coexisyst.80211", _networks_scan._wifi_scan_result);
			i.putExtra("com.gnychis.coexisyst.ZigBee", _networks_scan._zigbee_scan_result);
			
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
			
			/* TODO: fix this
			if(usbmon.getStatus()==Status.RUNNING) {
				if(usbmon.cancel(true))
					Log.d(TAG, "canceled USB monitor");
				else
					Log.d(TAG, "error trying to cancel USB monitor");	
				usbmon = null;
			}*/
			
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
			//scanSpectrum();
		}
		if(view.getId() == R.id.buttonViewSpectrum) {
			//clickViewSpectrum();
		}
		if(view.getId() == R.id.buttonAdb) {
			String[] adb_cmds = { 	"setprop service.adb.tcp.port 5555",
									"stop adbd",
									"start adbd"};
			try {
				RootTools.sendShell(adb_cmds,0,0);
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
	public native void libusbTest();
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