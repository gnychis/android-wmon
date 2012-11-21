package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.Arrays;

import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
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
        
        try { Thread.sleep(10000); } catch(Exception e) {}
        
        _upnpService.getRegistry().removeListener(_listener);
		
		debugOut("Finished SSDP resolution");
		return _supportedInterfaces;	// Make sure to return the _ version.
	}
    
    // UPnP discovery is asynchronous, we need a callback
    RegistryListener _listener = new RegistryListener() {

        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) { debugOut("Discovery started: " + device.getDisplayString()); }
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) { debugOut("Discovery failed: " + device.getDisplayString() + " => " + ex); }
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) { debugOut("Remote device removed: " + device.getDisplayString()); }
        public void localDeviceRemoved(Registry registry, LocalDevice device) { debugOut("Local device removed: " + device.getDisplayString()); }
        public void beforeShutdown(Registry registry) { debugOut("Before shutdown, the registry has devices: " + registry.getDevices().size()); }
        public void afterShutdown() { debugOut("Shutdown of registry complete!"); }
        
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        	debugOut(
                    "Remote device available: " + device.getDisplayString()
            );
        }

        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
        	debugOut(
                    "Remote device updated: " + device.getDisplayString()
            );
        }

        public void localDeviceAdded(Registry registry, LocalDevice device) {
        	debugOut(
                    "Local device added: " + device.getDisplayString()
            );
        }

    };

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
