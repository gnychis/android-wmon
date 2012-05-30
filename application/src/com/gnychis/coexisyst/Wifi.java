package com.gnychis.coexisyst;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
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
	private static final boolean VERBOSE = false;
	
	public static final boolean PCAP_DUMP = false;
	DataOutputStream _pcap_dump; 

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
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d("AtherosDev", msg);
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
	public static boolean _active_scan=false;
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
	
	public void trySleep(int length) {
		try {
			Thread.sleep(length);
		} catch(Exception e) {
			Log.e("WiFiMonitor", "Error running commands for connect atheros device", e);
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
	
	public Wifi(CoexiSyst c) {
		_state_lock = new Semaphore(1,true);
		_scan_results = new ArrayList<Packet>();
		coexisyst = c;
		_state = WifiState.IDLE;
		
		// All modules related to ath9k_htc that need to be inserted
		runCommand("insmod /system/etc/awmon_modules/compat.ko");
		runCommand("insmod /system/etc/awmon_modules/compat_firmware_class.ko");
		runCommand("insmod /system/etc/awmon_modules/cfg80211.ko");
		runCommand("insmod /system/etc/awmon_modules/mac80211.ko");
		runCommand("insmod /system/etc/awmon_modules/ath.ko");
		runCommand("insmod /system/etc/awmon_modules/ath9k_hw.ko");
		runCommand("insmod /system/etc/awmon_modules/ath9k_common.ko");
		runCommand("insmod /system/etc/awmon_modules/ath9k_htc.ko");
		Log.d("AtherosDev", "Inserted kernel modules");
		
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
			} catch(Exception e) { Log.e("AtherosDev", "Error trying to write output stream", e); }
		}

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
	
	// Returns -1 if the interface specified doesn't exist, 1 if up, 0 if down
	public int iface_up(String iface) {
		if(!iface_exists(iface))
			return -1;
		
		ArrayList<String> res = runCommand("netcfg | grep \"^" + iface + "\" | grep UP");
		if(res.size()==1)
			return 1;
		else
			return 0;
	}
	
	// Returns -1 if the interface specified doesn't exist, 1 if down, 0 if up
	public int iface_down(String iface) {
		if(!iface_exists(iface))
			return -1;
		
		ArrayList<String> res = runCommand("netcfg | grep \"^" + iface + "\" | grep DOWN");
		if(res.size()==1)
			return 1;
		else
			return 0;
	}
	
	public boolean iface_exists(String iface) {
		ArrayList<String> res = runCommand("netcfg | grep \"^" + iface + "\"");
		if(res.size()==1)
			return true;
		else
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
	
	public ArrayList<String> runCommand(String c) {
		ArrayList<String> res = new ArrayList<String>();
		try {
			// First, run the command push the result to an ArrayList
			List<String> res_list = RootTools.sendShell(c,0);
			Iterator it=res_list.iterator();
			while(it.hasNext()) 
				res.add((String)it.next());
			
			res.remove(res.size()-1);
			
			// Trim the ArrayList of an extra blank lines at the end
			while(true) {
				int index = res.size()-1;
				if(index>=0 && res.get(index).length()==0)
					res.remove(index);
				else
					break;
			}
			return res;
			
		} catch(Exception e) {
			Log.e("AtherosDev", "error writing to RootTools the command: " + c, e);
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
		
		private void debugOut(String msg) {
			if(VERBOSE)
				Log.d("WifiInit", msg);
		}
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(CoexiSyst.ThreadMessages t) {
			Message msg = new Message();
			msg.obj = t;
			coexisyst._handler.sendMessage(msg);
		}
		
		// Initialize the hardware
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			
			// Spin a little bit if it takes a second to bring the interface up
			// after we catch the USB device being inserted.
			while(!iface_exists("wlan0"))
				trySleep(100);
			
			// If we already have the monitoring interface up, we are already initialized
			if(iface_up("wlan0")==1 && iface_up("moni0")==1) {
				debugOut("Atheros device is already connected and initialized...");
				sendMainMessage(ThreadMessages.ATHEROS_INITIALIZED);
				return "true";				
			}

			// Otherwise, let's take wlan0 if it's not already
			while(iface_down("wlan0")==0) {
				runCommand("netcfg wlan0 down");
				trySleep(100);
			}
			debugOut("wlan0 interface has been taken down");
			
			// Get the phy interface name
			List<String> r = runCommand("/data/data/com.gnychis.coexisyst/files/iw list | busybox head -n 1 | busybox awk '{print $2}'");
			_iw_phy = r.get(0);
			
			while(!iface_exists("moni0")) {
				runCommand("/data/data/com.gnychis.coexisyst/files/iw phy " + _iw_phy + " interface add moni0 type monitor");
				trySleep(100);
			}
			debugOut("interface set to monitor mode");
			
			while(iface_up("wlan0")==0) {
				runCommand("netcfg wlan0 up");
				trySleep(100);
			}
			
			while(iface_up("moni0")==0) {
				runCommand("netcfg moni0 up");
				trySleep(100);
			}			
			debugOut("interface is now up");
			
			
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
	            debugOut("Can't read list of devices, error is " + errbuf.toString());  
	            sendMainMessage(ThreadMessages.ATHEROS_FAILED);
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
			coexisyst = (CoexiSyst) params[0];

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
				
				// To identify beacon: wlan_mgt.fixed.beacon is set.  If it is a beacon, add it
				// to our scan result.  This does not guarantee one beacon frame per network, but
				// pruning can be done at the next level.
				rpkt.dissect();
				if(rpkt.getField("wlan_mgt.fixed.beacon")!=null) {
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
