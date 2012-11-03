package com.gnychis.awmon.BackgroundService;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Interfaces.AWMon;
import com.stericson.RootTools.RootTools;

public class InitLibraries extends AsyncTask<Context, Integer, String> {
	Context _parent;
	public static String TAG = "AWMonInitLibraries";

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    
	@Override
	protected String doInBackground( Context ... params ) {
		String r="";
		_parent = params[0];
		
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
	    	RootTools.installBinary(_parent, R.raw.disabled_protos, "disabled_protos");
	    	RootTools.installBinary(_parent, R.raw.iwconfig, "iwconfig", "755");
	    	RootTools.installBinary(_parent, R.raw.lsusb, "lsusb", "755");
	    	RootTools.installBinary(_parent, R.raw.lsusb_core, "lsusb_core", "755");
	    	RootTools.installBinary(_parent, R.raw.testlibusb, "testlibusb", "755");
	    	RootTools.installBinary(_parent, R.raw.iwlist, "iwlist", "755");
	    	RootTools.installBinary(_parent, R.raw.iw, "iw", "755");
	    	RootTools.installBinary(_parent, R.raw.spectool_mine, "spectool_mine", "755");
	    	RootTools.installBinary(_parent, R.raw.spectool_raw, "spectool_raw", "755");
	    	RootTools.installBinary(_parent, R.raw.ubertooth_util, "ubertooth_util", "755");
	    	RootTools.installBinary(_parent, R.raw.link_libraries, "link_libraries.sh", "755");
	    	RootTools.installBinary(_parent, R.raw.link_binaries, "link_binaries.sh", "755");
	    	RootTools.installBinary(_parent, R.raw.init_wifi, "init_wifi.sh", "755");
	    	RootTools.installBinary(_parent, R.raw.tcpdump, "tcpdump", "755");
	    	RootTools.installBinary(_parent, R.raw.tshark, "tshark", "755");
	    	RootTools.installBinary(_parent, R.raw.dumpcap, "dumpcap", "755");
	    	RootTools.installBinary(_parent, R.raw.oui, "oui.txt", "755");
	    	RootTools.installBinary(_parent, R.raw.arp_scan, "arp_scan", "755");
	    	
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
    	
		if(wiresharkInit()!=1)
			r += "Failed to initialize wireshark library...\n";
		
		return r;
	}
	
    @Override
    protected void onPostExecute(String result) {
    	Intent i = new Intent();
		i.setAction(BackgroundService.SYSTEM_INITIALIZED);
		_parent.sendBroadcast(i);
    }
	
	public native int wiresharkInit();
}
