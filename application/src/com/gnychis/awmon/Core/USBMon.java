package com.gnychis.awmon.Core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.BackgroundService.LocationMonitor.StateChange;
import com.stericson.RootTools.RootTools;

// A class to handle USB worker like things
public class USBMon
{
	private static boolean VERBOSE = false;
	public static final String USBMON_DEVICELIST = "awmon.usbmon.devicelist";
	
	Context _parent;
	private Handler _handler;
	private static int USB_POLL_TIME=7000;  // in milliseconds, poll time
	
	private Timer _scan_timer;
	
	public USBMon(Context c, Handler h) {
		_parent = c;
		_handler = h;
		_scan_timer=null;
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
	
	// This function makes a major assumption that only the AR9280 has a file in /sys called loading
	// when it is expecting firmware.  But, it's held true so far to bypass USB detection issues
	// and workaround needed a udev daemon.
	protected int checkAR9280()
	{
		try {
			List<String> res = RootTools.sendShell("busybox find /sys -name loading",0);
			if(res.size()!=0)
				return 1;
		} catch(Exception e) {
			Log.e("USBMon", "exception trying to check for AR9280", e);
		}
		return 0;
	}
	
	// FIXME: Check 1, see if this gets the proper list.
	public void usbPoll( )
	{
		String usbList[] = GetUSBList();
		ArrayList<String> list = new ArrayList(Arrays.asList(usbList));
		Intent i = new Intent();
		i.setAction(USBMON_DEVICELIST);
		i.putExtra("type", StateChange.ENTERING_HOME);
		_parent.sendBroadcast(i);
		
		/*
		//int wifidev_in_devlist = USBcheckForDevice(0x13b1,0x002f) + USBcheckForDevice(0x0411,0x017f);
		//int econotag_in_devlist = USBcheckForDevice(0x0403, 0x6010);
				
		//if(atheros_in_devlist==0)
		//	atheros_in_devlist = checkAR9280();  // this is a more expensive check, only do when necessary

		// Wifi device check
		if(wifidev_in_devlist==1 && _parent._wifi._device_connected==false)
			updateState(Wifi.WIFIDEV_CONNECT);
		else if(wifidev_in_devlist==0 && _parent._wifi._device_connected==true)
			updateState(Wifi.WIFIDEV_DISCONNECT);

		// Econotag check
		if(econotag_in_devlist==1 && _parent._zigbee._device_connected==false)
			updateState(ZigBee.ZIGBEE_CONNECT);
		else if(econotag_in_devlist==0 && _parent._zigbee._device_connected==true)
			updateState(ZigBee.ZIGBEE_DISCONNECT);
			*/
 
	}
	
	// FIXME:  This seems redundant with the function above it (usbPoll())
	/*protected void updateState(int event)
	{
		// Handling events of Wifi device
		if(event == Wifi.WIFIDEV_CONNECT) {
			Message msg = new Message();
			msg.what = ThreadMessages.WIFIDEV_CONNECTED.ordinal();
			_parent._handler.sendMessage(msg);
			debugOut("got update that Wifi card was connected");
		}
		else if(event == Wifi.WIFIDEV_DISCONNECT) {
			debugOut("Wifi device now disconnected");
			sendToast("Wifi device disconnected");
			_parent._wifi.disconnected();
		}
		
		// Handling events for ZigBee device
		if(event == ZigBee.ZIGBEE_CONNECT) {
			Message msg = new Message();
			msg.what = ThreadMessages.ZIGBEE_CONNECTED.ordinal();
			_parent._handler.sendMessage(msg);
			debugOut("got update that ZigBee device was connected");
		}
		else if(event == ZigBee.ZIGBEE_DISCONNECT) {
			debugOut("ZigBee device now disconnected");
			sendToast("ZigBee device disconnected");
			_parent._zigbee.disconnected();
		}
	}
	*/
	
	private void sendToast(String msg) {
		Intent i = new Intent();
		i.setAction(AWMon.THREAD_MESSAGE);
		i.putExtra("type", AWMon.ThreadMessages.SHOW_TOAST);
		_parent.sendBroadcast(i);
	}
	
	public native void USBList();
	public native int USBcheckForDevice(int vid, int pid);
	public native String[] GetUSBList();

}
