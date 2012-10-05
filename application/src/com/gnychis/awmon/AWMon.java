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
import android.os.AsyncTask;
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
import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.Core.DBAdapter;
import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.DeviceHandlers.Wifi;
import com.gnychis.awmon.DeviceHandlers.ZigBee;
import com.gnychis.awmon.Interfaces.AddNetwork;
import com.gnychis.awmon.Interfaces.ManageNetworks;
import com.gnychis.awmon.Interfaces.Status;
import com.gnychis.awmon.Interfaces.Welcome;
import com.gnychis.awmon.ScanReceivers.NetworksScan;
import com.stericson.RootTools.RootTools;

public class AWMon extends Activity implements OnClickListener {
	
	private static final String TAG = "AWMon";
	public static String _app_name = "com.gnychis.awmon";
	
	// Internal Android mechanisms for settings/storage
	public DBAdapter _db;
	public UserSettings _settings;
	
	// Instances to our devices
	public WifiManager _wifiManager;
	public Wifi _wifi;
	public ZigBee _zigbee;
	public BluetoothAdapter _bt;
	public NetworksScan _networks_scan;
	protected USBMon _usbmon;

	// Interface related
	public BlockingQueue<String> toastMessages;
	private ProgressDialog _pd;
	public TextView textStatus;
		
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
           	
    	toastMessages = new ArrayBlockingQueue<String>(20);
        
        // Setup the database
    	_db = new DBAdapter(this);
    	_db.open();
    	
    	// Initialize the user settings
    	_settings = new UserSettings(this);
    	
    	// Start the background service
        BackgroundService.setMainActivity(this);
        startService(new Intent(this, BackgroundService.class));
      
		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		textStatus.setText("");
		((Button) findViewById(R.id.buttonAddNetwork)).setOnClickListener(this);
		((Button) findViewById(R.id.buttonManageDevs)).setOnClickListener(this);
		((Button) findViewById(R.id.buttonSettings)).setOnClickListener(this);
		((Button) findViewById(R.id.buttonStatus)).setOnClickListener(this);
		
		// Finally, initialize and link some of the libraries (which can take a while)
		InitLibraries init_thread = new InitLibraries();
		init_thread.execute(this);
    }
    
    // This runs after the initialization of the libraries, etc.
    public void postInitialization() {
    	
    	if(_settings.haveUserSettings())  // Do we have user settings?
    		return;
    	
    	// If we do not have the user settings, we open up an activity to query for them
		Intent i = new Intent(AWMon.this, Welcome.class);
        startActivity(i);
    }
    
    protected class InitLibraries extends AsyncTask<Context, Integer, String> {
		Context parent;
		AWMon awmon;
    	
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            _pd = ProgressDialog.show(AWMon.this, "", "Initializing application, please wait...", true, false); 
        }
        
		@Override
		protected String doInBackground( Context ... params ) {
			String r="";
			parent = params[0];
			awmon = (AWMon) params[0];
			
	        try {
	        	Log.d(TAG, "Remounting file system...");
		    	RootTools.sendShell("mount -o remount,rw -t yaffs2 /dev/block/mtdblock4 /system",0);
		    	RootTools.sendShell("mount -t usbfs -o devmode=0666 none /proc/bus/usb",0);
		    	RootTools.sendShell("mount -o remount,rw rootfs /",0);
		    	RootTools.sendShell("ln -s /mnt/sdcard /tmp",0);

		    	// WARNING: these files do NOT get overwritten if they already exist on the file
		    	// system with RootTools.  If you are updating ANY of these, you need to do:
		    	//   adb uninstall com.gnychis.coexisyst
		    	// And then any updates to these files will be installed on the next build/run.
		    	Log.d(TAG, "Installing binaries");
		    	RootTools.installBinary(parent, R.raw.disabled_protos, "disabled_protos");
		    	RootTools.installBinary(parent, R.raw.iwconfig, "iwconfig", "755");
		    	RootTools.installBinary(parent, R.raw.lsusb, "lsusb", "755");
		    	RootTools.installBinary(parent, R.raw.lsusb_core, "lsusb_core", "755");
		    	RootTools.installBinary(parent, R.raw.testlibusb, "testlibusb", "755");
		    	RootTools.installBinary(parent, R.raw.htc_7010, "htc_7010.fw");
		    	RootTools.installBinary(parent, R.raw.iwlist, "iwlist", "755");
		    	RootTools.installBinary(parent, R.raw.iw, "iw", "755");
		    	RootTools.installBinary(parent, R.raw.spectool_mine, "spectool_mine", "755");
		    	RootTools.installBinary(parent, R.raw.spectool_raw, "spectool_raw", "755");
		    	RootTools.installBinary(parent, R.raw.ubertooth_util, "ubertooth_util", "755");
		    	RootTools.installBinary(parent, R.raw.link_libraries, "link_libraries.sh", "755");
		    	RootTools.installBinary(parent, R.raw.init_wifi, "init_wifi.sh", "755");
		    	RootTools.installBinary(parent, R.raw.tcpdump, "tcpdump", "755");
		    	RootTools.installBinary(parent, R.raw.tshark_exec, "tshark_exec", "755");
		    	
		    	// Run a script that will link libraries in /system/lib so that our binaries can run
		    	Log.d(TAG, "Creating links to libraries...");
		    	awmon.runCommand("sh /data/data/" + AWMon._app_name + "/files/link_libraries.sh " + AWMon._app_name);
		    			
	        } catch(Exception e) {	Log.e(TAG, "error running RootTools commands for init", e); }

	    	// Load the libusb related libraries
	        Log.d(TAG, "Linking the libraries to the application");
	    	try {
	    		System.loadLibrary("glib-2.0");			System.loadLibrary("nl");
	    		System.loadLibrary("gmodule-2.0");		System.loadLibrary("usb");
	    		System.loadLibrary("usb-compat");		System.loadLibrary("wispy");
	    		System.loadLibrary("pcap");				System.loadLibrary("gpg-error");
	    		System.loadLibrary("gcrypt");			System.loadLibrary("tshark");
	    		System.loadLibrary("wireshark_helper");	System.loadLibrary("awmon");
	    	} catch (Exception e) { Log.e(TAG, "error trying to load a USB related library", e); }
	    	
			// Try to init the USB and the wireshark library.
			if(initUSB()==-1)
				r += "Failed to initialize USB subsystem...\n";			
			if(wiresharkInit()!=1)
				r += "Failed to initialize wireshark library...\n";
			
			return r;
		}
        
        @Override
        protected void onPostExecute(String result) {
        	
    		// Setup internal wireless device handles
    		_wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    		_bt = BluetoothAdapter.getDefaultAdapter();
    		
    		// Create handles to our internal devices and mechanisms
    		_wifi = new Wifi(awmon);
    		_zigbee = new ZigBee(awmon);
    		_usbmon = new USBMon(awmon, _handler);
    		
    		// Register various receivers to receive scan updates.
        	_networks_scan = new NetworksScan(_handler, _usbmon, _wifi, _zigbee, _bt);
    		registerReceiver(_networks_scan._rcvr_80211, new IntentFilter(Wifi.WIFI_SCAN_RESULT));
    		registerReceiver(_networks_scan._rcvr_ZigBee, new IntentFilter(ZigBee.ZIGBEE_SCAN_RESULT));
    		registerReceiver(_networks_scan._rcvr_BTooth, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    		registerReceiver(_networks_scan._rcvr_BTooth, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    		
    		textStatus.setText(result.replaceAll("\n", ""));	// Put any error messages in to the text status box
        	_pd.dismiss();				// Get rid of the spinner
        	
        	postInitialization();	// Run a method to do things post initialization on main activity
        }
    }
    
    // Everything related to clicking buttons in the main interface
	public void onClick(View view) {
		Intent i;
		
		switch(view.getId()) {
			case R.id.buttonAddNetwork:
				clickAddNetwork();
				break;
			
			case R.id.buttonManageDevs:
				i = new Intent(AWMon.this, ManageNetworks.class);
		        startActivity(i);
				break;
				
			case R.id.buttonSettings:
				i = new Intent(AWMon.this, Welcome.class);
				startActivity(i);
				break;
				
			case R.id.buttonStatus:
				i = new Intent(AWMon.this, Status.class);
				startActivity(i);
				break;
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
					_pd = ProgressDialog.show(AWMon.this, "", "Initializing Wifi device...", true, false); 
					_usbmon.stopUSBMon();
					_wifi.connected();
					break;
					
				case WIFIDEV_INITIALIZED:
					Toast.makeText(getApplicationContext(), "Successfully initialized Wifi device", Toast.LENGTH_LONG).show();	
					_pd.dismiss();
					_usbmon.startUSBMon();
					break;
					
				case WIFIDEV_FAILED:
					Toast.makeText(getApplicationContext(), "Failed to initialize Wifi device", Toast.LENGTH_LONG).show();	
					break;
					
				//////////////////////////////////////////////////////
				case ZIGBEE_CONNECTED:
					_pd = ProgressDialog.show(AWMon.this, "", "Initializing ZigBee device...", true, false);  
					_usbmon.stopUSBMon();
					_zigbee.connected();
					break;
					
				case ZIGBEE_WAIT_RESET:
					_pd.dismiss();
					_pd = ProgressDialog.show(AWMon.this, "", "Press ZigBee reset button...", true, false); 
					break;
					
				case ZIGBEE_INITIALIZED:
					_pd.dismiss();
					Toast.makeText(getApplicationContext(), "Successfully initialized ZigBee device", Toast.LENGTH_LONG).show();	
					_usbmon.startUSBMon();
					break;
					
				case ZIGBEE_FAILED:
					Toast.makeText(getApplicationContext(), "Failed to initialize ZigBee device", Toast.LENGTH_LONG).show();	
					break;
					
					
				//////////////////////////////////////////////////////
				case INCREMENT_SCAN_PROGRESS:
					_pd.incrementProgressBy(1);
					break;
				
				case NETWORK_SCANS_COMPLETE:
					_pd.dismiss();
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
		_pd = ProgressDialog.show(this, "", s, true, false);  
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
		_pd = new ProgressDialog(this);
		_pd.setCancelable(false);
		_pd.setMessage("Scanning for networks...");
		
		// Call the networks scan class to initiate a new scan
		// which, based on the devices connected for scanning,
		// will return a maximum value for the progress bar
		max_progress = _networks_scan.initiateScan();
		if(max_progress==-1) {
			Toast.makeText(getApplicationContext(), "No networks available to scan!", Toast.LENGTH_LONG).show();
			return;
		}
		if(max_progress > 0) {
			_pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			_pd.setProgress(0);
			_pd.setMax(max_progress);
		}
		_pd.show();
	}
	
	// Used to send messages to the main Activity (UI) thread
	public static void sendMainMessage(Handler handler, AWMon.ThreadMessages t) {
		Message msg = new Message();
		msg.what = t.ordinal();
		handler.sendMessage(msg);
	}
	
	public native int  initUSB();
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