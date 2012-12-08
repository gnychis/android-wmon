package com.gnychis.awmon.NameResolution;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

public class DNSHostName extends NameResolver {
	
	static final String TAG = "DNSHostName";
	static final boolean VERBOSE = true;
	
	private static final int LOOKUP_TIMEOUT_SECS = 0;
	private static final int LOOKUP_TIMEOUT_MS = 250;
	
	private static final int NUM_LOOKUPS_PER_HOST = 3;

	@SuppressWarnings("unchecked")
	public DNSHostName(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Wifi.class, LAN.class));
	}
	
	public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedInterfaces) {
		debugOut("Starting DNS Host Name resolution...");
		
		// Let's get the Gateway IP, so that we can use it as our local DNS server
		WifiManager wifi = (WifiManager) _parent.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo d = wifi.getDhcpInfo();
		String gatewayIP = Interface.reverseIPAddress(Interface.intIPtoString(d.gateway).substring(1));
		
		// If the gateway is invalid, let's not crash... instead just bail
		if(!Interface.validateIPAddress(gatewayIP))
			return supportedInterfaces;
		
		// Now, for each interface do a hostname lookup on it
		try {
			SimpleResolver resolver = new SimpleResolver();
			resolver.setAddress(InetAddress.getByName("192.168.1.1"));
			resolver.setTimeout(LOOKUP_TIMEOUT_SECS, LOOKUP_TIMEOUT_MS);
			
			for(Interface iface : supportedInterfaces) {

				if(iface.hasValidIP()) {  // If the interface has a valid IP address
					int j = NUM_LOOKUPS_PER_HOST;
					while(j-- > 0) { 
						Lookup lookup = new Lookup(Interface.reverseIPAddress(iface._IP) + "." + "in-addr.arpa", Type.PTR);
						lookup.setResolver(resolver);
						Record[] records = lookup.run();
						
						// If we had a successful hostname lookup
				        if(lookup.getResult() == Lookup.SUCCESSFUL) {
				        	
				              for (int i = 0; i < records.length; i++) {
				            	  
				                if(records[i] instanceof PTRRecord) {		// Get the record
				                
				                  PTRRecord ptr = (PTRRecord) records[i];
				                  String hostname = ptr.rdataToString().substring(0, ptr.rdataToString().length()-6);
						          if(iface._ifaceName==null) {
						        	  _manufacturers.add(iface.cleanOUIname());
						        	  _resolved++;
						          }
						          iface._ifaceName=hostname;	// Otherwise, you get the basic hostname
				                  debugOut("Mapped " + iface._IP + " <---> " + hostname);
				                }
				              }
				        } else {
				            debugOut("Could not map " + iface._IP);
				        }
					}
				}
			}
		} catch(Exception e) {
			debugOut("Exception: " + e);
		}

		debugOut("Done with DNS Host Name resolution");
		return supportedInterfaces;
	}
	
/*	
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
*/
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
