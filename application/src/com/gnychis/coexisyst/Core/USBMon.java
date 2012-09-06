package com.gnychis.coexisyst.Core;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst;
import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;
import com.gnychis.coexisyst.DeviceHandlers.Wifi;
import com.gnychis.coexisyst.DeviceHandlers.ZigBee;
import com.stericson.RootTools.RootTools;

// A class to handle USB worker like things
public class USBMon
{
	private static boolean VERBOSE = false;
	
	CoexiSyst _coexisyst;
	private Handler _handler;
	private static int USB_POLL_TIME=7000;  // in milliseconds, poll time
	
	private Timer _scan_timer;
	
	public USBMon(CoexiSyst c, Handler h) {
		_coexisyst = c;
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
	
	public void usbPoll( )
	{
		int wifidev_in_devlist = _coexisyst.USBcheckForDevice(0x13b1,0x002f) + _coexisyst.USBcheckForDevice(0x0411,0x017f);
		int econotag_in_devlist = _coexisyst.USBcheckForDevice(0x0403, 0x6010);
				
		//if(atheros_in_devlist==0)
		//	atheros_in_devlist = checkAR9280();  // this is a more expensive check, only do when necessary

		// Wifi device check
		if(wifidev_in_devlist==1 && _coexisyst.ath._device_connected==false)
			updateState(Wifi.WIFIDEV_CONNECT);
		else if(wifidev_in_devlist==0 && _coexisyst.ath._device_connected==true)
			updateState(Wifi.WIFIDEV_DISCONNECT);

		// Econotag check
		if(econotag_in_devlist==1 && _coexisyst.zigbee._device_connected==false)
			updateState(ZigBee.ZIGBEE_CONNECT);
		else if(econotag_in_devlist==0 && _coexisyst.zigbee._device_connected==true)
			updateState(ZigBee.ZIGBEE_DISCONNECT);
 
	}
	
	// FIXME:  This seems redundant with the function above it (usbPoll())
	protected void updateState(int event)
	{
		// Handling events of Wifi device
		if(event == Wifi.WIFIDEV_CONNECT) {
			Message msg = new Message();
			msg.what = ThreadMessages.WIFIDEV_CONNECTED.ordinal();
			_coexisyst._handler.sendMessage(msg);
			debugOut("got update that Wifi card was connected");
		}
		else if(event == Wifi.WIFIDEV_DISCONNECT) {
			debugOut("Wifi device now disconnected");
			_coexisyst.sendToastMessage(_handler, "Wifi device disconnected");
			_coexisyst.ath.disconnected();
		}
		
		// Handling events for ZigBee device
		if(event == ZigBee.ZIGBEE_CONNECT) {
			Message msg = new Message();
			msg.what = ThreadMessages.ZIGBEE_CONNECTED.ordinal();
			_coexisyst._handler.sendMessage(msg);
			debugOut("got update that ZigBee device was connected");
		}
		else if(event == ZigBee.ZIGBEE_DISCONNECT) {
			debugOut("ZigBee device now disconnected");
			_coexisyst.sendToastMessage(_handler, "ZigBee device disconnected");
			_coexisyst.zigbee.disconnected();
		}
	}
	public native void USBList();
}
