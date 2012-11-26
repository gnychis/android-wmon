package com.gnychis.awmon.NameResolution;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

public class DNSHostName extends NameResolver {
	
	static final String TAG = "DNSHostName";
	static final boolean VERBOSE = true;

	@SuppressWarnings("unchecked")
	public DNSHostName(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Wifi.class, LAN.class));
	}
	
	public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedInterfaces) {
		debugOut("Starting DNS Host Name resolution...");
		for(Interface iface : supportedInterfaces) {
			if(iface.hasValidIP()) {
				String response = scanIP(iface._IP);
				debugOut("... IP: " + iface._IP + " <---> " + response);
			}
		}
		debugOut("Done with DNS Host Name resolution");
		return supportedInterfaces;
	}
	
	private String scanIP(String IP) {
		InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(IP);
            if(inetAddress.isReachable(1000)){
                return inetAddress.getCanonicalHostName();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
