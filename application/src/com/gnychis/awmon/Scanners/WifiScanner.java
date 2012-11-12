package com.gnychis.awmon.Scanners;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.util.Log;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.Core.Packet;
import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.GUIs.MainInterface;
import com.gnychis.awmon.GUIs.MainInterface.ThreadMessages;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;
import com.gnychis.awmon.HardwareHandlers.Wifi;

public class WifiScanner extends Scanner {

	final String TAG = "WifiScanner";
	private static final boolean VERBOSE = false;
	public static final String WIFI_SCAN_RESULT = MainInterface._app_name + ".WIFI_SCAN_RESULT";
	static int WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP = 23;
	static int WTAP_ENCAP_ETHERNET = 1;
	Context parent;
	private int PCAP_HDR_SIZE = 16;
	private PcapIf _moni0_dev;
	private Pcap _moni0_pcap;
	private int _timer_counts;		// to know how many timer ticks are left before scan over
	
	public static final boolean PCAP_DUMP = true;
	DataOutputStream _pcap_dump; 
	
	UserSettings _settings;
	
	// All scan related variables.  There are 3 kinds of scans that I've come up.
	// There is native, which is when CoexiSyst modifies the channels and passively scans.
	// Then, when triggering scans on the card and listening for the probe responses,
	// it can be "one shot" which is a single timer triggers when done, or periodic updates
	// which are useful for updating the main thread on progress.  Also, active scans
	// are currently only implemented in non-native scanning methods.
	private static int SCAN_WAIT_TIME= 3500;  // in milliseconds
	private static int NUMBER_OF_SCANS=3;
	public static boolean _active_scan=true;
	private boolean _scan_timer_expired;
	private Timer _scan_timer;		// the timer which will fire to end the scan or update it
	
	public WifiScanner() {
		super(Wifi.class);
		// If we are dumping all of our packets for debugging...
		if(PCAP_DUMP) {
			Log.d(TAG, "Trying to open pcap dump file");
			try {
				_pcap_dump = new DataOutputStream(new FileOutputStream("/sdcard/coexisyst_wifi.pcap"));
				byte pcap_header[] = {(byte)0xd4, (byte)0xc3, (byte)0xb2, (byte)0xa1, 		// magic number
						(byte)0x02, (byte)0x00, (byte)0x04,(byte) 0x00, 	// version numbers
						(byte)0x00, (byte)0x00, (byte)0x00,(byte) 0x00, 	// thiszone
						(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 	// sigfigs
						(byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00, 	// snaplen
						(byte)0x7f, (byte)0x00, (byte)0x00, (byte)0x00};  	// wifi
				
				_pcap_dump.write(pcap_header);
				Log.d(TAG, "Wrote pcap header");
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
            MainInterface.sendToastRequest(_hw_device._parent, "Failed to initialize Wifi device");
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
        	MainInterface.sendToastRequest(_hw_device._parent, "Failed to initialize Wifi device");
        	return false;
        }
        
        // Get the wlan0 device
        _moni0_dev = alldevs.get(i);
		
        int snaplen = 64 * 1024;
        int flags = Pcap.MODE_PROMISCUOUS;
        int timeout = 10 * 1000;
        _moni0_pcap =  
                Pcap.openLive(_moni0_dev.getName(), snaplen, flags, timeout, errbuf);
        
        // We want to filter out all packets that our phone sends/receives.  This is to avoid picking up
        // our ARP scan requests which generates too much traffic for the phone to parse.
        // and (not wlan addr1 00:26:bb:74:5f:e5 and not type ctl subtype cts)
        PcapBpfProgram program = new PcapBpfProgram();  
        String expression = "not (wlan addr1 " + ((Wifi)_hw_device)._wlan_mac 
        				   + " or wlan addr2 "+ ((Wifi)_hw_device)._wlan_mac
        				   + " or wlan addr3 " + ((Wifi)_hw_device)._wlan_mac
        				   + " or (wlan addr1 " + _settings.getHomeWifiMAC() + " and type ctl subtype cts))";
        int optimize = 0;         // 0 = false  
        int netmask = 0xFFFFFFFF; // 255.255.255.255 ... not sure this is actually used
        
        if (_moni0_pcap.compile(program, expression, optimize, netmask) != Pcap.OK) {
        	Log.d(TAG, "Error while compiling filter for capture");
        	return false;
        }
        
        if (_moni0_pcap.setFilter(program) != Pcap.OK) {  
        	Log.d(TAG, "Error setting the filter on the capture");
        	return false;
        }
                
        if (_moni0_pcap == null) {  
            debugOut("Error while opening device for capture: "  
                + errbuf.toString());  
            MainInterface.sendToastRequest(_hw_device._parent, "failed to open wifi device for capture");
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
		_scan_timer_expired=false;
		_scan_timer = new Timer();
		Wifi.setFrequency(Wifi._wlan_iface_name, _settings.getHomeWifiFreq()); // Start on home AP channel
		_scan_timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				triggerScan();
			}

		}, SCAN_WAIT_TIME, SCAN_WAIT_TIME);		// Wait one scan time on the home AP's channel
		
		// Trigger some ARP scans to get local traffic as we sit on the home AP's channel
		LANScanner.backgroundARPScan(3);
		
		_timer_counts = NUMBER_OF_SCANS;			
	}
	
	// This triggers a scan which should happen every
	private void triggerScan() {
		MainInterface.sendThreadMessage(_hw_device._parent, ThreadMessages.INCREMENT_SCAN_PROGRESS);
		if(_active_scan)
			BackgroundService.runCommand("/data/data/" + MainInterface._app_name + "/files/iw dev " + Wifi._wlan_iface_name + " scan trigger");
		else
			BackgroundService.runCommand("/data/data/" + MainInterface._app_name + "/files/iw dev " + Wifi._wlan_iface_name + " scan trigger passive");
		_timer_counts--;
		debugOut("Wifi scan timer tick");
		if(_timer_counts==-1) {
			_scan_timer_expired=true;
			_scan_timer.cancel();
		}
	}
	
	// The entire meat of the thread, pulls packets off the interface and dissects them
	@Override
	protected ArrayList<Interface> doInBackground( InternalRadio ... params )
	{
		debugOut("Starting the Wifi scan thread");
		_hw_device = params[0];
		_settings = new UserSettings(_hw_device._parent);
		ArrayList<Packet> scanResult = new ArrayList<Packet>();

		openDev();
		setupChannelTimer();
		
		debugOut("Waiting for packets in scan thread...");
					
		// Loop and read headers and packets
		while(!_scan_timer_expired) {
				
			Packet rpkt = new Packet(WTAP_ENCAP_IEEE_802_11_WLAN_RADIOTAP);
		    PcapHeader ph = new PcapHeader();
	        JBuffer jb = new JBuffer(PCAP_HDR_SIZE);
	        JBuffer data;
	        
	        // Pull in a packet
	        if((data = _moni0_pcap.next(ph, jb))==null) // returns null if fails
	        	continue;
	        
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
			
			scanResult.add(rpkt);
		}
		debugOut("Finished with scan thread, exiting now");
		closeDev();
		return _result_parser.returnInterfaces(scanResult);
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
	
}
