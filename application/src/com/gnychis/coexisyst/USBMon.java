package com.gnychis.coexisyst;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
		int wispy_in_devlist=_coexisyst.USBcheckForDevice(0x1781, 0x083f);
		int wifidev_in_devlist = _coexisyst.USBcheckForDevice(0x13b1,0x002f) + _coexisyst.USBcheckForDevice(0x0411,0x017f);
		int econotag_in_devlist = _coexisyst.USBcheckForDevice(0x0403, 0x6010);
		
		//if(atheros_in_devlist==0)
		//	atheros_in_devlist = checkAR9280();  // this is a more expensive check, only do when necessary
				
		// Wispy related checks
		if(wispy_in_devlist==1 && _coexisyst.wispy._device_connected==false) {
			updateState(Wispy.WISPY_CONNECT);
		} else if(wispy_in_devlist==0 && _coexisyst.wispy._device_connected==true) {
			updateState(Wispy.WISPY_DISCONNECT);
		} else if(wispy_in_devlist==1 && _coexisyst.wispy._device_connected==true && _coexisyst.wispy._is_polling==false) {
			//debugOut("determined that a re-poll is needed");
			//Thread.sleep( 1000 );
			//publishProgress(CoexiSyst.WISPY_POLL);
		}
		
		if(wifidev_in_devlist==1 && _coexisyst.ath._device_connected==false) {
			updateState(Wifi.WIFIDEV_CONNECT);
		} else if(wifidev_in_devlist==0 && _coexisyst.ath._device_connected==true) {
			updateState(Wifi.WIFIDEV_DISCONNECT);
		}
		
		

		if(econotag_in_devlist==1 && _coexisyst.zigbee._device_connected==false) {
			updateState(ZigBee.ZIGBEE_CONNECT);
		} else if(econotag_in_devlist==0 && _coexisyst.zigbee._device_connected==true) {
			updateState(ZigBee.ZIGBEE_DISCONNECT);
		}
	}
	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
	 */
	protected void updateState(int event)
	{
		if(event == Wispy.WISPY_CONNECT) {
			debugOut("got update that WiSpy was connected");
			_coexisyst.sendToastMessage(_handler, "WiSpy device connected");
			_coexisyst.wispy._device_connected=true;
			
			// List the wispy devices
			_coexisyst.textStatus.append("\n\nWiSpy Devices:\n");
			String devices[] = _coexisyst.getWiSpyList();
			for (int i=0; i<devices.length; i++)
				_coexisyst.textStatus.append(devices[i] + "\n");
			
			// Start the poll thread now
			_coexisyst.wispyscan.execute(_coexisyst);
			_coexisyst.wispy._is_polling = true;
		}
		else if(event == Wispy.WISPY_DISCONNECT) {
			debugOut("got update that WiSpy was connected");
			_coexisyst.sendToastMessage(_handler, "WiSpy device has been disconnected");
			_coexisyst.wispy._device_connected=false;
			_coexisyst.wispyscan.cancel(true);  // make sure to stop polling thread
		}
		else if(event == Wispy.WISPY_POLL) {
			debugOut("trying to re-poll the WiSpy device");
			_coexisyst.sendToastMessage(_handler, "Re-trying polling");
			_coexisyst.wispyscan.cancel(true);
			_coexisyst.wispyscan = _coexisyst.wispy.new WispyThread();
			_coexisyst.wispyscan.execute(_coexisyst);
			_coexisyst.wispy._is_polling = true;
		}
		
		// Handling events of Wifi device
		if(event == Wifi.WIFIDEV_CONNECT) {
			Message msg = new Message();
			msg.obj = ThreadMessages.WIFIDEV_CONNECTED;
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
			msg.obj = ThreadMessages.ZIGBEE_CONNECTED;
			_coexisyst._handler.sendMessage(msg);
			debugOut("got update that ZigBee device was connected");
		}
		else if(event == ZigBee.ZIGBEE_DISCONNECT) {
			debugOut("ZigBee device now disconnected");
			_coexisyst.sendToastMessage(_handler, "ZigBee device disconnected");
			_coexisyst.zigbee.disconnected();
		}
	}
	
}
