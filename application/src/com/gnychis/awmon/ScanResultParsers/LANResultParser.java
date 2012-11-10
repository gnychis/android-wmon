package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.LAN;

public class LANResultParser extends ScanResultParser {

	@SuppressWarnings("unchecked")
	public <T extends Object> ArrayList<Interface> returnInterfaces(ArrayList<T> scanResult) {
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		
		for(String arpResponse : (ArrayList<String>)scanResult) {  // Maps this as Map<MAC,IP>
			String MAC = arpResponse.split("\t")[1].toLowerCase();
			String IP = arpResponse.split("\t")[0];
			
			// Create a new interface from this result
			Interface iface = new Interface(LAN.class);  // At this point, we don't know if it's wired or wireless
			iface._MAC=MAC;
			iface._IP=IP;
			interfaces.add(iface);
		}
		
		return interfaces;
	}
}
