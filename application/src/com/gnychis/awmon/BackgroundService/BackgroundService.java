package com.gnychis.awmon.BackgroundService;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.Core.UserSettings;

public class BackgroundService extends Service {
	
	// Need a binder so that the main activity can communicate with the server
	private final IBinder _binder = new BackgroundServiceBinder();

    public static AWMon _awmon;
    static BackgroundService _this;
    private MotionDetector _motionDetector;
    private LocationMonitor _locationMonitor;
    public DeviceHandler _deviceHandler;
    
    public static String TAG = "AWMonBackground";

    public static final boolean DEBUG=true;
    
    private UserSettings _settings;
    
    ServiceState _serviceState;
	public enum ServiceState {
		IDLE,
		PROXIMITY_DETECTION,
		TAKING_SNAPSHOT,
	}
       
    @Override
    public void onCreate() {
    	super.onCreate();
    	_this=this;
    	
    	Log.d(TAG, "Background service is now running");
    	
    	// Initialize the states
    	_serviceState=ServiceState.IDLE;
    	    	    	    	    
    	// Make a few instances to our helper classes
    	_settings = new UserSettings(this);
    	_motionDetector = new MotionDetector(this);
    	_locationMonitor = new LocationMonitor(this);
    	_deviceHandler = new DeviceHandler(this);
    	        
        registerReceiver(new BroadcastReceiver()
        {
        	@Override
        	public void onReceive(Context context, Intent intent)
        	{
        		cleanup();
        	}
        }, new IntentFilter(Intent.ACTION_SHUTDOWN));
        
        registerReceiver(locationUpdate, new IntentFilter(LocationMonitor.LOCATION_UPDATE));
    }
    
    // This receives updates when the phone either enters the home or leaves the home
    private BroadcastReceiver locationUpdate = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	LocationMonitor.StateChange state = (LocationMonitor.StateChange) intent.getExtras().get("state");
        	
        	switch(state) {
	        	case ENTERING_HOME:
	        		break;
	        		
	        	case LEAVING_HOME:
	        		break;
        	}     	
        }
    };   
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	cleanup();
    }
    
    // When the phone is being shut down or the application is being destroyed, we perform a cleanup
    // action and note the change in state of the phone.
    private void cleanup() {
    	_motionDetector.unregisterSensors();
    	unregisterReceiver(locationUpdate);
    }
    
    public class BackgroundServiceBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return _binder;
    }
    
	public static void setMainActivity(AWMon activity) { _awmon = activity; }
	
}
