package com.gnychis.awmon.Core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

// A class to handle USB worker like things
public class USBMon
{
	private static boolean VERBOSE = false;
	public static final String USBMON_DEVICELIST = "awmon.usbmon.devicelist";
	
	Context _parent;
	private static int USB_POLL_TIME=7000;  // in milliseconds, poll time
	
	private Timer _scan_timer;
	
	public USBMon(Context c, Handler h) {
		_parent = c;
		_scan_timer=null;
		initUSB();
		startUSBMon();
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d("USBMon", msg);
	}
	
	public boolean startUSBMon() {
		if(_scan_timer!=null)
			return false;
		_scan_timer=new Timer();
		_scan_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				usbPoll();
			}

		}, 0, USB_POLL_TIME);
		return true;
	}
	
	public boolean stopUSBMon() {
		if(_scan_timer==null)
			return false;
		
		_scan_timer.cancel();
		_scan_timer=null;
		return true;
	}

	// FIXME: Check 1, see if this gets the proper list.
	public void usbPoll( )
	{
		String usbList[] = GetUSBList();
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(usbList));
		
		// Store the entire device list in a broadcast message and send it out.
		// This allows each of the device handlers to be able to detect if their
		// device has been connected or disconnected.
		Intent i = new Intent();
		i.setAction(USBMON_DEVICELIST);
		i.putExtra("devices", list);
		_parent.sendBroadcast(i); 
		
		//int wifidev_in_devlist = USBcheckForDevice(0x13b1,0x002f) + USBcheckForDevice(0x0411,0x017f);
		//int econotag_in_devlist = USBcheckForDevice(0x0403, 0x6010);
	}
	
	// This allows external classes to use this helper function to give it the list of devices and
	// check if the device is in there based on the vendor and product IDs.
	public static boolean isDeviceInList(ArrayList<String> devices, int vendorID, int productID) {
		for(String dev : devices)
    		if(Integer.parseInt(dev.split(":")[0])==vendorID && Integer.parseInt(dev.split(":")[1])==productID)
    			return true;
    	return false;
	}
	
	public native int  initUSB();
	public native void USBList();
	public native int USBcheckForDevice(int vid, int pid);
	public native String[] GetUSBList();

}
