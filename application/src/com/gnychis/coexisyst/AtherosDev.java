package com.gnychis.coexisyst;

import java.io.InputStream;
import java.net.Socket;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.stericson.RootTools.RootTools;

public class AtherosDev {
	public static final int ATHEROS_CONNECT = 100;
	public static final int ATHEROS_DISCONNECT = 101;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	WifiMon _monitor_thread;
	static int WTAP_ENCAP_ETHERNET = 1;
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	
	
	public AtherosDev(CoexiSyst c) {
		coexisyst = c;
    	//coexisyst.system.cmd("cd /system/lib/modules\n");
    	coexisyst.system.cmd("insmod /system/lib/modules/cfg80211.ko");
    	coexisyst.system.cmd("insmod /system/lib/modules/crc7.ko");
    	coexisyst.system.cmd("insmod /system/lib/modules/mac80211.ko");
    	coexisyst.system.cmd("insmod /system/lib/modules/zd1211rw.ko");
    	coexisyst.system.cmd("cd /system/etc/firmware");
    	coexisyst.system.cmd("busybox unzip /data/data/com.gnychis.coexisyst/bin/zd_firmware.zip");
	}
	
	public void connected() {
		_device_connected=true;
		try {
			RootTools.sendShell("netcfg wlan0 down");
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/bin/iwconfig wlan0 mode monitor");
			RootTools.sendShell("/data/data/com.gnychis.coexisyst/bin/iwconfig wlan0 channel 6");
			RootTools.sendShell("netcfg wlan0 up");
		} catch(Exception e) {
			Log.e("WiFiMonitor", "Error running commands for connect atheros device", e);
		}
		
		coexisyst.ath._monitor_thread = new WifiMon();
		coexisyst.ath._monitor_thread.execute(coexisyst);	}
	
	public void disconnected() {
		_device_connected=false;
		coexisyst.ath._monitor_thread.cancel(true);

	}
	
	protected class WifiMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		SubSystem wifi_subsystem;
		Socket skt;
		private int PCAPD_WIFI_PORT = 2000;
		InputStream skt_in;
		private static final String WIMON_TAG = "WiFiMonitor";
		private int PCAP_HDR_SIZE = 16;
		Pcapd pcap_thread;
		int parsed;
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			Log.d(WIMON_TAG, "a new Wifi monitor thread was started");
			parsed=0;
			
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
				PcapHeader header = null;
				byte[] rawHeader, rawData;

				// Pull in the raw header and then cast it to a PcapHeader in JNetPcap
				rawHeader = getPcapHeader();  // get a header over the socket
				try {
					header = new PcapHeader();
					JBuffer headerBuffer = new JBuffer(rawHeader);  
					header.peer(headerBuffer, 0);				
				} catch(Exception e) {
					Log.e("WifiMon", "exception trying to read pcap header",e);
				}
				
				//Log.d("WifiMon", "PCAP Header size: " + Integer.toString(header.wirelen()));
				
				// Get the raw data now from the wirelen in the pcap header
				rawData = getPcapPacket(header.wirelen());
				
				// Get the value from a wireshark dissection
				if(parsed==0) {
					int dissect_ptr = coexisyst.dissectPacket(rawHeader, rawData, WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);
					String rval = coexisyst.wiresharkGet(dissect_ptr, "radiotap.channel.freq");
					Log.d("WifiMon", "Got value back from wireshark dissector: " + rval);
					coexisyst.dissectCleanup(dissect_ptr);
				}
				
				parsed++;
			}
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
		
		public byte[] getPcapPacket(int length) {
			byte[] rawdata = getSocketData(length);
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
			} catch(Exception e) { Log.e("WifiMon", "unable to read from pcapd buffer",e); }
			
			return data;
		}
	}
}
