package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;

import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

/**
 * This name resolver is most useful for finding non-Apple stuff.  Zeroconf (Bonjour) is great for Apple,
 * but UPnp and SSDP are great for just about everything else.
 * 
 * This leverages the great work on the Cling library, done by Christian Bauer, 
 * 
 * @author George Nychis (gnychis)
 */
public class SSDP extends NameResolver {
	
	static final String TAG = "SSDP";
	static final boolean VERBOSE = true;
	
	static final int RESPONSE_WAIT_TIME = 15000; // in milliseconds
	
	private UpnpService _upnpService;
	
    ArrayList<Interface> _supportedInterfaces;

    @SuppressWarnings("unchecked")
	public SSDP(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Wifi.class, LAN.class));
	}
    
	public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedInterfaces) {
		debugOut("Started SSDP resolution");
		_supportedInterfaces = supportedInterfaces;  // make them accessible
		
		// Instantiate a Upnp Service, make sure it is the Android type or it will throw an exception
        _upnpService = new UpnpServiceImpl(new AndroidUpnpServiceConfiguration((WifiManager) _parent.getSystemService(Context.WIFI_SERVICE)));
        
        _upnpService.getRegistry().addListener(_listener);
        _upnpService.getControlPoint().search(new STAllHeader());
        
        try { Thread.sleep(RESPONSE_WAIT_TIME); } catch(Exception e) {}
        
        _upnpService.getRegistry().removeListener(_listener);
		
		debugOut("Finished SSDP resolution");
		return _supportedInterfaces;	// Make sure to return the _ version.
	}
    
    // UPnP discovery is asynchronous, we need a callback
    RegistryListener _listener = new RegistryListener() {

        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) { /*debugOut("Discovery started: " + device.getDisplayString());*/ }
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) { /*debugOut("Discovery failed: " + device.getDisplayString() + " => " + ex);*/ }
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) { /*debugOut("Remote device removed: " + device.getDisplayString());*/ }
        public void localDeviceRemoved(Registry registry, LocalDevice device) { /*debugOut("Local device removed: " + device.getDisplayString());*/ }
        public void beforeShutdown(Registry registry) { /*debugOut("Before shutdown, the registry has devices: " + registry.getDevices().size());*/ }
        public void afterShutdown() { /*debugOut("Shutdown of registry complete!");*/ }
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) { /*debugOut("Remote device updated: " + device.getDisplayString());*/ }
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) { deviceNameResolved(device); }
        public void localDeviceAdded(Registry registry, LocalDevice device) { deviceNameResolved(device); }
        
        // This is where the code goes to extract the name and save it.
        public void deviceNameResolved(final Device device) {
        	
        	// We play some string manipulation tricks 
        	String IP = null;
        	try {
	        	String[] tokens = device.getIdentity().toString().split(" ");
	        	String URL = tokens[tokens.length-1];
	        	IP = URL.split("http://")[1].split(":")[0];
        	} catch(Exception e) { debugOut("Error trying to extract the IP: " + e); }
        	
        	if(IP==null)
        		return;
        	
        	// Go through our list of interfaces and look for a matching IP, if we find one, then we name it.
	    	for(Interface iface : _supportedInterfaces) {
	    		
	    		if(iface._IP!=null && iface._IP.equals(IP) && iface._ifaceName==null) {
	    			
	    			iface._ifaceName = device.getDetails().getFriendlyName();

	    			// If it happens to be a gateway device (AP), give it the DisplayString...
	    			if(device.getType().toString().equals("urn:schemas-upnp-org:device:InternetGatewayDevice:1"))
	    				iface._ifaceName = device.getDisplayString().split(" ")[0] + " " +  device.getDetails().getFriendlyName();
	    			
	    			debugOut("Matched IP: " + IP + "  with name: " + iface._ifaceName);
	    		}
	    	}
        	
        	/*
        	debugOut("SSDP DEVICE: " + device.getDetails().getFriendlyName());
        	debugOut("... " + device.getDisplayString());
        	debugOut("... " + device.getType().getType());
        	debugOut("... " + device.getType().toString());
        	debugOut(".... " + device.getIdentity().toString());
        	*/
        }

    };

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
