package com.gnychis.awmon.DeviceHandlers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.jnetpcap.protocol.network.Arp.HardwareType;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.Core.USBSerial;
import com.gnychis.awmon.Scanners.ZigBeeScanner;
import com.stericson.RootTools.RootTools;

public class ZigBee extends HardwareDevice {
	private static final String TAG = "ZigbeeDev";
	private static final boolean VERBOSE = true;
	
	// This defines the device USB ID we are looking for
	class USBZigBeeDev {
		public static final int vendorID=0x0403;
		public static final int productID=0x6010;
	}

	public static final int MS_SLEEP_UNTIL_PCAPD = 5000;
		
	public boolean _device_connected;
	ZigBeeScanner _monitor_thread;
	
	public ZigBee(Context c) {
		super(HardwareDevice.Type.ZigBee);
		_parent = c;
		Log.d(TAG, "Initializing ZigBee class...");
		_parent.registerReceiver(usbUpdate, new IntentFilter(USBMon.USBMON_DEVICELIST));
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Related to the connection and disconnection of the USB device
	
    // Receives messages about USB devices
    private BroadcastReceiver usbUpdate = new BroadcastReceiver() {
    	@Override @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
        	ArrayList<String> devices = (ArrayList<String>) intent.getExtras().get("devices");
        	if(USBMon.isDeviceInList(devices, USBZigBeeDev.vendorID, USBZigBeeDev.productID)) {
        		if(!_device_connected)
        			connected();
        	} else {
        		if(_device_connected)
        			disconnected();
        	}
        }
    };  
   
	public void connected() {
		_device_connected=true;
		ZigBeeInit zbi = new ZigBeeInit();
		zbi.execute(_parent);
	}
	
	public void disconnected() {
		_device_connected=false;
		AWMon.sendToastRequest(_parent, "ZigBee device disconnected");
	}
	
	public boolean isConnected() {
		return _device_connected;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Related to initializing the hardware
	protected class ZigBeeInit extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		USBSerial _dev;
		
		// The initialized sequence (hardware sends it when it is initialized)
		byte initialized_sequence[] = {0x67, 0x65, 0x6f, 0x72, 0x67, 0x65, 0x6e, 0x79, 0x63, 0x68, 0x69, 0x73};

	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        AWMon.sendProgressDialogRequest(_parent, "Initializing ZigBee device..");
	    }
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			
			// Create a serial device
			_dev = new USBSerial();
			
			// Get the name of the USB device, which will be the last thing in dmesg
			String ttyUSB_name;
			try {
				List<String> res = RootTools.sendShell("dmesg | grep ttyUSB | tail -n 1 | awk '{print $NF}'",0);
				ttyUSB_name = res.get(0);
			} catch (Exception e) { return ""; }	
			
			// Attempt to open the COM port which calls the native libraries
			if(!_dev.openPort("/dev/" + ttyUSB_name))
				return "FAIL";
			
			debugOut("opened device, now waiting for sequence");
			
			// Wait for the initialized sequence...
			byte[] readSeq = new byte[initialized_sequence.length];
			AWMon.sendThreadMessage(_parent, AWMon.ThreadMessages.CANCEL_PROGRESS_DIALOG);
			AWMon.sendProgressDialogRequest(_parent, "Press the ZigBee reset button...");
			while(!checkInitSeq(readSeq)) {
				for(int i=0; i<initialized_sequence.length-1; i++)
					readSeq[i] = readSeq[i+1];
				readSeq[initialized_sequence.length-1] = _dev.getByte();
			}
			
			debugOut("received the initialization sequence!");
			
			// Close the port
			if(!_dev.closePort())
				AWMon.sendToastRequest(_parent, "Failed to initialize ZigBee device");

			return "OK";
		}
		
	    @Override
	    protected void onPostExecute(String result) {
	    	AWMon.sendThreadMessage(_parent, AWMon.ThreadMessages.CANCEL_PROGRESS_DIALOG);
	    	AWMon.sendToastRequest(_parent, "ZigBee device initialized");
	    }
		
		private void debugOut(String msg) {
			if(VERBOSE)
				Log.d("ZigBeeInit", msg);
		}
		
		public boolean checkInitSeq(byte buf[]) {
			
			for(int i=0; i<initialized_sequence.length; i++)
				if(initialized_sequence[i]!=buf[i])
					return false;
						
			return true;
		}
		
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Functions for helping convert channels to frequencies
	public static int[] channels = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
	public static int[] frequencies = {2405, 2410, 2415, 2420, 2425, 2430, 2435, 
			2440, 2445, 2450, 2455, 2460, 2465, 2470, 2475, 2480};
	
	static public int freqToChan(int freq) {
		int i=0;
		for(i=0; i<frequencies.length; i++)
			if(frequencies[i]==freq)
				break;
		if(!(i<frequencies.length))
			return -1;
		
		return channels[i];
	}
	
	static public int chanToFreq(int chan) {
		if(chan<0 || chan>channels.length-1)
			return -1;
		return frequencies[chan];
	}	
	
	public boolean startScan() {
		// Only allow to enter scanning state IF idle
		if(!stateChange(State.SCANNING))
			return false;
				
		_monitor_thread = new ZigBeeScanner();
		_monitor_thread.execute(this);
		
		return true;
	}

	public static byte[] parseMacAddress(String macAddress)
    {
        String[] bytes = macAddress.split(":");
        byte[] parsed = new byte[bytes.length];

        for (int x = 0; x < bytes.length; x++)
        {
            BigInteger temp = new BigInteger(bytes[x], 16);
            byte[] raw = temp.toByteArray();
            parsed[x] = raw[raw.length - 1];
        }
        return parsed;
    }
	
	public static BigInteger parseMacStringToBigInteger(String macAddress)
	{
		String newMac = macAddress.replaceAll(":", "");
		BigInteger ret = new BigInteger(newMac, 16);  // 16 specifies hex
		return ret;
	}

}
