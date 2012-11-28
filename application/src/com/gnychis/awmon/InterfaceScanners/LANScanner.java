package com.gnychis.awmon.InterfaceScanners;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.ScanResultParsers.LANResultParser;
import com.gnychis.awmon.ScanResultParsers.ScanResultParser;

// Scanning the LAN is doing an active ARP scan, and then seeing which devices
// are active on the LAN.
public class LANScanner extends InterfaceScanner {
	
	private static String TAG = "LANScanner";
	private static boolean VERBOSE = true;
	
	private final static int NUM_ARP_RETRIES = 6;

	public LANScanner(Context c) {
		super(c, LAN.class);
	}
	
	@Override
	protected ArrayList<Interface> doInBackground( InternalRadio ... params )
	{
		debugOut("Scanning the LAN with ARP requests...");
		_hw_device = params[0];		
		ArrayList<String> scanResult = new ArrayList<String>();
		
		scanResult.addAll(BackgroundService.runCommand("arp_scan --retry=" + NUM_ARP_RETRIES + " --interface=wlan0 -l -q 2> /dev/null"));
		HashSet<String> hs = new HashSet<String>();
		hs.addAll(scanResult);
		scanResult.clear();
		scanResult.addAll(hs);
		
		debugOut("Completed LAN scan");
		return _result_parser.returnInterfaces(scanResult);
	}
	
	// FIXME: This is one hell of a hack.
	public static ArrayList<Interface> getActiveInterfaces(Context c) {
		
		ArrayList<String> scanResult = new ArrayList<String>();
		
		scanResult.addAll(BackgroundService.runCommand("arp_scan --retry=" + NUM_ARP_RETRIES + " --interface=wlan0 -l -q 2> /dev/null"));
		HashSet<String> hs = new HashSet<String>();
		hs.addAll(scanResult);
		scanResult.clear();
		scanResult.addAll(hs);
		
		ScanResultParser result_parser = new LANResultParser(c);
		return result_parser.returnInterfaces(scanResult);
	}

	
	//**********************************************************************************************//
	// The purpose of this function is to create a background ARP scan that generates traffic to all
	// Wifi devices that are connected to the home network.  Useful to get some quick RSSI values.
	public static void backgroundARPScan(int num_scans, String hosts) {
		debugOut("Trigger " + num_scans + " background ARP scans");
		
		class ArpThread implements Runnable { 
			int num_scans;
			String scanHosts;
			
			public ArpThread(int nscans, String hosts) {
				num_scans = nscans;
				scanHosts = hosts;
			}
			
			@Override
			public void run() {
				debugOut("Running background ARP scans with " + num_scans + " on hosts: " + scanHosts);
				int i=num_scans;
				while(i-- > 0) {
					if(scanHosts==null)
						BackgroundService.runCommand("arp_scan --interface=wlan0 -l -q");
					else
						BackgroundService.runCommand("arp_scan --interface=wlan0 -q " + scanHosts);
				}
			}
		}
		
		Runnable arpThread = new ArpThread(num_scans, hosts);
		new Thread(arpThread).start();
	}
	
	static final class BackgroundARPScan extends AsyncTask<Integer, String, String> {
		@Override
		protected String doInBackground(Integer... params) {
			int i = params[0];
			while(i-- > 0) {

			}
			return "OK";
		}
	}
	//**********************************************************************************************//
	
	private static void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
