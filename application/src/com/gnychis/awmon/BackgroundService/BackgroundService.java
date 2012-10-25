package com.gnychis.awmon.BackgroundService;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.UserSettings;
import com.stericson.RootTools.RootTools;

public class BackgroundService extends Service {
	
	// Need a binder so that the main activity can communicate with the server
	private final IBinder _binder = new BackgroundServiceBinder();

    public static AWMon _awmon;
    static BackgroundService _this;
    private MotionDetector _motionDetector;
    private LocationMonitor _locationMonitor;
    public DeviceHandler _deviceHandler;
    
    public static String TAG = "AWMonBackground";

    public static final boolean DEBUG=true;
    
    private UserSettings _settings;
    
    ServiceState _serviceState;
	public enum ServiceState {
		IDLE,
		PROXIMITY_DETECTION,
		TAKING_SNAPSHOT,
	}
       
    @Override
    public void onCreate() {
    	super.onCreate();
    	_this=this;
    	
    	Log.d(TAG, "Background service is now running");
    	
		String r="";
		
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
	    	RootTools.installBinary(this, R.raw.disabled_protos, "disabled_protos");
	    	RootTools.installBinary(this, R.raw.iwconfig, "iwconfig", "755");
	    	RootTools.installBinary(this, R.raw.lsusb, "lsusb", "755");
	    	RootTools.installBinary(this, R.raw.lsusb_core, "lsusb_core", "755");
	    	RootTools.installBinary(this, R.raw.testlibusb, "testlibusb", "755");
	    	RootTools.installBinary(this, R.raw.iwlist, "iwlist", "755");
	    	RootTools.installBinary(this, R.raw.iw, "iw", "755");
	    	RootTools.installBinary(this, R.raw.spectool_mine, "spectool_mine", "755");
	    	RootTools.installBinary(this, R.raw.spectool_raw, "spectool_raw", "755");
	    	RootTools.installBinary(this, R.raw.ubertooth_util, "ubertooth_util", "755");
	    	RootTools.installBinary(this, R.raw.link_libraries, "link_libraries.sh", "755");
	    	RootTools.installBinary(this, R.raw.link_binaries, "link_binaries.sh", "755");
	    	RootTools.installBinary(this, R.raw.init_wifi, "init_wifi.sh", "755");
	    	RootTools.installBinary(this, R.raw.tcpdump, "tcpdump", "755");
	    	RootTools.installBinary(this, R.raw.tshark, "tshark", "755");
	    	RootTools.installBinary(this, R.raw.dumpcap, "dumpcap", "755");
	    	
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
    	
//		if(wiresharkInit()!=1)
//			r += "Failed to initialize wireshark library...\n";
    	
    	// Initialize the states
    	_serviceState=ServiceState.IDLE;
    	    	    	    	    
    	// Make a few instances to our helper classes
    	_settings = new UserSettings(this);
    	_motionDetector = new MotionDetector(this);
    	_locationMonitor = new LocationMonitor(this);
    	_deviceHandler = new DeviceHandler(this);
    	        
        registerReceiver(new BroadcastReceiver()
        {
        	@Override
        	public void onReceive(Context context, Intent intent)
        	{
        		cleanup();
        	}
        }, new IntentFilter(Intent.ACTION_SHUTDOWN));
        
        registerReceiver(locationUpdate, new IntentFilter(LocationMonitor.LOCATION_UPDATE));
    }
    
    // This receives updates when the phone either enters the home or leaves the home
    private BroadcastReceiver locationUpdate = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	LocationMonitor.StateChange state = (LocationMonitor.StateChange) intent.getExtras().get("state");
        	
        	switch(state) {
	        	case ENTERING_HOME:
	        		break;
	        		
	        	case LEAVING_HOME:
	        		break;
        	}     	
        }
    };   
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	cleanup();
    }
    
    // When the phone is being shut down or the application is being destroyed, we perform a cleanup
    // action and note the change in state of the phone.
    private void cleanup() {
    	_motionDetector.unregisterSensors();
    	unregisterReceiver(locationUpdate);
    }
    
    public class BackgroundServiceBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }
    
	public static void setMainActivity(AWMon activity) { _awmon = activity; }
	
	public native int wiresharkInit();
}
