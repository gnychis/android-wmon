package com.gnychis.awmon.DeviceHandlers;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.AWMon.ThreadMessages;
import com.gnychis.awmon.Core.Packet;
import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.ScanReceivers.WiFiScanReceiver;

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
public class Wifi {
	private static final boolean VERBOSE = false;
	
	public static final boolean PCAP_DUMP = false;
	DataOutputStream _pcap_dump; 

	public static final int WIFIDEV_CONNECT = 100;
	public static final int WIFIDEV_DISCONNECT = 101;
	public static final String WIFI_SCAN_RESULT = AWMon._app_name + ".WIFI_SCAN_RESULT";
	public static final int MS_SLEEP_UNTIL_PCAPD = 1500;
	
	// A non-CM9 device likely to use wlan0
	public String _wlan_iface_name = "wlan1";
	
	AWMon coexisyst;
	UserSettings _settings;
	
	public boolean _device_connected;
	
	static int WTAP_ENCAP_ETHERNET = 1;
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	
	WifiState _state;
	private Semaphore _state_lock;
	public enum WifiState {
		IDLE,
		SCANNING,
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d("WifiDev", msg);
	}
	
	// All scan related variables.  There are 3 kinds of scans that I've come up.
	// There is native, which is when CoexiSyst modifies the channels and passively scans.
	// Then, when triggering scans on the card and listening for the probe responses,
	// it can be "one shot" which is a single timer triggers when done, or periodic updates
	// which are useful for updating the main thread on progress.  Also, active scans
	// are currently only implemented in non-native scanning methods.
	WifiScan _scan_thread;
	WiFiScanReceiver rcvr_80211;
	private static int SCAN_WAIT_TIME= 6000;  // in milliseconds
	private static int SCAN_UPDATE_TIME=250; // update every 250ms to the main thread
	public static int SCAN_WAIT_COUNTS=SCAN_WAIT_TIME/SCAN_UPDATE_TIME;
	public static boolean _native_scan=false;
	public static boolean _one_shot_scan=false;
	public static boolean _active_scan=true;
	private Timer _scan_timer;		// the timer which will fire to end the scan or update it
	ArrayList<Packet> _scan_results;

	String _iw_phy;
	String _rxpackets_loc;
	
	// http://en.wikipedia.org/wiki/List_of_WLAN_channels
	public static int[] channels = {1,2,3,4,5,6,7,8,9,10,11,36,40,44,48,52,56,60,64,100,104,108,112,116,136,140,149,153,157,161,165};
	public static int[] frequencies = {2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447, 2452, 2457, 2462,5180, 5200, 5220, 5240, 5260, 5280, 5300, 5320, 
		5500, 5520, 5540, 5560, 5580, 5680, 5700, 5745, 5765, 5785, 5805, 5825};
	
	
	public Wifi(AWMon c) {
		_state_lock = new Semaphore(1,true);
		_scan_results = new ArrayList<Packet>();
		coexisyst = c;
		_settings = new UserSettings(coexisyst);
		_state = WifiState.IDLE;
		
		Log.d("WifiDev", "Inserted kernel modules");
		
		// If we are dumping all of our packets for debugging...
		if(PCAP_DUMP) {
			try {
				_pcap_dump = new DataOutputStream(new FileOutputStream("/sdcard/coexisyst_wifi.pcap"));
				byte initialized_sequence[] = {0x67, 0x65, 0x6f, 0x72, 0x67, 0x65, 0x6e, 0x79, 0x63, 0x68, 0x69, 0x73};
				byte pcap_header[] = {(byte)0xd4, (byte)0xc3, (byte)0xb2, (byte)0xa1, 		// magic number
						(byte)0x02, (byte)0x00, (byte)0x04,(byte) 0x00, 	// version numbers
						(byte)0x00, (byte)0x00, (byte)0x00,(byte) 0x00, 	// thiszone
						(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 	// sigfigs
						(byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00, 	// snaplen
						(byte)0x7f, (byte)0x00, (byte)0x00, (byte)0x00};  	// wifi
				
				_pcap_dump.write(pcap_header);
			} catch(Exception e) { Log.e("WifiDev", "Error trying to write output stream", e); }
		}

	}
	
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
	
	public void trySleep(int length) {
		try {
			Thread.sleep(length);
		} catch(Exception e) {
			Log.e("WiFiMonitor", "Error running commands for connecting wifi device", e);
		}
	}
	
	// Set the state to scan and start to switch channels
	public boolean APScan() {
		
		// Only allow to enter scanning state IF idle
		if(!WifiStateChange(WifiState.SCANNING))
			return false;
		
		_scan_results.clear();
		
		// Native scanning relies on doing the actual scanning (channel hopping)
		// within CoexiSyst.  Non-native relies on the nl80211 driver to do the
		// channel hopping for us.  It's not clear which is better, so I opted for
		// tighter control as the default.
		_scan_thread = new WifiScan();
		_scan_thread.execute(coexisyst);
		
		debugOut("Waiting for scan thread to start");
		while(_scan_thread.getStatus()!=AsyncTask.Status.RUNNING)
			trySleep(100);
		debugOut("...finished!");
				
		
		return true;  // in scanning state, and channel hopping
	}
	
	public boolean APScanStop() {
				
		// Need to return the state back to IDLE from scanning
		if(!WifiStateChange(WifiState.IDLE)) {
			debugOut("Failed to change from scanning to IDLE");
			return false;
		}
		
		debugOut("Waiting for scan thread to stop");
		while(_scan_thread.getStatus()!=AsyncTask.Status.FINISHED)
			trySleep(100);
		debugOut("...finished!");
		
		trySleep(1000);
				
		// Now, send out a broadcast with the results
		Intent i = new Intent();
		i.setAction(WIFI_SCAN_RESULT);
		i.putExtra("packets", _scan_results);
		coexisyst.sendBroadcast(i);
		
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
					debugOut("Switching state from " + _state.toString() + " to " + s.toString());
					_state = s;
					res = true;
				break;
				
				// We can go to idle, or ignore if we are in a
				// scan already.
				case SCANNING:
					if(s==WifiState.IDLE) {  // cannot go directly to IDLE from SCANNING
						debugOut("Switching state from " + _state.toString() + " to " + s.toString());
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

	// When a wifi device is connected, spawn a thread which
	// initializes the hardware
	public void connected() {
		_device_connected=true;
		WifiInit init_thread = new WifiInit();
		init_thread.execute(coexisyst);
	}
	
	public boolean isConnected() {
		return _device_connected;
	}
	
	public void setChannel(int channel) {
		coexisyst.runCommand("/data/data/" + AWMon._app_name + "/files/iw phy " + _iw_phy + " set channel " + Integer.toString(channel));
	}
	
	static public void setChannel(String ifname, int channel) {
		AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw phy " + ifname + " set channel " + Integer.toString(channel));
	}
	
	static public void setFrequency(String ifname, int frequency) {
		AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw phy " + ifname + " set freq " + Integer.toString(frequency));
	}
	
	public void disconnected() {
		_device_connected=false;
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

	
	// The purpose of this thread is solely to initialize the Wifi hardware
	// that will be used for monitoring.
	protected class WifiInit extends AsyncTask<Context, Integer, String>
	{
		// Initialize the hardware
		@Override
		protected String doInBackground( Context ... params )
		{
			AWMon awmon = (AWMon) params[0];
			awmon.runCommand("sh /data/data/" + AWMon._app_name + "/files/init_wifi.sh " + AWMon._app_name);
			AWMon.sendMainMessage(awmon._handler, ThreadMessages.WIFIDEV_INITIALIZED);
			//setFrequency(_wlan_iface_name, );
			return "true";
		}		
	}
	
	// WARNING: Do not cancel(true) this task!  It is unsafe for it to be interrupted in
	// the middle of a dissection.  This will cause the application to segfault.
	protected class WifiScan extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		AWMon coexisyst;
		private int PCAP_HDR_SIZE = 16;
		private int _received_pkts;
		private PcapIf _moni0_dev;
		private Pcap _moni0_pcap;
		private int _timer_counts;		// to know how many timer ticks are left before scan over
		private int _scan_channel;		// to keep track of the channel when scanning natively
		
		public void trySleep(int length) {
			try {
				Thread.sleep(length);
			} catch(Exception e) {
				Log.e("WiFiMonitor", "Error running commands for connect wifi device", e);
			}
		}
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(AWMon.ThreadMessages t) {
			Message msg = new Message();
			msg.what = t.ordinal();
			coexisyst._handler.sendMessage(msg);
		}
		
		// Opens a the moni0 device as a pcap interface for packet capture
		public boolean openDev() {
			
			// Try to use jnetpcap straight up to open the interface
			List<PcapIf> alldevs = new ArrayList<PcapIf>();
			StringBuilder errbuf = new StringBuilder(); // For any error msgs  
			int r = Pcap.findAllDevs(alldevs, errbuf);  
			if (r == Pcap.NOT_OK || alldevs.isEmpty()) {  
	            debugOut("Can't read list of devices, error is " + errbuf.toString());  
	            sendMainMessage(ThreadMessages.WIFIDEV_FAILED);
	            return false;  
	        } 
			
			debugOut("Network devices found:");  
			int i = 0;  
	        for (PcapIf device : alldevs) {  
	            String description =  
	                (device.getDescription() != null) ? device.getDescription()  
	                    : "No description available";  
	            debugOut(Integer.toString(i) + ": " + device.getName() + "[" + description + "]");  
	            if(device.getName().equals("moni0"))
	            	break;
	            else
	            	i++;
	        }  
	        
	        if(i>=alldevs.size()) {
	        	sendMainMessage(ThreadMessages.WIFIDEV_FAILED);
	        	return false;
	        }
	        
	        // Get the wlan0 device
	        _moni0_dev = alldevs.get(i);
			
	        int snaplen = 64 * 1024;
	        int flags = Pcap.MODE_PROMISCUOUS;
	        int timeout = 10 * 1000;
	        _moni0_pcap =  
	                Pcap.openLive(_moni0_dev.getName(), snaplen, flags, timeout, errbuf);
	        
	        if (_moni0_pcap == null) {  
	            debugOut("Error while opening device for capture: "  
	                + errbuf.toString());  
	            sendMainMessage(ThreadMessages.WIFIDEV_FAILED);
	            return false;  
	        }  
	        
	        return true;
		}
		
		public boolean closeDev() {
			_moni0_pcap.close();
			return true;
		}
		
		// The way that the timer works is setup according to the type of scan
		// specified (read the comment near the top of the Wifi class).
		public void setupChannelTimer() {
			if(_native_scan) {
				_scan_channel=0;
				setChannel(channels[_scan_channel]);
				_scan_timer = new Timer();
				_scan_timer.schedule(new TimerTask() {
					@Override
					public void run() {
						scanIncrement();
					}

				}, 0, 210);			
			} else if(!_one_shot_scan) {
				// 5 seconds (250ms*20), that's the rough time it takes to scan both bands
				_scan_timer = new Timer();
				_scan_timer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						scanIncrement();
					}
		
				}, 500, SCAN_UPDATE_TIME);
				_timer_counts = SCAN_WAIT_COUNTS;
				if(_active_scan)
					coexisyst.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + _wlan_iface_name + " scan trigger");
				else
					coexisyst.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + _wlan_iface_name + " scan trigger passive");
					
			} else {
				_scan_timer = new Timer();
				_scan_timer.schedule(new TimerTask() {
					@Override
					public void run() {
						scanIncrement();
					}
		
				}, Wifi.SCAN_WAIT_TIME);  // 6.5 seconds seems enough to let all of the packets trickle in from the scan
				if(_active_scan)
					coexisyst.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + _wlan_iface_name + " scan trigger");
				else
					coexisyst.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + _wlan_iface_name + " scan trigger passive");
			}
		}
		
		// Notify to increment progress to the main thread
		private void scanIncrement() {
			if(_native_scan) {
				_scan_channel++;
				if(_scan_channel<channels.length) {
					debugOut("Incrementing channel to:" + Integer.toString(channels[_scan_channel]));
					setChannel(channels[_scan_channel]);
					debugOut("... increment complete!");
					sendMainMessage(ThreadMessages.INCREMENT_SCAN_PROGRESS);
				} else {
					APScanStop();
					_scan_timer.cancel();
				}
			} else if(!_one_shot_scan) {	
				sendMainMessage(ThreadMessages.INCREMENT_SCAN_PROGRESS);
				_timer_counts--;
				debugOut("Wifi scan timer tick");
				if(_timer_counts==0) {
					APScanStop();
					_scan_timer.cancel();
				}
			} else {
				debugOut("One shot expire");
				APScanStop();
				_scan_timer.cancel();
			}
		}	
		
		// The entire meat of the thread, pulls packets off the interface and dissects them
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (AWMon) params[0];

			openDev();
			setupChannelTimer();
			
			debugOut("Waiting for packets in scan thread...");
						
			// Loop and read headers and packets
			while(_state==WifiState.SCANNING) {
					
				Packet rpkt = new Packet(WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);
			    PcapHeader ph = new PcapHeader();
		        JBuffer jb = new JBuffer(PCAP_HDR_SIZE);
		        JBuffer data;
		        
		        // Pull in a packet
		        if((data = _moni0_pcap.next(ph, jb))==null) // returns null if fails
		        	continue;
		        _received_pkts++;
		        
		        // Get the raw bytes of the header
		        byte[] hdr = new byte[PCAP_HDR_SIZE];
		        ph.transferTo(hdr, 0);
		        rpkt._rawHeader = hdr;
				rpkt._headerLen = rpkt._rawHeader.length;
								
				// Get the raw data now from the wirelen in the pcap header
				rpkt._rawData = new byte[ph.wirelen()];
				data.getByteArray(0, rpkt._rawData);
				rpkt._dataLen = rpkt._rawData.length;
				
				// Dump it if we have the debug enabled
				if(PCAP_DUMP) {
					try {
						_pcap_dump.write(rpkt._rawHeader);
						_pcap_dump.write(rpkt._rawData);
						_pcap_dump.flush();
					} catch(Exception e) { Log.e("WifiScan", "Error writing pcap packet", e); }
				}
				
				// Take any packet where there is an SSID in it and the BSSID equals the wlan.sa, which
				// means the packet was transmit by the AP.
				rpkt.dissect();
				if( rpkt.getField("wlan_mgt.ssid")!=null && rpkt.getField("wlan.bssid").equals(rpkt.getField("wlan.sa"))) {
					debugOut("[" + Integer.toString(_received_pkts) + "] Found 802.11 network: " + rpkt.getField("wlan_mgt.ssid") + " on channel " + rpkt.getField("wlan_mgt.ds.current_channel"));
					_scan_results.add(rpkt);
				}
				rpkt.cleanDissection();
			}
			debugOut("Finished with scan thread, exiting now");
			closeDev();
			return "DONE";
		}
	}	
}
