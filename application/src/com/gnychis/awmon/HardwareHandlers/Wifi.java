package com.gnychis.awmon.HardwareHandlers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.Core.Packet;
import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.GUIs.MainInterface;
import com.gnychis.awmon.Scanners.WifiScanner;

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
public class Wifi extends InternalRadio {
	
	// This defines the device USB ID we are looking for
	class USBWifiDev {
		public static final int vendorID=0x13b1;
		public static final int productID=0x002f;
	}
		
	public static final int MS_SLEEP_UNTIL_PCAPD = 1500;
	
	// A non-CM9 device likely to use wlan0
	public static String _wlan_iface_name = "wlan1";
	public static String _moni_iface_name = "moni0";
	
	UserSettings _settings;
	WifiScanner _scan_thread;
	
	public boolean _device_connected;

	String _iw_phy;
	String _rxpackets_loc;
	
	public Wifi(Context c) {
		super();
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
		MainInterface.sendToastRequest(_parent, "Wifi device disconnected");
	}
	
	public boolean isConnected() { return _device_connected; }
	
	@Override
	public void leavingIdleState() {	
		BackgroundService.runCommand("netcfg moni0 up && netcfg wlan1 up");
	}
	
	@Override
	public void enteringIdleState() {	// Save power by bringing interfaces down If our interaction with the hardware is idle
		BackgroundService.runCommand("netcfg wlan1 down && netcfg moni0 down");
	}
	
	
	// The purpose of this thread is solely to initialize the Wifi hardware
	// that will be used for monitoring.
	protected class WifiInit extends AsyncTask<Context, Integer, String>
	{
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        MainInterface.sendProgressDialogRequest(_parent, "Initializing Wifi device..");
	    }
	    
		// Initialize the hardware
		@Override
		protected String doInBackground( Context ... params )
		{
			BackgroundService.runCommand("sh /data/data/" + MainInterface._app_name + "/files/init_wifi.sh " + MainInterface._app_name);
			MainInterface.sendToastRequest(_parent, "Wifi device initialized");
			try { Thread.sleep(100); } catch(Exception e) {}
			setFrequency(_wlan_iface_name, _settings.getHomeWifiFreq());
			return "true";
		}	
		
	    @Override
	    protected void onPostExecute(String result) {
	    	MainInterface.sendThreadMessage(_parent, MainInterface.ThreadMessages.CANCEL_PROGRESS_DIALOG);
	    	MainInterface.sendToastRequest(_parent, "Wifi device initialized");
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
		ArrayList<String> res = BackgroundService.runCommand("iwconfig " + ifname + " | grep Freq | awk '{print $2}' | awk -F':' '{print $2}'");
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
		BackgroundService.runCommand("/data/data/" + MainInterface._app_name + "/files/iw phy " + _iw_phy + " set channel " + Integer.toString(channel));
	}
	
	static public void setChannel(String ifname, int channel) {
		BackgroundService.runCommand("/data/data/" + MainInterface._app_name + "/files/iw dev " + ifname + " set channel " + Integer.toString(channel));
	}
	
	static public void setFrequency(String ifname, int frequency) {
		BackgroundService.runCommand("/data/data/" + MainInterface._app_name + "/files/iw dev " + ifname + " set freq " + Integer.toString(frequency));
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
	
	public static boolean validClientAddress(String MAC) {
		if(MAC==null || MAC.equals("ff:ff:ff:ff:ff:ff") || MAC.equals("00:00:00:00:00:00"))
			return false;
		return true;
	}
    
    // The purpose of this function is to take an 802.11 packet and return a list
    // of all addresses in the packet that are confirmed to be true wireless clients.
    // It means that the MAC address is guaranteed to be a true wireless client, and
    // not a client wired to the wireless AP.
    public static List<String> getWirelessAddresses(Packet p) {
    	List<String> wirelessAddresses = new ArrayList<String>();
 
    	// First, the true transmitter is definitely a wireless client
    	String transmitter_addr = getTransmitterAddress(p);
    	if(transmitter_addr!=null && validClientAddress(transmitter_addr))
    		wirelessAddresses.add(transmitter_addr);
    	
    	// Next, the BSSID is always a wireless "client"
    	String wlan_bssid = p.getField("wlan.bssid");
    	if(wlan_bssid != null && validClientAddress(wlan_bssid) && !wirelessAddresses.contains(wlan_bssid))
    		wirelessAddresses.add(wlan_bssid);
    	
    	// If there was a receiver address (wlan.sa), that is the recipient of an ACK
    	// or a management frame, so they must also be a wireless client.
    	String receiver_addr = p.getField("wlan.ra");
    	if(receiver_addr != null && validClientAddress(wlan_bssid) && !wirelessAddresses.contains(receiver_addr))
    		wirelessAddresses.add(receiver_addr);
    	
    	// Note that we don't have to check for (wlan.ta) because if wlan.ta was
    	// in the packet, getTransmitterAddress() will return it.
    	
    	return wirelessAddresses;
    }
    
    // The purpose of this function is to take an 802.11 packet and determine who
    // the transmitter of the packet was.  This includes inspecting the DS status
    // and allows us to associate the RSSI of a packet with who actually sent it.
    // The order of these heuristics matter, don't re-order without understanding.
    public static String getTransmitterAddress(Packet p) {
    	
    	if(p==null)
    		return null;
    	
    	String transmitter_addr = p.getField("wlan.ta");
    	String receiver_addr = p.getField("wlan.ra");
    	String wlan_sa = p.getField("wlan.sa");
    	String wlan_bssid = p.getField("wlan.bssid");
    	String ds_status = p.getField("wlan.fc.ds");
    	
    	if(transmitter_addr=="ff:ff:ff:ff:ff:ff" || transmitter_addr=="00:00:00:00:00:00")
    		transmitter_addr=null;
    	if(receiver_addr=="ff:ff:ff:ff:ff:ff" || receiver_addr=="00:00:00:00:00:00")
    		receiver_addr=null;
    	if(wlan_sa=="ff:ff:ff:ff:ff:ff" || wlan_sa=="00:00:00:00:00:00")
    		wlan_sa=null;
    	if(wlan_bssid=="ff:ff:ff:ff:ff:ff" || wlan_bssid=="00:00:00:00:00:00")
    		wlan_bssid=null;
    	
    	// If the packet has a receiver address but no transmitter address, it is an
    	// ACK or a CTS usually, and without some form of logic graph (e.g., JigSaw)
    	// we don't determine who the transmitter was.
    	if(receiver_addr!=null && transmitter_addr==null)
    		return null;

    	// The first heuristic is if there is a wlan.ta, a true transmitter address.
    	if(transmitter_addr!=null)
    		return transmitter_addr;
    	
    	// If the DS status is "0x00" (i.e., To DS: 0 and From DS: 0), then it is typically a mangement
    	// frame and the source address is definitely the transmitter.
    	if(ds_status=="0x00")
    		return wlan_sa;
    	
    	// If the DS status is "0x01" (i.e., To DS: 1 and From DS: 0), then it means it was a frame from
    	// a station (i.e., a true wireless client) which is the source.
    	if(ds_status=="0x01")
    		return wlan_sa;
    	
    	// If the DS status is "0x02" (i.e., To DS: 0 and From DS: 1), the AP is relaying
    	// the packet, so the bssid is the source transmitter.
    	if(ds_status=="0x02")
    		return wlan_bssid;

    	return null;  // we have no clue
    }
}
