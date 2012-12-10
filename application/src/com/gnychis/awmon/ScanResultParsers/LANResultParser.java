package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WiredInterface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

public class LANResultParser extends ScanResultParser {
	
	private static boolean VERBOSE = true;
	private static String TAG = "LANResultParser";
	
	public LANResultParser(Context c) { super(c); }

	@SuppressWarnings("unchecked")
	public <T extends Object> ArrayList<Interface> returnInterfaces(ArrayList<T> scanResult) {
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		
		// Let's get the Gateway IP, so that we can mark the interface as the "gateway" in our LAN
		WifiManager wifi = (WifiManager) _parent.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo d = wifi.getDhcpInfo();
		String gatewayIP = Interface.reverseIPAddress(Interface.intIPtoString(d.gateway).substring(1));
		
		for(String arpResponse : (ArrayList<String>)scanResult) {  // Maps this as Map<MAC,IP>
			debugOut("LAN Interface: " + arpResponse);
			String MAC = arpResponse.split("\t")[1].toLowerCase();
			String IP = arpResponse.split("\t")[0];
			
			// Create a new interface from this result
			Interface iface = new Interface(LAN.class);  // At this point, we don't know if it's wired or wireless
			iface._MAC=MAC;
			iface._IP=IP;
			
			// If this interface has the gateway's IP, mark it as the gateway and the interface must be wired.
			if(iface._IP.equals(gatewayIP)) {
				Interface tmp = new Interface(iface);
				iface = new WiredInterface(tmp);
				((WiredInterface)iface)._gateway=true;
				debugOut("Set " + iface._IP + " to the gateway");
			}
			
			interfaces.add(iface);
		}
		
		// Now, we add in the phone that is currently in use.  For some reason, it's skipped in the ARP scan.
		// Create a new interface from this result
		try {
		Interface iface = new WirelessInterface(Wifi.class);  // At this point, we don't know if it's wired or wireless
		iface._MAC=wifi.getConnectionInfo().getMacAddress();
		iface._IP=Interface.reverseIPAddress(Interface.intIPtoString(wifi.getConnectionInfo().getIpAddress()).substring(1));
		interfaces.add(iface);
		debugOut("LAN Interface: " + iface._IP + "    " + iface._MAC);
		} catch(Exception e) {
			
		}
		
		return interfaces;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
