package com.gnychis.coexisyst;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;
import com.stericson.RootTools.RootTools;

public class Wifi {
	private static final String TAG = "AtherosDev";

	public static final int ATHEROS_CONNECT = 100;
	public static final int ATHEROS_DISCONNECT = 101;
	public static final String WIFI_SCAN_RESULT = "com.gnychis.coexisyst.WIFI_SCAN_RESULT";
	public static final int MS_SLEEP_UNTIL_PCAPD = 1500;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	WifiMon _monitor_thread;
	
	static int WTAP_ENCAP_ETHERNET = 1;
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	
	WifiState _state;
	private Semaphore _state_lock;
	public enum WifiState {
		IDLE,
		SCANNING,
	}
	
	ArrayList<Packet> _scan_results;
	private int _scan_channel;
	private Timer _scan_timer;
	
	// http://en.wikipedia.org/wiki/List_of_WLAN_channels
	static int[] channels = {1,2,3,4,5,6,7,8,9,10,11,36,40,44,48,52,56,60,64,100,104,108,112,116,136,140,149,153,157,161,165};
	static int[] frequencies = {2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462,5180, 5200, 5220, 5240, 5260, 5280, 5300, 5320, 
		5500, 5520, 5540, 5560, 5580, 5680, 5700, 5745, 5765, 5785, 5805, 5825};

	// Take an 802.11 channel number, get a frequency in KHz
	static int channelToFreq(int chan) {
		int index=-1;
		int i;
		
		for(i=0;i<channels.length;i++)
			if(channels[i]==chan)
				index=i;
		
		if(index==-1)
			return -1;
		
		return frequencies[index];
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
	
	// Set the state to scan and start to switch channels
	public boolean APScan() {
		
		// Only allow to enter scanning state IF idle
		if(!WifiStateChange(WifiState.SCANNING))
			return false;
		
		_scan_results.clear();
		
		_scan_channel=0;
		setChannel(channels[_scan_channel]);
		_scan_timer = new Timer();
		_scan_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				scanIncrement();
			}

		}, 0, 210);
		
		return true;  // in scanning state, and channel hopping
	}
	
	// Keep incrementing, but let the timer run an additional 5 times to catch packets on the last
	// several channels before we leave the "SCANNING" state (in which we save packets)
	public static int _additional_ticks = 75;
	private void scanIncrement() {
		_scan_channel++;
		if(_scan_channel<channels.length) {
			Log.d(TAG, "Incrementing channel to:" + Integer.toString(channels[_scan_channel]));
			setChannel(channels[_scan_channel]);
			Log.d(TAG, "... increment complete!");
			_monitor_thread.sendMainMessage(ThreadMessages.INCREMENT_PROGRESS);
		}
		else if(_scan_channel<channels.length+_additional_ticks) {
			// Do nothing, just keep receiving
			_monitor_thread.sendMainMessage(ThreadMessages.INCREMENT_PROGRESS);
		}
		else { // finally, give up!
			APScanStop();
			_scan_timer.cancel();
			return;
		}
		
	}
	
	public boolean APScanStop() {
		// Need to return the state back to IDLE from scanning
		if(!WifiStateChange(WifiState.IDLE)) {
			Log.d(TAG, "Failed to change from scanning to IDLE");
			return false;
		}
		
		// Now, send out a broadcast with the results
		Intent i = new Intent();
		i.setAction(WIFI_SCAN_RESULT);
		i.putExtra("packets", _scan_results);
		coexisyst.sendBroadcast(i);
				
		// Send a message to stop the spinner if it is running
		//Message msg 99999999999999999999999= new Message();
		//msg.obj = ThreadMessages.WIFI_SCAN_COMPLETE;
		//coexisyst.handler.sendMessage(msg);
		
		return true;
	}
	
	// Attempts to change the current state, will return
	// the state after the change if successful/failure
	public boolean WifiStateChange(WifiState s) {
		boolean res = false;
		if(_state_lock.tryAcquire()) {
			try {
				
				// Can add logic here to only allow certain state changes
				// Given a _state... then...
				switch(_state) {
				
				// From the IDLE state, we can go anywhere...
				case IDLE:
					Log.d(TAG, "Switching state from " + _state.toString() + " to " + s.toString());
					_state = s;
					res = true;
				break;
				
				// We can go to idle, or ignore if we are in a
				// scan already.
				case SCANNING:
					if(s==WifiState.IDLE) {  // cannot go directly to IDLE from SCANNING
						Log.d(TAG, "Switching state from " + _state.toString() + " to " + s.toString());
						_state = s;
						res = true;
					} else if(s==WifiState.SCANNING) {  // ignore an attempt to switch in to same state
						res = false;
					} 
				break;
				
				default:
					res = false;
				}
				
			} finally {
				_state_lock.release();
			}
		} 		
		
		return res;
	}
	
	public Wifi(CoexiSyst c) {
		_state_lock = new Semaphore(1,true);
		_scan_results = new ArrayList<Packet>();
		coexisyst = c;
		_state = WifiState.IDLE;
		try {
			// All modules related to ath9k_htc that need to be inserted
			RootTools.sendShell("insmod /system/lib/modules/compat.ko");
			RootTools.sendShell("insmod /system/lib/modules/compat_firmware_class.ko");
			RootTools.sendShell("insmod /system/lib/modules/cfg80211.ko");
			RootTools.sendShell("insmod /system/lib/modules/mac80211.ko");
			RootTools.sendShell("insmod /system/lib/modules/ath.ko");
			RootTools.sendShell("insmod /system/lib/modules/ath9k_hw.ko");
			RootTools.sendShell("insmod /system/lib/modules/ath9k_common.ko");
			RootTools.sendShell("insmod /system/lib/modules/ath9k_htc.ko");
			Log.d("AtherosDev", "Inserted kernel modules");
		} catch (Exception e) {
			Log.e("AtherosDev", "Error running shell commands for atheros dev", e);
		}
	}
	
	public void connected() {
		_device_connected=true;
		coexisyst.ath._monitor_thread = new WifiMon();
		coexisyst.ath._monitor_thread.execute(coexisyst);
	}
	
	public boolean isConnected() {
		return _device_connected;
	}
	
	public String compatLoading() {
		try {
			List<String> res = RootTools.sendShell("busybox find /sys -name loading");
			return res.get(0);
		} catch (Exception e) { return ""; }	
	}
	public boolean compatIsLoading(String loc) {
		try {
			List<String> res = RootTools.sendShell("cat " + loc);
			
			if(Integer.parseInt(res.get(0))==1)
				return true;
			else
				return false;
		} catch (Exception e) { return false; }	
	}
	public boolean wlan0_monitor() {
		try {
			List<String> res = RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 | busybox grep Monitor");
			if(res.size()!=0)
				return true;
		} catch (Exception e) { return false; }	

		return false;		
	}
	public boolean wlan0_up() {
		try {
			List<String> res = RootTools.sendShell("netcfg | busybox grep \"^wlan0\" | busybox grep UP");
			if(res.size()!=0)
				return true;
		} catch (Exception e) { return false; }	

		return false;		
	}
	public boolean wlan0_down() {
		try {
			List<String> res = RootTools.sendShell("netcfg | busybox grep \"^wlan0\" | busybox grep DOWN");
			if(res.size()!=0)
				return true;
		} catch (Exception e) { return false; }	

		return false;		
	}
	public boolean wlan0_exists() {
		try {
			List<String> res = RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0");
			if(res.size()>1)
				return true;
		} catch (Exception e) { return false; }	

		return false;
	}
	
	public void setChannel(int channel) {
		try {
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel " + Integer.toString(channel));
		} catch(Exception e) {
			Log.e(TAG, "exception trying to handle setChannel", e);
		}
	}
	
	public void disconnected() {
		_device_connected=false;
		coexisyst.ath._monitor_thread.cancel(true);
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

	
	protected class WifiMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		Socket skt;
		InputStream skt_in;
		private static final String WIMON_TAG = "WiFiMonitor";
		private int PCAP_HDR_SIZE = 16;
		Pcapd pcap_thread;
		
		// On pre-execute, we make sure that we initialize the card properly and set the state to IDLE
		@Override 
		protected void onPreExecute( )
		{
			_state = WifiState.IDLE;			
		}
		
		// Initializes the Atheros hardware strictly.  First writes the firmware, then sets the
		// interface to be in monitor mode
		protected void initAtherosCard() {
			// The AR9280 needs to have its firmware written when inserted, which is not automatic
			// FIXME: need to dynamically find the usb device id
			
			// Only initialize if it is not already initialized
			if(wlan0_exists() && wlan0_up() && wlan0_monitor()) {
				Log.d(TAG, "Atheros device is already connected and initialized...");
				return;
			}
			
			
			
			// Find the location of the "loading" register in the filesystem to alert the hardware
			// that the firmware is going to be loaded.
			String load_loc;
			while(true) {
				load_loc = compatLoading();
				if(!load_loc.equals(""))
					break;
				trySleep(100);
			}
			Log.d(TAG, "Found loading location at: " + load_loc);
			
			// Write a "1" to notify of impending firmware write
			while(!compatIsLoading(load_loc)) {
				try {
					RootTools.sendShell("echo 1 > " + load_loc);
				} catch(Exception e) { Log.e(TAG, "error writing to RootTools", e); } 
			}
			Log.d(TAG, "Wrote notification of impending firmware write");
			
			// Write the firmware to the appropriate location
			String firmware_loc = load_loc.substring(0, load_loc.length()-7);
			try{
			RootTools.sendShell("cat /data/data/com.gnychis.coexisyst/files/htc_7010.fw > " + firmware_loc + "data");
			} catch(Exception e) { Log.e(TAG, "that is bad... not going to load firmware, fail", e); } 
			Log.d(TAG, "Wrote the firmware to the device");
			
			// Notify that we are done writing the firmware
			while(compatIsLoading(load_loc)) {
				try {
					RootTools.sendShell("echo 0 > " + load_loc);
				} catch(Exception e) { Log.e(TAG, "error writing to RootTools", e); } 
			}
			Log.d(TAG, "Notify of firmware complete");
			
			// Wait for the firmware to settle, and device interface to pop up
			while(!wlan0_exists()) {
				trySleep(100);
			}
			Log.d(TAG, "wlan0 now exists");
				
			while(!wlan0_down()) {
				try {
					RootTools.sendShell("netcfg wlan0 down");
				} catch(Exception e) { Log.e(TAG, "error writing to RootTools", e); } 
				trySleep(100);
			}
			Log.d(TAG, "interface has been taken down");
			
			while(!wlan0_monitor()) {
				try {
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 mode monitor");
				} catch(Exception e) { Log.e(TAG, "error writing to RootTools", e); } 
				trySleep(100);
			}
			Log.d(TAG, "interface set to monitor mode");
			
			try {
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel 6");
			} catch(Exception e) { Log.e(TAG, "error writing to RootTools", e); } 
			Log.d(TAG, "channel initialized");
			
			while(!wlan0_up()) {
				try {
					RootTools.sendShell("netcfg wlan0 up");
				} catch(Exception e) {  Log.e(TAG, "error writing to RootTools", e); } 
				trySleep(100);
			}			
			Log.d(TAG, "interface is now up");
		}
		
		public void trySleep(int length) {
			try {
				Thread.sleep(length);
			} catch(Exception e) {
				Log.e("WiFiMonitor", "Error running commands for connect atheros device", e);
			}
		}
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(CoexiSyst.ThreadMessages t) {
			Message msg = new Message();
			msg.obj = t;
			coexisyst._handler.sendMessage(msg);
		}
		
		// The entire meat of the thread, pulls packets off the interface and dissects them
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			
			// Initialize the Atheros hardware
			initAtherosCard();

			// Connect to the pcap daemon to pull packets from the hardware
			if(connectToPcapd() == false) {
				Log.d(TAG, "failed to connect to the pcapd daemon, doh");
				sendMainMessage(ThreadMessages.ATHEROS_FAILED);
				return "FAIL";
			}
			sendMainMessage(ThreadMessages.ATHEROS_INITIALIZED);
						
			// Loop and read headers and packets
			while(true) {
				Packet rpkt = new Packet(WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);

				if((rpkt._rawHeader = getPcapHeader())==null) {
					pcap_thread.cancel(true);
					return "error reading pcap header";
				}
				rpkt._headerLen = rpkt._rawHeader.length;
								
				// Get the raw data now from the wirelen in the pcap header
				if((rpkt._rawData = getPcapPacket(rpkt._rawHeader))==null) {
					pcap_thread.cancel(true);
					return "error reading data";
				}
				rpkt._dataLen = rpkt._rawData.length;
				
				// Based on the state of our wifi thread, we determine what to do with the packet
				switch(_state) {
				
				case IDLE:
					break;
				
				// In the scanning state, we save all beacon frames as we hop channels (handled by a
				// separate thread).
				case SCANNING:
					// To identify beacon: wlan_mgt.fixed.beacon is set.  If it is a beacon, add it
					// to our scan result.  This does not guarantee one beacon frame per network, but
					// pruning can be done at the next level.
					if(rpkt.getField("wlan_mgt.fixed.beacon")!=null) {
						Log.d(TAG, "Found 802.11 network: " + rpkt.getField("wlan_mgt.ssid") + " on channel " + rpkt.getField("wlan_mgt.ds.current_channel"));
						_scan_results.add(rpkt);
					}
					
					break;
				}
			}
		}
		
		public boolean connectToPcapd() {
			
			// Generate a random port for Pcapd
			Random generator = new Random();
			int pcapd_port = 2000 + generator.nextInt(500);
			
			Log.d(WIMON_TAG, "a new Wifi monitor thread was started");
			
			// Attempt to create capture process spawned in the background
			// which we will connect to for pcap information.
			pcap_thread = new Pcapd(pcapd_port);
			pcap_thread.execute(coexisyst);
			
			// Send a message to block the main dialog after the card is done initializing
			try { Thread.sleep(MS_SLEEP_UNTIL_PCAPD); } catch (Exception e) {} // give some time for the process
			
			// Attempt to connect to the socket via TCP for the PCAP info
			try {
				skt = new Socket("localhost", pcapd_port);
			} catch(Exception e) {
				Log.e(WIMON_TAG, "exception trying to connect to wifi socket for pcap on " + Integer.toString(pcapd_port), e);
				return false;
			}
			
			try {
				skt_in = skt.getInputStream();
			} catch(Exception e) {
				Log.e(WIMON_TAG, "exception trying to get inputbuffer from socket stream");
				return false;
			}
			Log.d(WIMON_TAG, "successfully connected to pcapd on port " + Integer.toString(pcapd_port));
			return true;
		}
		
		public byte[] getPcapHeader() {
			byte[] rawdata = getSocketData(PCAP_HDR_SIZE);
			return rawdata;
		}
		
		public byte[] getPcapPacket(byte[] rawHeader) {
			byte[] rawdata;
			PcapHeader header = null;

			try {
				header = new PcapHeader();
				JBuffer headerBuffer = new JBuffer(rawHeader);  
				header.peer(headerBuffer, 0);				
			} catch(Exception e) {
				Log.e("WifiMon", "exception trying to read pcap header",e);
			}
			
			rawdata = getSocketData(header.wirelen());
			return rawdata;
		}
		
		public byte[] getSocketData(int length) {
			byte[] data = new byte[length];
			int v=0;
			try {
				int total=0;
				while(total < length) {
					v = skt_in.read(data, total, length-total);
					//Log.d("WifiMon", "Read in " + Integer.toString(v) + " - " + Integer.toString(total+v) + " / " + Integer.toString(length));
					if(v==-1)
						cancel(true);  // cancel the thread if we have errors reading socket
					total+=v;
				}
			} catch(Exception e) { 
				Log.e("WifiMon", "unable to read from pcapd buffer",e);
				return null;
			}
			
			return data;
		}
	}	
}
