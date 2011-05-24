package com.gnychis.coexisyst;

import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.stericson.RootTools.RootTools;

public class Wifi {
	public static final int ATHEROS_CONNECT = 100;
	public static final int ATHEROS_DISCONNECT = 101;
	public static final String PACKET_UPDATE = "com.gnychis.coexisyst.PACKET_UPDATE";
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	WifiMon _monitor_thread;
	protected ChannelScanner _cscan_thread;
	
	static int WTAP_ENCAP_ETHERNET = 1;
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	
	WifiState _state;
	Lock _state_lock;
	public enum WifiState {
		IDLE,
		SCANNING,
	}
	
	
	
	// http://en.wikipedia.org/wiki/List_of_WLAN_channels
	int[] channels24 = {1,2,3,4,5,6,7,8,9,10,11};
	int[] channels5 = {36,40,44,48,52,56,60,64,100,104,108,112,116,136,140,149,153,157,161,165};
	int scan_period = 110; // time to sit on each channel, in milliseconds
						   // 110 is to catch the 100ms beacon interval
	
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
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel " + Integer.toString(i));
					Thread.sleep(scan_period);
				}
				
				for(int i=0; i<channels5.length; i++) {
					RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel " + Integer.toString(i));
					Thread.sleep(scan_period);
				}
			} catch(Exception e) {
				Log.e(TAG, "error trying to scan channels", e);
			}
			
			return "OK";
		}
	}
	
	// Set the state to scan and start to switch channels
	public boolean APScan() {
		
		// Only allow to enter scanning state IF idle
		if(!WifiStateChange(WifiState.SCANNING))
			return false;
		
		_cscan_thread = new ChannelScanner();
		_cscan_thread.execute();
		
		return true;  // in scanning state, and channel hopping
	}
	
	// Attempts to change the current state, will return
	// the state after the change if successful/failure
	public boolean WifiStateChange(WifiState s) {
		boolean res = false;
		if(_state_lock.tryLock()) {
			try {
				// Can add logic here to only allow certain state changes
				switch(_state) {
				
				// From the IDLE state, we can go anywhere...
				case IDLE:
					_state = s;
					res = true;
				break;
				
				// We can go to idle, or ignore if we are in a
				// scan already.
				case SCANNING:
					if(_state==WifiState.IDLE) {
						_state = s;
						res = true;
					} else if(_state==WifiState.SCANNING) {
						res = false;
					}
				break;
				
				default:
					res = false;
				}
			} finally {
				_state_lock.unlock();
			}
		} 		
		
		return res;
	}
	
	public Wifi(CoexiSyst c) {
		coexisyst = c;
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
		try {
			// The AR9280 needs to have its firmware written when inserted, which is not automatic
			// FIXME: need to dynamically find the usb device id
			Thread.sleep(1000);
			RootTools.sendShell("echo 1 > /sys/devices/platform/musb_hdrc/usb3/3-1/3-1.1/compat_firmware/3-1.1/loading");
			RootTools.sendShell("cat /data/data/com.gnychis.coexisyst/files/htc_7010.fw > /sys/devices/platform/musb_hdrc/usb3/3-1/3-1.1/compat_firmware/3-1.1/data");
			RootTools.sendShell("echo 0 > /sys/devices/platform/musb_hdrc/usb3/3-1/3-1.1/compat_firmware/3-1.1/loading");
			Thread.sleep(1000);
			RootTools.sendShell("netcfg wlan0 down");
			RootTools.sendShell("netcfg wlan0 down");
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 mode monitor");
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 mode monitor");
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig wlan0 channel 6");
			RootTools.sendShell("netcfg wlan0 up");
		} catch(Exception e) {
			Log.e("WiFiMonitor", "Error running commands for connect atheros device", e);
		}
		
		coexisyst.ath._monitor_thread = new WifiMon();
		coexisyst.ath._monitor_thread.execute(coexisyst);
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
		private int PCAPD_WIFI_PORT = 2000;
		InputStream skt_in;
		private static final String WIMON_TAG = "WiFiMonitor";
		private int PCAP_HDR_SIZE = 16;
		Pcapd pcap_thread;
		
		@Override 
		protected void onPreExecute( )
		{
			_state = WifiState.IDLE;
		}
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			Log.d(WIMON_TAG, "a new Wifi monitor thread was started");
			
			// Attempt to create capture process spawned in the background
			// which we will connect to for pcap information.
			pcap_thread = new Pcapd();
			pcap_thread.execute(coexisyst);
			try { Thread.sleep(5000); } catch (Exception e) {} // give some time for the process
			Log.d(WIMON_TAG, "launched pcapd");
			
			if(connectToPcapd() == false)
				return "FAIL";
			
			// Loop and read headers and packets
			while(true) {
				byte[] rawHeader, rawData;

				// Pull in the raw header and then cast it to a PcapHeader in JNetPcap
				if((rawHeader = getPcapHeader())==null) {
					pcap_thread.cancel(true);
					return "error reading pcap header";
				}
								
				// Get the raw data now from the wirelen in the pcap header
				if((rawData = getPcapPacket(rawHeader))==null) {
					pcap_thread.cancel(true);
					return "error reading data";
				}
				
				// Based on the state of our wifi thread, we determine what to do with the packet
				switch(_state) {
				
				case IDLE:
					dissectAll(rawHeader, rawHeader);
					break;
					
				case SCANNING:
					break;
				}
				
				
				// Send out a broadcast with the packet
				//Intent intent = new Intent(PACKET_UPDATE);
				//intent.putExtra("header",rawHeader);
				//intent.putExtra("data", rawData);
				//intent.putExtra("encap", WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);
				//parent.sendBroadcast(intent);
				
			}
		}
		
		public String[] dissectAll(byte[] rawHeader, byte[] rawData) {
			int dissect_ptr = coexisyst.dissectPacket(rawHeader, rawData, WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);
			Log.d("Testing", "Dissected value: " + coexisyst.wiresharkGet(dissect_ptr, "radiotap.channel.freq"));
			String rval[] = coexisyst.wiresharkGetAll(dissect_ptr);
			coexisyst.wiresharkGetAllTest(dissect_ptr);
			coexisyst.dissectCleanup(dissect_ptr);
			return rval;
		}
		
		public boolean connectToPcapd() {
			// Attempt to connect to the socket via TCP for the PCAP info
			try {
				skt = new Socket("localhost", PCAPD_WIFI_PORT);
			} catch(Exception e) {
				Log.e(WIMON_TAG, "exception trying to connect to wifi socket for pcap", e);
				return false;
			}
			
			try {
				skt_in = skt.getInputStream();
			} catch(Exception e) {
				Log.e(WIMON_TAG, "exception trying to get inputbuffer from socket stream");
				return false;
			}
			Log.d(WIMON_TAG, "successfully connected to pcapd");
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
