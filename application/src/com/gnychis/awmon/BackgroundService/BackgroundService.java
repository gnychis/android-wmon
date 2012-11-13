package com.gnychis.awmon.BackgroundService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.GUIs.MainInterface;
import com.gnychis.awmon.HardwareHandlers.HardwareHandler;
import com.stericson.RootTools.RootTools;

@SuppressWarnings("unused")
public class BackgroundService extends Service {
	
	// Need a binder so that the main activity can communicate with the server
	private final IBinder _binder = new BackgroundServiceBinder();

    public static MainInterface _mainInterface;
    static BackgroundService _this;
    private MotionDetector _motionDetector;
    private LocationMonitor _locationMonitor;
    public HardwareHandler _hardwareHandler;
    private ScanManager _scanManager;
    
    public static String TAG = "AWMonBackground";
	public static final String SYSTEM_INITIALIZED = "awmon.system.initialized";

    public static final boolean DEBUG=true;
    
    private UserSettings _settings;
    
    ServiceState _serviceState;
	public enum ServiceState {
		INITIALIZING,
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
    	_serviceState=ServiceState.INITIALIZING;
    	
    	// Finally, initialize and link some of the libraries (which can take a while)
    	InitLibraries init_thread = new InitLibraries();
    	init_thread.execute(this);
    	        
    	// Register a receiver which will be notified when the phone is getting shut down
        registerReceiver(new BroadcastReceiver()
        { @Override public void onReceive(Context context, Intent intent) { cleanup(); }
        }, new IntentFilter(Intent.ACTION_SHUTDOWN));
        
    	// Register a receiver which will be notified when the phone is getting shut down
        registerReceiver(new BroadcastReceiver()
        { @Override public void onReceive(Context context, Intent intent) { systemInitialized(); }
        }, new IntentFilter(BackgroundService.SYSTEM_INITIALIZED));
        
        // Register a receiver to get updates on the location
        registerReceiver(locationUpdate, new IntentFilter(LocationMonitor.LOCATION_UPDATE));
    }
    
    // Only initialize the classes after the entire system is initialized (libraries are linked)
    private void systemInitialized() {	    
		_settings = new UserSettings(this);
		_motionDetector = new MotionDetector(this);
		_locationMonitor = new LocationMonitor(this);
		_hardwareHandler = new HardwareHandler(this);
		_scanManager = new ScanManager(this, _hardwareHandler);
		_serviceState=ServiceState.IDLE;
    }
    
    public ServiceState getSystemState() { return _serviceState; }
    
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
    
	public String getAppUser() {
		try {
			List<String> res = RootTools.sendShell("ls -l /data/data | grep " + MainInterface._app_name,0);
			return res.get(0).split(" ")[1];
		} catch(Exception e) {
			return "FAIL";
		}
	}

	static public ArrayList<String> runCommand(String c) {
		ArrayList<String> res = new ArrayList<String>();
		try {
			// First, run the command push the result to an ArrayList
			List<String> res_list = RootTools.sendShell(c,0);
			Iterator<String> it=res_list.iterator();
			while(it.hasNext()) 
				res.add((String)it.next());
			
			res.remove(res.size()-1);
			
			// Trim the ArrayList of an extra blank lines at the end
			while(true) {
				int index = res.size()-1;
				if(index>=0 && res.get(index).length()==0)
					res.remove(index);
				else
					break;
			}
			return res;
			
		} catch(Exception e) {
			Log.e("AWMon", "error writing to RootTools the command: " + c, e);
			return null;
		}
	}

	public static void setMainActivity(MainInterface activity) { _mainInterface = activity; }
	
}
