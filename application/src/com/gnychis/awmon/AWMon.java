package com.gnychis.awmon;

// do a random port number for pcapd

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.BackgroundService.LocationMonitor;
import com.gnychis.awmon.BackgroundService.BackgroundService.BackgroundServiceBinder;
import com.gnychis.awmon.Core.DBAdapter;
import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.Interfaces.ManageNetworks;
import com.gnychis.awmon.Interfaces.Status;
import com.gnychis.awmon.Interfaces.Welcome;
import com.stericson.RootTools.RootTools;

public class AWMon extends Activity implements OnClickListener {
	
	private static final String TAG = "AWMon";
	public static String _app_name = "com.gnychis.awmon";
	public static final String THREAD_MESSAGE = "awmon.thread.message";
	
	// Internal Android mechanisms for settings/storage
	public DBAdapter _db;
	public UserSettings _settings;
	
	// Related to communication and tracking of the background service
	public BackgroundService _backgroundService;
	private boolean mBound=false;
	
	// Interface related
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
    
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver _messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	ThreadMessages tm = (ThreadMessages) intent.getExtras().get("type");
        	
        	switch(tm) {
        		case SHOW_TOAST:
        			String msg = (String) intent.getExtras().get("msg");
        			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        		break;
        	}     	
        }
    }; 
    
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BackgroundServiceBinder binder = (BackgroundServiceBinder) service;
            _backgroundService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
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
		    	RootTools.installBinary(parent, R.raw.iwlist, "iwlist", "755");
		    	RootTools.installBinary(parent, R.raw.iw, "iw", "755");
		    	RootTools.installBinary(parent, R.raw.spectool_mine, "spectool_mine", "755");
		    	RootTools.installBinary(parent, R.raw.spectool_raw, "spectool_raw", "755");
		    	RootTools.installBinary(parent, R.raw.ubertooth_util, "ubertooth_util", "755");
		    	RootTools.installBinary(parent, R.raw.link_libraries, "link_libraries.sh", "755");
		    	RootTools.installBinary(parent, R.raw.link_binaries, "link_binaries.sh", "755");
		    	RootTools.installBinary(parent, R.raw.init_wifi, "init_wifi.sh", "755");
		    	RootTools.installBinary(parent, R.raw.tcpdump, "tcpdump", "755");
		    	RootTools.installBinary(parent, R.raw.tshark, "tshark", "755");
		    	RootTools.installBinary(parent, R.raw.dumpcap, "dumpcap", "755");
		    	
		    	// Run a script that will link libraries in /system/lib so that our binaries can run
		    	Log.d(TAG, "Creating links to libraries...");
		    	AWMon.runCommand("sh /data/data/" + AWMon._app_name + "/files/link_libraries.sh " + AWMon._app_name);
		    	AWMon.runCommand("sh /data/data/" + AWMon._app_name + "/files/link_binaries.sh " + AWMon._app_name);
		    			
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
			
			wiresharkTest("/sdcard/test.pcap");
			
			return r;
		}
        
        @Override
        protected void onPostExecute(String result) {
    		
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
	
	public void scanResultsAvailable() {
		
	}
	
	public void showProgressUpdate(String s) {
		_pd = ProgressDialog.show(this, "", s, true, false);  
	}
	

	static public ArrayList<String> runCommand(String c) {
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
			Log.e("AWMon", "error writing to RootTools the command: " + c, e);
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
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BackgroundService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
	@Override
	public void onResume() { 
		super.onResume(); 
		registerReceiver(_messageReceiver, new IntentFilter(AWMon.THREAD_MESSAGE));
	}
	public void onPause() { 
		super.onPause(); 
		Log.d(TAG, "onPause()"); 
		unregisterReceiver(_messageReceiver);
	}
	public void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy()"); }

	// This triggers a scan through the networks to return a list of
	// networks and devices for a user to add for management.
	public void clickAddNetwork() {
		
		int max_progress;
		
		// Do not start another scan, if we already are
		if(_backgroundService._deviceHandler._networks_scan.isScanning())
			return;
		
		// Create a progress dialog to show progress of the scan
		// to the user.
		_pd = new ProgressDialog(this);
		_pd.setCancelable(false);
		_pd.setMessage("Scanning for networks...");
		
		// Call the networks scan class to initiate a new scan
		// which, based on the devices connected for scanning,
		// will return a maximum value for the progress bar
		max_progress = _backgroundService._deviceHandler._networks_scan.initiateScan();
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