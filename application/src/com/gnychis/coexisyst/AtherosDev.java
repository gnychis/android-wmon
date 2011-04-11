package com.gnychis.coexisyst;

import java.io.InputStream;
import java.net.Socket;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class AtherosDev {
	public static final int ATHEROS_CONNECT = 100;
	public static final int ATHEROS_DISCONNECT = 101;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	WifiMon _monitor_thread;
	
	
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
		coexisyst.system.cmd("netcfg wlan0 down");
		coexisyst.system.local_cmd("iwconfig wlan0 mode monitor");
		coexisyst.system.cmd("netcfg wlan0 up");
		
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
			try { Thread.sleep(1000); } catch (Exception e) {} // give some time for the process
			Log.d(WIMON_TAG, "launched pcapd");
			
			if(connectToPcapd() == false)
				return "FAIL";
			
			// Loop and read headers and packets
			while(true) {
				
				PcapHeader header = getPcapHeader();  // get a header over the socket

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
		
		public PcapHeader getPcapHeader() {
			PcapHeader header = null;
			byte[] rawph = new byte[PCAP_HDR_SIZE];
			int v=0;
			try {
				int total=0;
				while(total < PCAP_HDR_SIZE) {
					v = skt_in.read(rawph, total, PCAP_HDR_SIZE-total);
					Log.d("WifiMon", "Read in " + Integer.toString(v));
					if(v==-1)
						cancel(true);  // cancel the thread if we have errors reading socket
					total+=v;
				}
			} catch(Exception e) { Log.e("WifiMon", "unable to read from pcapd buffer",e); }
			
			try {
				header = new PcapHeader();
				JBuffer headerBuffer = new JBuffer(rawph);  
				header.peer(headerBuffer, 0);
				Log.d("WifiMon", "PCAP Header size: " + Integer.toString(header.wirelen()));
				//Log.d(TAG, "PCAP Header size: " + Integer.toString(header.wirelen()));
			} catch(Exception e) {
				Log.e("WifiMon", "exception trying to read pcap header",e);
			}
			
			return header;
		}
	}
}
