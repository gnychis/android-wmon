package com.gnychis.coexisyst;

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
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;
import com.stericson.RootTools.RootTools;

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
	private static final String TAG = "AtherosDev";

	public static final int ATHEROS_CONNECT = 100;
	public static final int ATHEROS_DISCONNECT = 101;
	public static final String WIFI_SCAN_RESULT = "com.gnychis.coexisyst.WIFI_SCAN_RESULT";
	public static final int MS_SLEEP_UNTIL_PCAPD = 1500;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;

	
	static int WTAP_ENCAP_ETHERNET = 1;
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	
	WifiState _state;
	private Semaphore _state_lock;
	public enum WifiState {
		IDLE,
		SCANNING,
	}
	
	// All scan related variables.  There are 3 kinds of scans that I've come up.
	// There is native, which is when CoexiSyst modifies the channels and passively scans.
	// Then, when triggering scans on the card and listening for the probe responses,
	// it can be "one shot" which is a single timer triggers when done, or periodic updates
	// which are useful for updating the main thread on progress.  Also, active scans
	// are currently only implemented in non-native scanning methods.
	WifiScan _scan_thread;
	private static int SCAN_WAIT_TIME=5500;  // in milliseconds
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
		
		// Native scanning relies on doing the actual scanning (channel hopping)
		// within CoexiSyst.  Non-native relies on the nl80211 driver to do the
		// channel hopping for us.  It's not clear which is better, so I opted for
		// tighter control as the default.
		_scan_thread = new WifiScan();
		_scan_thread.execute(coexisyst);
		
		return true;  // in scanning state, and channel hopping
	}
	
	public boolean APScanStop() {
		
		_scan_thread._stop=true;  // notify the thread to stop
		
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
		
		// All modules related to ath9k_htc that need to be inserted
		runCommand("insmod /system/lib/modules/compat.ko");
		runCommand("insmod /system/lib/modules/compat_firmware_class.ko");
		runCommand("insmod /system/lib/modules/cfg80211.ko");
		runCommand("insmod /system/lib/modules/mac80211.ko");
		runCommand("insmod /system/lib/modules/ath.ko");
		runCommand("insmod /system/lib/modules/ath9k_hw.ko");
		runCommand("insmod /system/lib/modules/ath9k_common.ko");
		runCommand("insmod /system/lib/modules/ath9k_htc.ko");
		Log.d("AtherosDev", "Inserted kernel modules");

	}
	
	// When an atheros device is connected, spawn a thread which
	// initializes the hardware
	public void connected() {
		_device_connected=true;
		WifiInit init_thread = new WifiInit();
		init_thread.execute(coexisyst);
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
			List<String> res = RootTools.sendShell("/data/data/com.gnychis.coexisyst/files/iwconfig moni0");
			if(res.size()>1)
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
	public boolean moni0_up() {
		try {
			List<String> res = RootTools.sendShell("netcfg | busybox grep \"^moni0\" | busybox grep UP");
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
	
	// cat the atheros hard statistics count for the number of received packets
	public int getRxPacketCount() {
		return Integer.parseInt(runCommand("cat " + _rxpackets_loc).get(0));
	}
	
	
	public void setChannel(int channel) {
		runCommand("/data/data/com.gnychis.coexisyst/files/iw phy " + _iw_phy + " set channel " + Integer.toString(channel));
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
	
	public List<String> runCommand(String c) {
		try {
			return RootTools.sendShell(c);
		} catch(Exception e) {
			Log.e(TAG, "error writing to RootTools the command: " + c, e);
			return null;
		}
	}

	
	// The purpose of this thread is solely to initialize the Atheros hardware
	// that will be used for monitoring.
	// Initializes the Atheros hardware strictly.  First writes the firmware, then sets the
	// interface to be in monitor mode
	protected class WifiInit extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		private static final String WIMON_TAG = "WifiInit";
		
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
			// Only initialize if it is not already initialized
			if(wlan0_exists() && wlan0_up() && wlan0_monitor()) {
				Log.d(TAG, "Atheros device is already connected and initialized...");
				sendMainMessage(ThreadMessages.ATHEROS_INITIALIZED);
				return "true";
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
				runCommand("echo 1 > " + load_loc);
			}
			Log.d(TAG, "Wrote notification of impending firmware write");
			
			// Write the firmware to the appropriate location
			String firmware_loc = load_loc.substring(0, load_loc.length()-7);
			runCommand("cat /data/data/com.gnychis.coexisyst/files/htc_7010.fw > " + firmware_loc + "data");
			Log.d(TAG, "Wrote the firmware to the device");
			
			// Notify that we are done writing the firmware
			while(compatIsLoading(load_loc)) {
				runCommand("echo 0 > " + load_loc);
			}
			Log.d(TAG, "Notify of firmware complete");
			
			// Wait for the firmware to settle, and device interface to pop up
			while(!wlan0_exists()) {
				trySleep(100);
			}
			Log.d(TAG, "wlan0 now exists");
				
			while(!wlan0_down()) {
				runCommand("netcfg wlan0 down");
				trySleep(100);
			}
			Log.d(TAG, "interface has been taken down");
			
			// Get the phy interface name
			List<String> r = runCommand("/data/data/com.gnychis.coexisyst/files/iw list | busybox head -n 1 | busybox awk '{print $2}'");
			_iw_phy = r.get(0);
			
			while(!wlan0_monitor()) {
				runCommand("/data/data/com.gnychis.coexisyst/files/iw phy " + _iw_phy + " interface add moni0 type monitor");
				trySleep(100);
			}
			Log.d(TAG, "interface set to monitor mode");
			
			while(!wlan0_up()) {
				runCommand("netcfg wlan0 up");
				trySleep(100);
			}
			
			while(!moni0_up()) {
				runCommand("netcfg moni0 up");
				trySleep(100);
			}			
			Log.d(TAG, "interface is now up");
			
			// Get the location of the rx packets file
			List<String> l = runCommand("busybox find /sys -name rx_packets | busybox grep moni0");
			_rxpackets_loc = l.get(0);

			sendMainMessage(ThreadMessages.ATHEROS_INITIALIZED);
			return "true";
		}		
	}
	
	// WARNING: Do not cancel(true) this task!  It is unsafe for it to be interrupted in
	// the middle of a dissection.  This will cause the application to segfault.
	protected class WifiScan extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		private static final String WIMON_TAG = "WiFiMonitor";
		private int PCAP_HDR_SIZE = 16;
		private int _received_pkts;
		private PcapIf _moni0_dev;
		private Pcap _moni0_pcap;
		private int _timer_counts;		// to know how many timer ticks are left before scan over
		private int _scan_channel;		// to keep track of the channel when scanning natively
		public boolean _stop;
		
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
		
		// Opens a the moni0 device as a pcap interface for packet capture
		public boolean openDev() {
			
			// Try to use jnetpcap straight up to open the interface
			List<PcapIf> alldevs = new ArrayList<PcapIf>();
			StringBuilder errbuf = new StringBuilder(); // For any error msgs  
			int r = Pcap.findAllDevs(alldevs, errbuf);  
			if (r == Pcap.NOT_OK || alldevs.isEmpty()) {  
	            Log.d(TAG, "Can't read list of devices, error is " + errbuf.toString());  
	            sendMainMessage(ThreadMessages.ATHEROS_FAILED);
	            return false;  
	        } 
			
			Log.d(TAG, "Network devices found:");  
			int i = 0;  
	        for (PcapIf device : alldevs) {  
	            String description =  
	                (device.getDescription() != null) ? device.getDescription()  
	                    : "No description available";  
	            Log.d(TAG, Integer.toString(i) + ": " + device.getName() + "[" + description + "]");  
	            if(device.getName().equals("moni0"))
	            	break;
	            else
	            	i++;
	        }  			
	        
	        // Get the wlan0 device
	        _moni0_dev = alldevs.get(i);
			
	        int snaplen = 64 * 1024;
	        int flags = Pcap.MODE_PROMISCUOUS;
	        int timeout = 10 * 1000;
	        _moni0_pcap =  
	                Pcap.openLive(_moni0_dev.getName(), snaplen, flags, timeout, errbuf);
	        
	        if (_moni0_pcap == null) {  
	            Log.d(TAG, "Error while opening device for capture: "  
	                + errbuf.toString());  
	            sendMainMessage(ThreadMessages.ATHEROS_FAILED);
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
					runCommand("/data/data/com.gnychis.coexisyst/files/iw dev wlan0 scan trigger");
				else
					runCommand("/data/data/com.gnychis.coexisyst/files/iw dev wlan0 scan trigger passive");
					
			} else {
				_scan_timer = new Timer();
				_scan_timer.schedule(new TimerTask() {
					@Override
					public void run() {
						scanIncrement();
					}
		
				}, Wifi.SCAN_WAIT_TIME);  // 6.5 seconds seems enough to let all of the packets trickle in from the scan
				if(_active_scan)
					runCommand("/data/data/com.gnychis.coexisyst/files/iw dev wlan0 scan trigger");
				else
					runCommand("/data/data/com.gnychis.coexisyst/files/iw dev wlan0 scan trigger passive");
			}
		}
		
		// Notify to increment progress to the main thread
		private void scanIncrement() {
			if(_native_scan) {
				_scan_channel++;
				if(_scan_channel<channels.length) {
					Log.d(TAG, "Incrementing channel to:" + Integer.toString(channels[_scan_channel]));
					setChannel(channels[_scan_channel]);
					Log.d(TAG, "... increment complete!");
					sendMainMessage(ThreadMessages.INCREMENT_PROGRESS);
				} else {
					APScanStop();
					_scan_timer.cancel();
				}
			} else if(!_one_shot_scan) {	
				sendMainMessage(ThreadMessages.INCREMENT_PROGRESS);
				_timer_counts--;
				Log.d(TAG, "Wifi scan timer tick");
				if(_timer_counts==0) {
					APScanStop();
					_scan_timer.cancel();
				}
			} else {
				Log.d(TAG, "One shot expire");
				APScanStop();
				_scan_timer.cancel();
			}
		}	
		
		// The entire meat of the thread, pulls packets off the interface and dissects them
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			_stop=false;

			openDev();
			setupChannelTimer();
			
			Log.d(TAG, "Waiting for packets in scan thread...");
						
			// Loop and read headers and packets
			while(!_stop) {
					
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
				
				// To identify beacon: wlan_mgt.fixed.beacon is set.  If it is a beacon, add it
				// to our scan result.  This does not guarantee one beacon frame per network, but
				// pruning can be done at the next level.
				if(rpkt.getField("wlan_mgt.fixed.beacon")!=null) {
					Log.d(TAG, "[" + Integer.toString(_received_pkts) + "] Found 802.11 network: " + rpkt.getField("wlan_mgt.ssid") + " on channel " + rpkt.getField("wlan_mgt.ds.current_channel"));
					_scan_results.add(rpkt);
				}
			}
			closeDev();
			return "DONE";
		}
	}	
}
