package com.gnychis.awmon.DeviceHandlers;

import java.math.BigInteger;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.DeviceScanners.WifiDeviceScanner;

/* 
 * record received packets
set state to SCANNING
initialize number of packets to -1
initiate a non-blocking scan
in the thread, count up the number of received packets while in SCANNING
after 5 seconds record the received packets again
keep receiving packets in thread, counting up
if(num_pkts==-1) then just keep going
once num_pkts != -1, and read>num_pkts, then leave scanning thread

bingo.
 */
public class Wifi extends HardwareDevice {
	
	// This defines the device USB ID we are looking for
	class USBWifiDev {
		public static final int vendorID=0x13b1;
		public static final int productID=0x002f;
	}
		
	public static final int MS_SLEEP_UNTIL_PCAPD = 1500;
	
	// A non-CM9 device likely to use wlan0
	public static String _wlan_iface_name = "wlan1";
	
	UserSettings _settings;
	WifiDeviceScanner _scan_thread;
	
	public boolean _device_connected;

	String _iw_phy;
	String _rxpackets_loc;
	
	public Wifi(Context c) {
		super(HardwareDevice.Type.Wifi);
		_parent = c;
		_settings = new UserSettings(_parent);
		
		Log.d("WifiDev", "Inserted kernel modules");
		
		_parent.registerReceiver(usbUpdate, new IntentFilter(USBMon.USBMON_DEVICELIST));
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Related to the connection and disconnection of the USB device
	
    // Receives messages about USB devices
    private BroadcastReceiver usbUpdate = new BroadcastReceiver() {
    	@Override @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
        	ArrayList<String> devices = (ArrayList<String>) intent.getExtras().get("devices");
        	if(USBMon.isDeviceInList(devices, USBWifiDev.vendorID, USBWifiDev.productID)) {
        		if(!_device_connected)
        			connected();
        	} else {
        		if(_device_connected)
        			disconnected();
        	}
        }
    };  
    

	// When a wifi device is connected, spawn a thread which
	// initializes the hardware
	public void connected() {
		_device_connected=true;
		WifiInit init_thread = new WifiInit();
		init_thread.execute(_parent);
	}
	
	public void disconnected() {
		_device_connected=false;
		AWMon.sendToastRequest(_parent, "Wifi device disconnected");
	}
	
	public boolean isConnected() { return _device_connected; }
	
	
	// The purpose of this thread is solely to initialize the Wifi hardware
	// that will be used for monitoring.
	protected class WifiInit extends AsyncTask<Context, Integer, String>
	{
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        AWMon.sendProgressDialogRequest(_parent, "Initializing Wifi device..");
	    }
	    
		// Initialize the hardware
		@Override
		protected String doInBackground( Context ... params )
		{
			AWMon.runCommand("sh /data/data/" + AWMon._app_name + "/files/init_wifi.sh " + AWMon._app_name);
			AWMon.sendToastRequest(_parent, "Wifi device initialized");
			try { Thread.sleep(100); } catch(Exception e) {}
			setFrequency(_wlan_iface_name, _settings.getHomeWifiFreq());
			return "true";
		}	
		
	    @Override
	    protected void onPostExecute(String result) {
	    	AWMon.sendThreadMessage(_parent, AWMon.ThreadMessages.CANCEL_PROGRESS_DIALOG);
	    	AWMon.sendToastRequest(_parent, "Wifi device initialized");
	    }
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Functions for helping convert channels to frequencies
	// http://en.wikipedia.org/wiki/List_of_WLAN_channels
	public static int[] channels = {1,2,3,4,5,6,7,8,9,10,11,36,40,44,48,52,56,60,64,100,104,108,112,116,136,140,149,153,157,161,165};
	public static int[] frequencies = {2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462,
		5180, 5200, 5220, 5240, 5260, 5280, 5300, 5320,  5500, 5520, 5540, 5560, 5580, 5680, 5700, 5745, 5765, 5785, 5805, 5825};
	
	// Take an 802.11 channel number, get a frequency in KHz
	public static int channelToFreq(int chan) {
		int index=-1;
		int i;
		
		for(i=0;i<channels.length;i++)
			if(channels[i]==chan)
				index=i;
		
		if(index==-1)
			return -1;
		
		return frequencies[index];
	}
	
	static public int getOperationalFreq(String ifname) {
		ArrayList<String> res = AWMon.runCommand("iwconfig " + ifname + " | grep Freq | awk '{print $2}' | awk -F':' '{print $2}'");
		if(res.size()==0)
			return -1;
		else
			return Integer.parseInt(res.get(0).replace(".", ""));
	}
	
	// Take a frequency in KHz, get a channel!
	static int freqtoChannel(int freq) {
		int index=-1;
		int i;
		
		for(i=0;i<frequencies.length;i++)
			if(frequencies[i]==freq)
				index=i;
		
		if(index==-1)
			return -1;
		return channels[index];
	}
	
	public void setChannel(int channel) {
		AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw phy " + _iw_phy + " set channel " + Integer.toString(channel));
	}
	
	static public void setChannel(String ifname, int channel) {
		AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + ifname + " set channel " + Integer.toString(channel));
	}
	
	static public void setFrequency(String ifname, int frequency) {
		AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + ifname + " set freq " + Integer.toString(frequency));
	}

	public void trySleep(int length) {
		try {
			Thread.sleep(length);
		} catch(Exception e) {
			Log.e("WiFiMonitor", "Error running commands for connecting wifi device", e);
		}
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
