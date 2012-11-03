package com.gnychis.awmon.NameResolution;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.Core.Radio;

// Bonjour 
public class Zeroconf extends NameResolver {
	
	static final String TAG = "Zeroconf";
	static final boolean VERBOSE = true;
	
    private boolean _waitingOnResults;
    private boolean _waitingOnThread;
    
    public final static List<String> serviceListeners 
    				= Arrays.asList("100.1.168.192.in-addr.arpa.",
    								"_workstation._tcp.local.",
    								"_tcp.in-addr.arpa.",
    								//"in-addr.arpa.",
    								"_device-info._tcp.local.",
    								//"_home-sharing._tcp.local.",
    								//"_touch-able._tcp.local.",
    								//"_dacp._tcp.local.",
    								"_ssh._tcp.local.",
    								"_udp.local."
    						);
    
    private static JmDNS zeroConf = null;
    private static MulticastLock mLock = null;
    private ServiceListener _jmdnsListener = null;

	public Zeroconf(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Radio.Type.Wifi));
	}

	public ArrayList<Radio> resolveSupportedDevices(ArrayList<Radio> supportedDevices) {
		debugOut("Started Zeroconf resolution");
		
		_waitingOnThread=true;
		zeroConfThread monitorThread = new zeroConfThread();
		monitorThread.execute(_nr_manager._parent);
		
		while(_waitingOnThread)
			try { Thread.sleep(1000); } catch(Exception e) {}
		
		debugOut("Finished Zeroconf resolution");
		return supportedDevices;
	}
	
	// The purpose of this thread is solely to initialize the Wifi hardware
	// that will be used for monitoring.
	protected class zeroConfThread extends AsyncTask<Context, Integer, String>
	{
		// Initialize the hardware
		@Override
		protected String doInBackground( Context ... params )
		{
			setUp();
			
			// We need to wait a bit for some results
			while(_waitingOnResults) { 
				try{ Thread.sleep(100); } catch(Exception e) {} 
			}

			tearDown();	// tear down the search for services
			_waitingOnThread=false;

			return "true";
		}	
		
	    @Override
	    protected void onPostExecute(String result) { }
	    
		// The application needs to request the multicast lock.  Without it the application will not
		// receive packets that are not addressed to it.  This should be disabled when the scan is complete.
		// Otherwise, you will get battery drain.
	    private void setUp() {
	        WifiManager wifi = (WifiManager) _nr_manager._parent.getSystemService(Context.WIFI_SERVICE);

	        WifiInfo wifiinfo = wifi.getConnectionInfo();
	        int intaddr = wifiinfo.getIpAddress();

	        try {
		        if (intaddr != 0) { // Only worth doing if there's an actual wifi
		           // connection
	
		           byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
		                    (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
		           InetAddress addr = InetAddress.getByAddress(byteaddr);
	
		           Log.d(TAG, String.format("found intaddr=%d, addr=%s", intaddr, addr.toString()));
		           // start multicast lock
		           mLock = wifi.createMulticastLock("TunesRemote lock");
		           mLock.setReferenceCounted(true);
		           mLock.acquire();
		           
		           _jmdnsListener = new ServiceListener() {
	
		                @Override
		                public void serviceResolved(ServiceEvent ev) {
		                    debugOut("Service resolved: " + ev.getInfo().getQualifiedName() + " port:" + ev.getInfo().getPort());
		                }
	
		                @Override
		                public void serviceRemoved(ServiceEvent ev) {
		                    debugOut("Service removed: " + ev.getName());
		                }
	
		                @Override
		                public void serviceAdded(ServiceEvent event) {
		                    // Required to force serviceResolved to be called again (after the first search)
		                	debugOut("Service added: " + event.getName());
		                    zeroConf.requestServiceInfo(event.getType(), event.getName(), 1);
		                }
		            };
	
		           zeroConf = JmDNS.create(addr, "awmon");
		           for(String service : serviceListeners) 
		        	   zeroConf.addServiceListener(service, _jmdnsListener);
		        }
	        } catch(Exception e) { Log.e(TAG, "Error" + e); }
	        
			// Setup a handler to change the value of _waitingOnResults which blocks progress
			// until we have waited from results of a scan to trickle in.
			_waitingOnResults=true;
			Timer oneShotTimer = new Timer();
			oneShotTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					_waitingOnResults=false;
				}

			}, 10000);
	    }
	    
	    
	    // Give up the multicast lock and teardown, this saves us battery usage.
	    private void tearDown() {
	        for(String service : serviceListeners) 
	        	zeroConf.removeServiceListener(service, _jmdnsListener);
	        try {
	        	zeroConf.close();
	        	zeroConf=null;
	        } catch(Exception e) { Log.e(TAG, "zeroConf close error: " + e); }
	        
	        mLock.release();
	        mLock=null;
	    }
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
