package com.gnychis.awmon.GUIs;

// do a random port number for pcapd

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.gnychis.awmon.R;
import com.gnychis.awmon.BackgroundService.BackgroundService;
import com.gnychis.awmon.BackgroundService.BackgroundService.BackgroundServiceBinder;
import com.gnychis.awmon.BackgroundService.ScanManager;
import com.gnychis.awmon.Core.DBAdapter;
import com.gnychis.awmon.Core.ScanRequest;
import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;
import com.gnychis.awmon.InterfaceMerging.InterfaceMergingManager;

public class MainInterface extends Activity implements OnClickListener {
	
	private static final String TAG = "AWMon";
	public static String _app_name = "com.gnychis.awmon";
	public static final String THREAD_MESSAGE = "awmon.thread.message";
	
	// Internal Android mechanisms for settings/storage
	public DBAdapter _db;
	public UserSettings _settings;
	
	// Related to communication and tracking of the background service
	public BackgroundService _backgroundService;
	private boolean mBound=false;
	
	// Interface related
	private ProgressDialog _pd;
	public TextView textStatus;
		
	public enum ThreadMessages {	
		SHOW_TOAST,
		SHOW_PROGRESS_DIALOG,
		CANCEL_PROGRESS_DIALOG,
		INCREMENT_SCAN_PROGRESS,
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
           	
        // Setup the database
    	_db = new DBAdapter(this);
    	_db.open();
    	
    	// Initialize the user settings
    	_settings = new UserSettings(this);
      
		// Setup UI
		textStatus = (TextView) findViewById(R.id.textStatus);
		textStatus.setText("");
		((Button) findViewById(R.id.buttonAddNetwork)).setOnClickListener(this);
		((Button) findViewById(R.id.buttonManageDevs)).setOnClickListener(this);
		((Button) findViewById(R.id.buttonSettings)).setOnClickListener(this);
		((Button) findViewById(R.id.buttonStatus)).setOnClickListener(this);
		
    	// Start the background service.  This MUST go after the linking of the libraries.
        BackgroundService.setMainActivity(this);
        startService(new Intent(this, BackgroundService.class));
    }
    
    // This is called when we are bound to the background service, which allows us to check
    // its state and know what to do with the main activity.
    public void serviceBound() {
    	if(!mBound)
    		return;
    	
    	// If the background service is initializing, pop up a spinner which we will cancel after initialized
    	if(_backgroundService.getSystemState()==BackgroundService.ServiceState.INITIALIZING)
    		showProgressDialog("Initializing application, please wait...");
    	
    	// If the background service is already initialized, then we can go ahead call the post-init
    	if(_backgroundService.getSystemState()==BackgroundService.ServiceState.IDLE)
    		systemInitialized();
    }
    
    // This runs after the initialization of the libraries, etc.
    public void systemInitialized() {
    	
    	if(_pd!=null)
    		_pd.dismiss();

    	if(_settings.haveUserSettings())  // Do we have user settings?
    		return;
    	
    	// If we do not have the user settings, we open up an activity to query for them
		Intent i = new Intent(MainInterface.this, Welcome.class);
        startActivity(i);        
    }
    
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver _initializedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	systemInitialized(); 	
        }
    }; 
    
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver _messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String msg;
        	ThreadMessages tm = (ThreadMessages) intent.getExtras().get("type");
        	
        	switch(tm) {
        		case SHOW_TOAST:
        			msg = (String) intent.getExtras().get("msg");
        			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        			break;
        		
        		case SHOW_PROGRESS_DIALOG:
        			msg = (String) intent.getExtras().get("msg");
        			showProgressUpdate(msg);
        			break;
        		
        		case CANCEL_PROGRESS_DIALOG:
        			if(_pd!=null) _pd.dismiss();
        			break;
        			
				case INCREMENT_SCAN_PROGRESS:
					if(_pd!=null) _pd.incrementProgressBy(1);
					break;
        	}     	
        }
    }; 
    
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BackgroundServiceBinder binder = (BackgroundServiceBinder) service;
            _backgroundService = binder.getService();
            mBound = true;
            serviceBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
    
    // Everything related to clicking buttons in the main interface
	public void onClick(View view) {
		Intent i;
		
		switch(view.getId()) {
			case R.id.buttonAddNetwork:
				clickAddNetwork();
				break;
			
			case R.id.buttonManageDevs:
				i = new Intent(MainInterface.this, ManageNetworks.class);
		        startActivity(i);
				break;
				
			case R.id.buttonSettings:
				i = new Intent(MainInterface.this, Welcome.class);
				startActivity(i);
				break;
				
			case R.id.buttonStatus:
				i = new Intent(MainInterface.this, Status.class);
				startActivity(i);
				break;
		}
	}
	
	public void scanResultsAvailable() {
		
	}
	
	public void showProgressUpdate(String s) {
		_pd = ProgressDialog.show(this, "", s, true, false);  
	}
	

	@Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, BackgroundService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
	@Override
	public void onResume() { 
		super.onResume(); 
		registerReceiver(_messageReceiver, new IntentFilter(MainInterface.THREAD_MESSAGE));
		registerReceiver(_initializedReceiver, new IntentFilter(BackgroundService.SYSTEM_INITIALIZED));
		registerReceiver(_deviceScanReceiver, new IntentFilter(ScanManager.SCAN_RESPONSE));
		
	}
	public void onPause() { 
		super.onPause(); 
		Log.d(TAG, "onPause()"); 
		unregisterReceiver(_messageReceiver);
		unregisterReceiver(_initializedReceiver);
		unregisterReceiver(_deviceScanReceiver);
	}
	
    // A broadcast receiver to get messages from background service and threads
    private BroadcastReceiver _deviceScanReceiver = new BroadcastReceiver() {
    	@SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
    		
    		// First, get the response and see if the results are interfaces or devices.
    		ScanManager.ResultType resultType = (ScanManager.ResultType) intent.getExtras().get("type");
    		
    		if(resultType == ScanManager.ResultType.INTERFACES) {
	        	ArrayList<Interface> deviceScanResult = (ArrayList<Interface>) intent.getExtras().get("result");
	        	
	        	for(Interface iface : deviceScanResult) {
	        		Log.d(TAG, "Got a device (" + simplifiedClassName(iface.getClass()) + " - " + simplifiedClassName(iface._type) + "): " 
	        				   + iface._MAC 
	        				   + " - " + iface._IP
	        				   + " - " + iface._ifaceName
	        				   + " - " + iface._ouiName
	        				   );
	        	}
	        	if(_pd!=null)
	        		_pd.dismiss();
    		}
    		
    		if(resultType == ScanManager.ResultType.DEVICES) {
	        	ArrayList<Device> deviceScanResult = (ArrayList<Device>) intent.getExtras().get("result");
	        	
	        	for(Device device : deviceScanResult) {
	        		Log.d(TAG, "Got a device: " + device.getName());
	        		List<Interface> interfaces = device.getInterfaces();
	        		for(Interface iface : interfaces) {
		        		Log.d(TAG, "... interface (" + simplifiedClassName(iface.getClass()) + " - " + simplifiedClassName(iface._type) + "): " 
		        				   + iface._MAC 
		        				   + " - " + iface._IP
		        				   + " - " + iface._ifaceName
		        				   + " - " + iface._ouiName
		        				   );	
	        		}
	        	}
	        	if(_pd!=null)
	        		_pd.dismiss();
    		}
        }
    }; 
	
	public void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy()"); }

	// This triggers a scan through the networks to return a list of
	// networks and devices for a user to add for management.
	// FIXME: this should pop up the progress dialogue and just wait for the broadcast results
	public void clickAddNetwork() {
		
		// Send a request to start a device scan.  If one is not currently being done, it will start.
		// Otherwise, if one is already running we just await the result.
		ScanRequest request = new ScanRequest();
		request.setNameResolution(true);
		request.setMerging(true);
		request.send(this);
		
		// Start a progress dialogue which will be canceled when the scan result returns.
		showProgressDialog("Scanning for devices, please wait");
	}
	
	private void showProgressDialog(String message) { _pd = ProgressDialog.show(MainInterface.this, "", message, true, false); }
	
	public static void sendToastRequest(Context c, String message) {
		Intent i = new Intent();
		i.setAction(MainInterface.THREAD_MESSAGE);
		i.putExtra("type", MainInterface.ThreadMessages.SHOW_TOAST);
		i.putExtra("msg", message);
		c.sendBroadcast(i);
	}
	
	public static void sendProgressDialogRequest(Context c, String message) {
		Intent i = new Intent();
		i.setAction(MainInterface.THREAD_MESSAGE);
		i.putExtra("type", MainInterface.ThreadMessages.SHOW_PROGRESS_DIALOG);
		i.putExtra("msg", message);
		c.sendBroadcast(i);
	}
	
	public static void sendThreadMessage(Context c, ThreadMessages type) {
		Intent i = new Intent();
		i.setAction(THREAD_MESSAGE);
		i.putExtra("type", type);
		c.sendBroadcast(i);
	}
		
	public static String simplifiedClassName(Class<?> c) {
		String fullName = c.getName();
		String[] topName = fullName.split("\\.");
		if(topName.length==0)
			return fullName;
		return topName[topName.length-1];
	}
}