package com.gnychis.awmon.BackgroundService;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.UserSettings;

public class BackgroundService extends Service implements SensorEventListener {

    public static AWMon mMainActivity;
    static BackgroundService _this;
    PowerManager mPowerManager;
    
    public static String TAG = "AWMonBackground";
    
    PendingIntent mPendingIntent;
    Intent mIntent;

    public final int LOCATION_TOLERANCE=150;			// in meters
    public final int LOCATION_UPDATE_INTERVAL=120000; //900000;	// in milliseconds (15 minutes)
    private final boolean DEBUG=true;

	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private final float NOISE = (float) 0.25;
    
    private UserSettings _settings;

    public boolean mDisableWifiAS;
    public int mScansLeft;
    public final int NUM_SCANS=4;
    
    // Used to keep the location of the home so that we know when the user is home.
    // However, we NEVER retrieve this information from the phone.  Your home location
    // is only kept privately on your phone.
    boolean mNextLocIsHome;
    Location mHomeLoc;

    WifiManager wifi;
    boolean mPhoneIsInTheHome;
    List<ScanResult> scan_result;
    String home_ssid;
    
    LocationManager locationManager;
    LocationListener locationListener;
    
    private static Timer mLocationTimer = new Timer(); 
    
    Map<String,Object> mLastState;
    Semaphore _data_lock;
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	_this=this;
    	
    	Log.d(TAG, "Background service is now running");
    	    	    	    	    
    	_settings = new UserSettings(this);
    	    	
        mInitialized = false;			// Related to initializing the sensors
    	mNextLocIsHome=false;			// The next "location" update would be the user's home location
    	mDisableWifiAS=false;			// Initialize "disable wifi after scan"
    	mScansLeft=0;					// Do not initialize with any scans
    	mPhoneIsInTheHome=false;		// To detect when the user is home
    	
    	_data_lock = new Semaphore(1,true);
    	
    	// Set up listeners to detect movement of the phone
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        // If we have already determined the location of the user's home (NEVER shared with us, and only
        // stored locally on your phone), then we read it from the application settings.
        mHomeLoc = _settings.getHomeLocation();
        
        // Setup a location manager to receive location updates.
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        // Create a broadcast receiver to listen for wifi scan results. We don't invoke them, we only passively
        // listen whenever they become available.
        registerReceiver(new BroadcastReceiver()
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
	            		   home();	// Their home network is in the list, so they must be home  
	            		   return;
	            	   }
	               }
            	}
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); 
        
        registerReceiver(new BroadcastReceiver()
        {
        	@Override
        	public void onReceive(Context context, Intent intent)
        	{
        		cleanup();
        	}
        }, new IntentFilter(Intent.ACTION_SHUTDOWN));
        
        mIntent=new Intent("LOC_OBTAINED");
        mPendingIntent=PendingIntent.getBroadcast(this,0,mIntent,0);
        registerReceiver(new BroadcastReceiver(){
            public void onReceive(Context arg0, Intent arg1)
            {
            	Bundle bundle=arg1.getExtras();
                Location location=(Location)bundle.get(LocationManager.KEY_LOCATION_CHANGED);
              	onLocationChanged(location);
            }
           },new IntentFilter("LOC_OBTAINED"));

        mLocationTimer.scheduleAtFixedRate(new PeriodicUpdate(), 0, LOCATION_UPDATE_INTERVAL);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
        		mPhoneIsInTheHome=false;	// Phone can't be in the home if we don't have the location
        		triggerScan(!wifi.isWifiEnabled());	// Trigger a scan
        	}	
        	// We have the home location, so we can cancel this thread
        	else {
        		cancel();
        	}
        }
    }   
    
    private void changeUpdateInterval(long interval) {
    	locationManager.removeUpdates(mPendingIntent);
    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, interval, 0, mPendingIntent);
    }
    
    // The user's phone is in the home, either based on localization information or the fact that their
    // Wifi access point is within range. If we don't have the location of the home saved yet 
    // (which is NEVER sent back to us, it's only kept locally on the user's phone), 
    // then we save it in the application preferences.
    private void home() {
    	Log.d(TAG, "Got an update that the phone is in the home");
    	if(!mPhoneIsInTheHome)
    		mSensorManager.registerListener(_this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
    	mPhoneIsInTheHome=true;
    	_settings.setPhoneIsInHome(true);
    	
    	if(mHomeLoc==null) {
 		   mNextLocIsHome=true;
 		   changeUpdateInterval(60000);
    	}
    }
    
    // The user's phone is not in the home based on localization information.
    private void notHome() {
    	Log.d(TAG, "The phone is not in the home");
    	if(mPhoneIsInTheHome) {
    		mSensorManager.unregisterListener(_this);
    	}
    	if(mMainActivity!=null && DEBUG) mMainActivity.findViewById(R.id.main_id).setBackgroundColor(Color.BLACK);
    	mPhoneIsInTheHome=false;
    	_settings.setPhoneIsInHome(false);
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
    	SupplicantState supState = currWifi.getSupplicantState();
    	if(WifiInfo.getDetailedStateOf(supState) == NetworkInfo.DetailedState.CONNECTED)
    		if(currWifi.getSSID().equals(_settings.getHomeSSID()))
    			associatedToHomeAP=true;
    	
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
    		home();
    		changeUpdateInterval(LOCATION_UPDATE_INTERVAL);  // Once we get the location, we slow down updates.
    	}
    	
		if(associatedToHomeAP && location.getAccuracy()<mHomeLoc.getAccuracy()) 
			_settings.setHomeLocation(location);   
		
		if(associatedToHomeAP)
			home();  // If we are connected to their AP, we are home regardless of location info
    	
    	if(mHomeLoc!=null) {
    		if(mHomeLoc.distanceTo(location)<=LOCATION_TOLERANCE)
    			home();
    		else
    			notHome();
    	}
    }    

    // We have an update on the sensor data.  We check that the movement of the phone
    // exceeds a threshold, and if so we consider the phone as actively moving, otherwise
    // we consider it to be stable (not moving).
	@Override
	public void onSensorChanged(SensorEvent event) {
		boolean movement=false;
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			mInitialized = true;
		} else {
			float deltaX = Math.abs(mLastX - x);
			float deltaY = Math.abs(mLastY - y);
			float deltaZ = Math.abs(mLastZ - z);
			if (deltaX < NOISE) deltaX = (float)0.0;
			if (deltaY < NOISE) deltaY = (float)0.0;
			if (deltaZ < NOISE) deltaZ = (float)0.0;
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			
			if (deltaX > deltaY) {  // We moved horizontally
				if(mMainActivity!=null && DEBUG) mMainActivity.findViewById(R.id.main_id).setBackgroundColor(Color.RED);
				movement=true;
			} else if (deltaY > deltaX) {  // We moved vertically
				if(mMainActivity!=null && DEBUG) mMainActivity.findViewById(R.id.main_id).setBackgroundColor(Color.RED);
				movement=true;
			} else {
				if(mMainActivity!=null && DEBUG) mMainActivity.findViewById(R.id.main_id).setBackgroundColor(Color.BLACK);
				movement=false;
			}
		}
	}

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	cleanup();
    }
    
    // When the phone is being shut down or the application is being destroyed, we perform a cleanup
    // action and note the change in state of the phone.
    private void cleanup() {
    	mSensorManager.unregisterListener(this);
    }
    
    @Override
    public IBinder onBind(Intent intent) { return null; }
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	public static void setMainActivity(AWMon activity) { mMainActivity = activity; }
	
}
