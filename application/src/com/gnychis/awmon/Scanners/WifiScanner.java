package com.gnychis.awmon.Scanners;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.AWMon.ThreadMessages;
import com.gnychis.awmon.Core.Packet;
import com.gnychis.awmon.DeviceHandlers.HardwareDevice;
import com.gnychis.awmon.DeviceHandlers.Wifi;

public class WifiScanner extends AsyncTask<HardwareDevice, Integer, String> {

	final String TAG = "WifiScanner";
	private static final boolean VERBOSE = false;
	public static final String WIFI_SCAN_RESULT = AWMon._app_name + ".WIFI_SCAN_RESULT";
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	static int WTAP_ENCAP_ETHERNET = 1;
	HardwareDevice _hw_device;
	Context parent;
	AWMon coexisyst;
	private int PCAP_HDR_SIZE = 16;
	private int _received_pkts;
	private PcapIf _moni0_dev;
	private Pcap _moni0_pcap;
	private int _timer_counts;		// to know how many timer ticks are left before scan over
	private int _scan_channel;		// to keep track of the channel when scanning natively
	
	public static final boolean PCAP_DUMP = false;
	DataOutputStream _pcap_dump; 
	
	// All scan related variables.  There are 3 kinds of scans that I've come up.
	// There is native, which is when CoexiSyst modifies the channels and passively scans.
	// Then, when triggering scans on the card and listening for the probe responses,
	// it can be "one shot" which is a single timer triggers when done, or periodic updates
	// which are useful for updating the main thread on progress.  Also, active scans
	// are currently only implemented in non-native scanning methods.
	private static int SCAN_WAIT_TIME= 6000;  // in milliseconds
	private static int SCAN_UPDATE_TIME=250; // update every 250ms to the main thread
	public static int SCAN_WAIT_COUNTS=SCAN_WAIT_TIME/SCAN_UPDATE_TIME;
	public static boolean _native_scan=false;
	public static boolean _one_shot_scan=false;
	public static boolean _active_scan=true;
	private Timer _scan_timer;		// the timer which will fire to end the scan or update it
	ArrayList<Packet> _scan_results;
	
	public WifiScanner() {
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
	
	public void trySleep(int length) {
		try {
			Thread.sleep(length);
		} catch(Exception e) {
			Log.e("WiFiMonitor", "Error running commands for connect wifi device", e);
		}
	}
	
	// Opens a the moni0 device as a pcap interface for packet capture
	public boolean openDev() {
		
		// Try to use jnetpcap straight up to open the interface
		List<PcapIf> alldevs = new ArrayList<PcapIf>();
		StringBuilder errbuf = new StringBuilder(); // For any error msgs  
		int r = Pcap.findAllDevs(alldevs, errbuf);  
		if (r == Pcap.NOT_OK || alldevs.isEmpty()) {  
            debugOut("Can't read list of devices, error is " + errbuf.toString());  
            AWMon.sendToastRequest(_hw_device._parent, "Failed to initialize Wifi device");
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
        	AWMon.sendToastRequest(_hw_device._parent, "Failed to initialize Wifi device");
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
            AWMon.sendToastRequest(_hw_device._parent, "failed to open wifi device for capture");
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
			Wifi.setChannel(Wifi._wlan_iface_name, Wifi.channels[_scan_channel]);
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
				AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + Wifi._wlan_iface_name + " scan trigger");
			else
				AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + Wifi._wlan_iface_name + " scan trigger passive");
				
		} else {
			_scan_timer = new Timer();
			_scan_timer.schedule(new TimerTask() {
				@Override
				public void run() {
					scanIncrement();
				}
	
			}, SCAN_WAIT_TIME);  // 6.5 seconds seems enough to let all of the packets trickle in from the scan
			if(_active_scan)
				AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + Wifi._wlan_iface_name + " scan trigger");
			else
				AWMon.runCommand("/data/data/" + AWMon._app_name + "/files/iw dev " + Wifi._wlan_iface_name + " scan trigger passive");
		}
	}
	
	// Notify to increment progress to the main thread
	private void scanIncrement() {
		if(_native_scan) {
			_scan_channel++;
			if(_scan_channel<Wifi.channels.length) {
				debugOut("Incrementing channel to:" + Integer.toString(Wifi.channels[_scan_channel]));
				Wifi.setChannel(Wifi._wlan_iface_name, Wifi.channels[_scan_channel]);
				debugOut("... increment complete!");
				AWMon.sendThreadMessage(_hw_device._parent, ThreadMessages.INCREMENT_SCAN_PROGRESS);
			} else {
				_hw_device.scanComplete();
				_scan_timer.cancel();
			}
		} else if(!_one_shot_scan) {	
			AWMon.sendThreadMessage(_hw_device._parent, ThreadMessages.INCREMENT_SCAN_PROGRESS);
			_timer_counts--;
			debugOut("Wifi scan timer tick");
			if(_timer_counts==0) {
				_hw_device.scanComplete();
				_scan_timer.cancel();
			}
		} else {
			debugOut("One shot expire");
			_hw_device.scanComplete();
			_scan_timer.cancel();
		}
	}
	
	// The entire meat of the thread, pulls packets off the interface and dissects them
	@Override
	protected String doInBackground( HardwareDevice ... params )
	{
		_hw_device = params[0];

		openDev();
		setupChannelTimer();
		
		debugOut("Waiting for packets in scan thread...");
					
		// Loop and read headers and packets
		while(_hw_device.getState()==HardwareDevice.State.SCANNING) {
				
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
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
	
}
