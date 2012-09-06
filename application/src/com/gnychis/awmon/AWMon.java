package com.gnychis.awmon;

// do a random port number for pcapd

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.DBAdapter;
import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.DeviceHandlers.Wifi;
import com.gnychis.awmon.DeviceHandlers.ZigBee;
import com.gnychis.awmon.Interfaces.AddNetwork;
import com.gnychis.awmon.Interfaces.ManageNetworks;
import com.gnychis.awmon.ScanReceivers.NetworksScan;
import com.stericson.RootTools.RootTools;

public class AWMon extends Activity implements OnClickListener {
	
	private static final String TAG = "WiFiDemo";
	
	// Make instances of our helper classes
	DBAdapter db;
	WifiManager wifi;
	BluetoothAdapter bt;
	protected USBMon usbmon;
	
	public static String _app_name = "com.gnychis.awmon";
	
	private ProgressDialog pd;
	
	public TextView textStatus;
	
	Button buttonAddNetwork; 
	Button buttonManageNets; 
	Button buttonManageDevs;
	Button buttonScanSpectrum;
	Button buttonViewSpectrum;
	Button buttonADB;
	
	// USB device related
	public Wifi ath;
	public ZigBee zigbee;
	
	NetworksScan _networks_scan;
	
	// For remembering whether to renable interfaces
	boolean _wifi_reenable;
	boolean _bt_reenable;
	
	public BlockingQueue<String> toastMessages;
	
	public enum ThreadMessages {
		WIFI_SCAN_START,
		WIFI_SCAN_COMPLETE,
		
		WIFIDEV_CONNECTED,
		WIFIDEV_INITIALIZED,
		WIFIDEV_FAILED,
		
		ZIGBEE_CONNECTED,
		ZIGBEE_INITIALIZED,
		ZIGBEE_FAILED,
		ZIGBEE_WAIT_RESET,
		ZIGBEE_SCAN_COMPLETE,
		
		BLUETOOTH_SCAN_COMPLETE,
		
		SHOW_TOAST,
		INCREMENT_SCAN_PROGRESS,
		NETWORK_SCANS_COMPLETE,
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
	    	ArrayList<String> lib_list = runCommand("ls /data/data/" + _app_name + "/lib/ | grep \".so\"");
	    	for(int i=0; i<lib_list.size(); i++) {
	    		runCommand("rm /system/lib/" + lib_list.get(i));
	    		runCommand("ln -s /data/data/" + _app_name + "/lib/" + lib_list.get(i) + " /system/lib/" + lib_list.get(i));
	    	}

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
	    	RootTools.installBinary(this, R.raw.spectool_mine, "spectool_mine", "755");
	    	RootTools.installBinary(this, R.raw.spectool_raw, "spectool_raw", "755");
	    	RootTools.installBinary(this, R.raw.ubertooth_util, "ubertooth_util", "755");
	    			
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
    		System.loadLibrary("awmon");
    	} catch (Exception e) {
    		Log.e(TAG, "error trying to load a USB related library", e);
    	}
           	
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
		
		// Setup wireless devices
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		bt = BluetoothAdapter.getDefaultAdapter();
		
		// Check the states of the interfaces
		_wifi_reenable = (wifi.isWifiEnabled()) ? true : false;
		_bt_reenable = (bt.isEnabled()) ? true : false;

		textStatus.append(initUSB());
		
		// Start the USB monitor thread, but only instantiate the wispy scan
		ath = new Wifi(this);
		zigbee = new ZigBee(this);
				
		if(wiresharkInit()==1)
			Log.d(TAG, "success with wireshark library");
		else
			Log.d(TAG, "error with wireshark library");
		
		usbmon = new USBMon(this, _handler);
		
    	_networks_scan = new NetworksScan(_handler, usbmon, ath, zigbee, bt);
		registerReceiver(_networks_scan._rcvr_80211, new IntentFilter(Wifi.WIFI_SCAN_RESULT));
		registerReceiver(_networks_scan._rcvr_ZigBee, new IntentFilter(ZigBee.ZIGBEE_SCAN_RESULT));
		registerReceiver(_networks_scan._rcvr_BTooth, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(_networks_scan._rcvr_BTooth, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		
		// Uncomment to test wireshark parsing using /sdcard/test.pcap (must be radiotap)
		//wiresharkTest("/sdcard/test.pcap");
		//wiresharkTestGetAll("/sdcard/test.pcap");
		//Log.d(TAG, "Successfully run wireshark test!");		
    }
    
    // Everything related to clicking buttons in the main interface
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
	
	public Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			// Based on the thread message, a difference action will take place
			ThreadMessages tm = ThreadMessages.values()[msg.what];
			switch(tm) {
			
				//////////////////////////////////////////////////////
				case SHOW_TOAST:
					try {
						String m = toastMessages.remove();
						Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show();	
					} catch(Exception e) { }
					break;
					
				//////////////////////////////////////////////////////
				case WIFIDEV_CONNECTED:
					pd = ProgressDialog.show(AWMon.this, "", "Initializing Wifi device...", true, false); 
					usbmon.stopUSBMon();
					ath.connected();
					break;
					
				case WIFIDEV_INITIALIZED:
					Toast.makeText(getApplicationContext(), "Successfully initialized Wifi device", Toast.LENGTH_LONG).show();	
					pd.dismiss();
					usbmon.startUSBMon();
					break;
					
				case WIFIDEV_FAILED:
					Toast.makeText(getApplicationContext(), "Failed to initialize Wifi device", Toast.LENGTH_LONG).show();	
					break;
					
				//////////////////////////////////////////////////////
				case ZIGBEE_CONNECTED:
					pd = ProgressDialog.show(AWMon.this, "", "Initializing ZigBee device...", true, false);  
					usbmon.stopUSBMon();
					zigbee.connected();
					break;
					
				case ZIGBEE_WAIT_RESET:
					pd.dismiss();
					pd = ProgressDialog.show(AWMon.this, "", "Press ZigBee reset button...", true, false); 
					break;
					
				case ZIGBEE_INITIALIZED:
					pd.dismiss();
					Toast.makeText(getApplicationContext(), "Successfully initialized ZigBee device", Toast.LENGTH_LONG).show();	
					usbmon.startUSBMon();
					break;
					
				case ZIGBEE_FAILED:
					Toast.makeText(getApplicationContext(), "Failed to initialize ZigBee device", Toast.LENGTH_LONG).show();	
					break;
					
					
				//////////////////////////////////////////////////////
				case INCREMENT_SCAN_PROGRESS:
					pd.incrementProgressBy(1);
					break;
				
				case NETWORK_SCANS_COMPLETE:
					pd.dismiss();
					try {
						Log.d(TAG,"Trying to load add networks window");
						Intent i = new Intent(AWMon.this, AddNetwork.class);
						
						// Hopefully this is not broken, using it as a WifiScanReceiver rather
						// than BroadcastReceiver type.
						i.putExtra(_app_name + ".80211", _networks_scan._wifi_scan_result);
						i.putExtra(_app_name + ".ZigBee", _networks_scan._zigbee_scan_result);
						i.putExtra(_app_name + ".Bluetooth", _networks_scan._bluetooth_scan_result);
						i.putExtra(_app_name + ".WiSpy", _networks_scan._wispy_scan_result);
						
						startActivity(i);
					} catch (Exception e) {
						Log.e(TAG, "Exception trying to load network add window",e);
						return;
					}
					break;	
			}
		}
	};
	
	// This works by putting a bunch of Toast messages in a queue
	// for the main thread to take out and show.
	public void sendToastMessage(Handler h, String msg) {
		try {
			toastMessages.put(msg);
			Message m = new Message();
			m.what = ThreadMessages.SHOW_TOAST.ordinal();
			h.sendMessage(m);
		} catch (Exception e) {
			Log.e(TAG, "Exception trying to put toast msg in queue:", e);
		}
	}
	
	public void showProgressUpdate(String s) {
		pd = ProgressDialog.show(this, "", s, true, false);  
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
    		List<String> res = RootTools.sendShell("ls -l /data/data | grep " + _app_name,0);
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
		
		// Call the networks scan class to initiate a new scan
		// which, based on the devices connected for scanning,
		// will return a maximum value for the progress bar
		max_progress = _networks_scan.initiateScan();
		if(max_progress==-1) {
			Toast.makeText(getApplicationContext(), "No networks available to scan!", Toast.LENGTH_LONG).show();
			return;
		}
		if(max_progress > 0) {
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setProgress(0);
			pd.setMax(max_progress);
		}
		pd.show();
	}
	
	public void clickManageDevs() {
		Log.d(TAG,"Trying to load manage networks window");
        Intent i = new Intent(AWMon.this, ManageNetworks.class);
        startActivity(i);
	}
	
	public native String  initUSB();
	public native String[] getDeviceNames();
	public native String[] getWiSpyList();
	public native int USBcheckForDevice(int vid, int pid);
	public native void libusbTest();
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