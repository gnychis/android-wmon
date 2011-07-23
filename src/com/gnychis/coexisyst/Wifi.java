package com.gnychis.coexisyst;

import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;
import com.stericson.RootTools.RootTools;

public class Wifi {
	private static final String TAG = "AtherosDev";

	public static final int ATHEROS_CONNECT = 100;
	public static final int ATHEROS_DISCONNECT = 101;
	public static final String WIFI_SCAN_RESULT = "com.gnychis.coexisyst.WIFI_SCAN_RESULT";
	public static final int MS_SLEEP_UNTIL_PCAPD = 5000;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	WifiMon _monitor_thread;
	protected ChannelScanner _cscan_thread;
	
	static int WTAP_ENCAP_ETHERNET = 1;
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	
	WifiState _state;
	private Semaphore _state_lock;
	public enum WifiState {
		IDLE,
		SCANNING,
	}
	
	ArrayList<Packet> _scan_results;
	
	// http://en.wikipedia.org/wiki/List_of_WLAN_channels
	int[] channels24 = {1,2,3,4,5,6,7,8,9,10,11};
	int[] channels5 = {36,40,44,48,52,56,60,64,100,104,108,112,116,136,140,149,153,157,161,165};
	int scan_period = 110; // time to sit on each channel, in milliseconds
						   // 110 is to catch the 100ms beacon interval
	
	// Set the state to scan and start to switch channels
	public boolean APScan() {
		
		// Only allow to enter scanning state IF idle
		if(!WifiStateChange(WifiState.SCANNING))
			return false;
		
		_scan_results.clear();
		
		_cscan_thread = new ChannelScanner();
		_cscan_thread.execute();
		
		return true;  // in scanning state, and channel hopping
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
	
	public void disconnected() {
		_device_connected=false;
		coexisyst.ath._monitor_thread.cancel(true);
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
			try {
				// The AR9280 needs to have its firmware written when inserted, which is not automatic
				// FIXME: need to dynamically find the usb device id
				
				// Find the location of the "loading" register in the filesystem to alert the hardware
				// that the firmware is going to be loaded.
				String load_loc;
				while(true) {
					load_loc = compatLoading();
					if(!load_loc.equals(""))
						break;
				}
				
				// Write a "1" to notify of impending firmware write
				while(!compatIsLoading(load_loc))
					RootTools.sendShell("echo 1 > " + load_loc);
				
				// Write the firmware to the appropriate location
				String firmware_loc = load_loc.substring(0, load_loc.length()-7);
				RootTools.sendShell("cat /data/data/com.gnychis.coexisyst/files/htc_7010.fw > " + firmware_loc + "data");
				
				// Notify that we are done writing the firmware
				while(compatIsLoading(load_loc))
					RootTools.sendShell("echo 0 > " + load_loc);
				
				// Wait for the firmware to settle, and device interface to pop up
				while(!wlan0_exists())
					Thread.sleep(100);
					
				while(!wlan0_down()) {
					RootTools.sendShell("netcfg wlan0 down");
					Thread.sleep(100);
				}
				
				while(!wlan0_monitor()) {
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 mode monitor");
					Thread.sleep(100);
				}
				
				RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel 6");
				
				while(!wlan0_up()) {
					RootTools.sendShell("netcfg wlan0 up");
					Thread.sleep(100);
				}
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

				// Pull in the raw header and then cast it to a PcapHeader in JNetPcap
				if((rpkt._rawHeader = getPcapHeader())==null) {
					pcap_thread.cancel(true);
					return "error reading pcap header";
				}
								
				// Get the raw data now from the wirelen in the pcap header
				if((rpkt._rawData = getPcapPacket(rpkt._rawHeader))==null) {
					pcap_thread.cancel(true);
					return "error reading data";
				}
				
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
					rpkt.dissect();
					if(rpkt.getField("wlan_mgt.fixed.beacon")!=null)
						_scan_results.add(rpkt);
					
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
	
	////////////////////////////////////////////////////////////////////////////////
	// ChannelScanner: a class which instantiates a new thread to issues commands
	//     which changes the channel of the Atheros card.  This allows packet
	//     capture to continue smoothly, as the channel hops in the background.
	protected class ChannelScanner extends AsyncTask<Integer, Integer, String>
	{
		private static final String TAG = "WiFiChannelManager";

		
		@Override
		protected String doInBackground( Integer ... params )
		{
			Log.d(TAG, "a new Wifi channel manager thread was started");
			
			try {
				// For each of the channels, go through and scan
				for(int i=0; i<channels24.length; i++) {
					int c = channels24[i];
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel " + Integer.toString(c));
					Log.d(TAG, "Hopping to channel " + Integer.toString(c));
					Thread.sleep(scan_period);
				}
				
				for(int i=0; i<channels5.length; i++) {
					int c = channels5[i];
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel " + Integer.toString(c));
					Log.d(TAG, "Hopping to channel " + Integer.toString(c));

					Thread.sleep(scan_period);
				}
			} catch(Exception e) {
				Log.e(TAG, "error trying to scan channels", e);
			}
			
			// Alerts the main thread that the scanning has stopped, by changing the state and
			// saving the relevant data
			if(APScanStop())
				return "OK";
			else
				return "FAIL";
		}
	}
	
}
