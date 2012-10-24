package com.gnychis.awmon.BackgroundService;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.DeviceHandlers.Wifi;

public class LocationMonitor {
	
    public static String TAG = "AWMonLocationMonitor";
	
    public final int LOCATION_TOLERANCE=150;			// in meters
    public final int LOCATION_UPDATE_INTERVAL=120000; //900000;	// in milliseconds (15 minutes)
    
    private static BackgroundService _backgroundService;
    
    PendingIntent mPendingIntent;
    Intent mIntent;

    // Used to keep the location of the home so that we know when the user is home.
    // However, we NEVER retrieve this information from the phone.  Your home location
    // is only kept privately on your phone.
    public boolean mNextLocIsHome;
    public Location mHomeLoc;
    String home_ssid;
    
    WifiManager wifi;
    List<ScanResult> scan_result;
    public boolean mDisableWifiAS;
    public int mScansLeft;
    public final int NUM_SCANS=4;
    
    LocationManager locationManager;
    LocationListener locationListener;
    PowerManager mPowerManager;
    
    private static Timer mLocationTimer = new Timer(); 
    
    private UserSettings _settings;
    
    public LocationMonitor(BackgroundService bs) {	
    	_backgroundService=bs;
    	_settings = new UserSettings(_backgroundService);
    	
		mNextLocIsHome=false;			// The next "location" update would be the user's home location
		mDisableWifiAS=false;			// Initialize "disable wifi after scan"
		mScansLeft=0;					// Do not initialize with any scans
		
        // If we have already determined the location of the user's home (NEVER shared with us, and only
        // stored locally on your phone), then we read it from the application settings.
        mHomeLoc = _settings.getHomeLocation();
        
        locationManager = (LocationManager) _backgroundService.getSystemService(Context.LOCATION_SERVICE);
        
        // Setup a location manager to receive location updates.
        wifi = (WifiManager) _backgroundService.getSystemService(Context.WIFI_SERVICE);
        
        // Create a broadcast receiver to listen for wifi scan results. We don't invoke them, we only passively
        // listen whenever they become available.
        _backgroundService.registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent) 
            {
            	if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
	               if((scan_result = wifi.getScanResults())==null)
	            	   return;
	               
	               home_ssid = _settings.getHomeSSID();
	               
	               Log.d(TAG, "Scan result received, current scan: " + Integer.toString(mScansLeft));
	               
	               if(--mScansLeft>0) {		// If there are more scans left....
	            	   Log.d(TAG, "Triggering another scan...");
	            	   wifi.startScan();	// scan again.
	               } else {								// There are no scans left.
	            	   Log.d(TAG, "Finished with the scans...");
		               if(mDisableWifiAS) {				// If the user had Wifi disabled, disable it again
		            	   Log.d(TAG, "Trying to disable Wifi");
		            	   while(wifi.isWifiEnabled()) { wifi.setWifiEnabled(false); }
		            	   mDisableWifiAS=false;		// Reset the wifi disable state.
		               }
		               mScansLeft=0;
	               }
	            	   
	               if(home_ssid==null) // If it is still null, then the user still hasn't set it
	            	   return;
	               
	               for(ScanResult result : scan_result) {
	            	   if(result.SSID.replaceAll("^\"|\"$", "").equals(home_ssid)) {
	            		   _backgroundService.home();	// Their home network is in the list, so they must be home  
	            		   return;
	            	   }
	               }
            	}
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); 
        
        mIntent=new Intent("LOC_OBTAINED");
        mPendingIntent=PendingIntent.getBroadcast(_backgroundService,0,mIntent,0);
        _backgroundService.registerReceiver(new BroadcastReceiver(){
            public void onReceive(Context arg0, Intent arg1)
            {
            	Bundle bundle=arg1.getExtras();
                Location location=(Location)bundle.get(LocationManager.KEY_LOCATION_CHANGED);
              	onLocationChanged(location);
            }
           },new IntentFilter("LOC_OBTAINED"));

        mLocationTimer.scheduleAtFixedRate(new PeriodicUpdate(), 0, LOCATION_UPDATE_INTERVAL);
        mPowerManager = (PowerManager) _backgroundService.getSystemService(Context.POWER_SERVICE);
        changeUpdateInterval(LOCATION_UPDATE_INTERVAL);   
    }
    
    
    // This runs when our Wifi check timer expires, this is once every 15 minutes and *only*
    // used if we do not yet know the location of the home.
    private class PeriodicUpdate extends TimerTask
    { 
        public void run() 
        {
        	// We still do not have the home location, so we keep periodically scanning
        	if(mHomeLoc==null) {
        		_backgroundService.mPhoneIsInTheHome=false;	// Phone can't be in the home if we don't have the location
        		triggerScan(!wifi.isWifiEnabled());	// Trigger a scan
        	}	
        	// We have the home location, so we can cancel this thread
        	else {
        		cancel();
        	}
        }
    }   
    
    public void changeUpdateInterval(long interval) {
    	locationManager.removeUpdates(mPendingIntent);
    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, 0, mPendingIntent);
    }
    

    // This triggers a wifi scan.  If the wifi is disabled, it enables it for the duration of
    // a single scan and then disables it again.  This likely only takes 200ms total from the time
    // to enable, scan, and get a result, and then disable it again.  If the wifi is already enabled,
    // then it simply triggers the scan.
    public void triggerScan(boolean disable_after_scan) {
    	mDisableWifiAS=disable_after_scan;
        boolean wifi_enabled=wifi.isWifiEnabled();
        if(!wifi_enabled) {
        	wifi.setWifiEnabled(true);
        	Log.d(TAG, "Enabling Wifi, disable after scan: " + Boolean.toString(mDisableWifiAS));
        }
        while(!wifi.isWifiEnabled()) {}
        mScansLeft=NUM_SCANS;
        wifi.startScan();
    }
    
	
	// We have an incoming location update.  If we have not yet saved the user's home location (WHICH
	// IS NEVER SHARED WITH US - it is only stored locally on your phone), then we save it.  Otherwise
	// we check the distance of the current location with the home location to detect if the user's
	// phone is in their home.
    public void onLocationChanged(Location location) {
    	
    	boolean associatedToHomeAP=false;

    	if(location==null)	// The location must not be null
    		return;
    	
    	Log.d(TAG, "Got a location: (" + location.getLatitude() + "," + location.getLongitude() + ") .. Accuracy: " + Double.toString(location.getAccuracy()));
    	
    	// First, if we are associated to an access point that is the same name of the user's home access
    	// point, then we consider them home and save locations with a greater accuracy than what we have
    	WifiInfo currWifi = wifi.getConnectionInfo();
    	ConnectivityManager connManager = (ConnectivityManager) _backgroundService.getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	if(wifiInfo.isConnected() && currWifi.getSSID().equals(_settings.getHomeSSID())) {
    		associatedToHomeAP=true;
    		_settings.setHomeWifiFreq(Wifi.getOperationalFreq("wlan0"));
    	}
    	
    	_settings.setLastLocation(location);
    	
    	if(location.getAccuracy()>100)	// Do nothing with bad accuracy
    		return;

    	// If we have marked the next location as home, we will save it only if we are associated to the access point.
    	// This tries to help in scenarios with SSIDs that are too general, and to improve location accuracy.
    	if(mNextLocIsHome && associatedToHomeAP) {
    		Log.d(TAG, "Saving the location of the home");
    		_settings.setHomeLocation(location);
    		mHomeLoc=location;
    		mNextLocIsHome=false;
    		_backgroundService.home();
    		changeUpdateInterval(LOCATION_UPDATE_INTERVAL);  // Once we get the location, we slow down updates.
    	}
    	
    	// If we are in the home and we got a location update that is more accurate than our previously stored one.
		if(associatedToHomeAP && location.getAccuracy()<=mHomeLoc.getAccuracy()) 
			_settings.setHomeLocation(location);   
		
		// We are always home when we are associated to the access point.
		if(associatedToHomeAP)
			_backgroundService.home();  // If we are connected to their AP, we are home regardless of location info
    	
    	if(mHomeLoc!=null) {
    		if(mHomeLoc.distanceTo(location)<=LOCATION_TOLERANCE)
    			_backgroundService.home();
    		else
    			_backgroundService.notHome();
    	}
    }    
}
