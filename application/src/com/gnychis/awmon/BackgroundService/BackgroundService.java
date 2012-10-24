package com.gnychis.awmon.BackgroundService;

import java.util.Map;
import java.util.concurrent.Semaphore;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.UserSettings;

public class BackgroundService extends Service {

    public static AWMon _awmon;
    static BackgroundService _this;
    private MotionDetector _motionDetector;
    private LocationMonitor _locationMonitor;
    
    public static String TAG = "AWMonBackground";

    public static final boolean DEBUG=true;
    
    private UserSettings _settings;

    public boolean mPhoneIsInTheHome;
    
    Map<String,Object> mLastState;
    Semaphore _data_lock;
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	_this=this;
    	
    	Log.d(TAG, "Background service is now running");
    	    	    	    	    
    	_settings = new UserSettings(this);
    	_motionDetector = new MotionDetector(this);
    	_locationMonitor = new LocationMonitor(this);

    	mPhoneIsInTheHome=false;		// To detect when the user is home
    	
    	_data_lock = new Semaphore(1,true);
        
        registerReceiver(new BroadcastReceiver()
        {
        	@Override
        	public void onReceive(Context context, Intent intent)
        	{
        		cleanup();
        	}
        }, new IntentFilter(Intent.ACTION_SHUTDOWN));
             
    }
    
    // The user's phone is in the home, either based on localization information or the fact that their
    // Wifi access point is within range. If we don't have the location of the home saved yet 
    // (which is NEVER sent back to us, it's only kept locally on the user's phone), 
    // then we save it in the application preferences.
    public void home() {
    	Log.d(TAG, "Got an update that the phone is in the home");
    	if(!mPhoneIsInTheHome) {
    		_motionDetector.registerSensors();
    	}
    	mPhoneIsInTheHome=true;
    	_settings.setPhoneIsInHome(true);
    }
    
    // The user's phone is not in the home based on localization information.
    public void notHome() {
    	Log.d(TAG, "The phone is not in the home");
    	if(mPhoneIsInTheHome) {
    		_motionDetector.unregisterSensors();
    	}
    	if(_awmon!=null && DEBUG) _awmon.findViewById(R.id.main_id).setBackgroundColor(Color.BLACK);
    	mPhoneIsInTheHome=false;
    	_settings.setPhoneIsInHome(false);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	cleanup();
    }
    
    // When the phone is being shut down or the application is being destroyed, we perform a cleanup
    // action and note the change in state of the phone.
    private void cleanup() {
    	_motionDetector.unregisterSensors();
    }
    
    @Override
    public IBinder onBind(Intent intent) { return null; }
	public static void setMainActivity(AWMon activity) { _awmon = activity; }
	
}
