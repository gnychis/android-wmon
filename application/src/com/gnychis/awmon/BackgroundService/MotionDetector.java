package com.gnychis.awmon.BackgroundService;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.gnychis.awmon.R;

public class MotionDetector implements SensorEventListener {
	
	private static BackgroundService _backgroundService;

    public static final String SENSOR_UPDATE = "awmon.sensor.update";

	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mOrientation;
    private final float NOISE = (float) 0.25;
    
    public float[] mAValues;
    public float[] mMValues;
    
    public boolean _movement;
    
    public MotionDetector(BackgroundService bs) {
    	
    	_backgroundService=bs;
    	
        mInitialized = false;			// Related to initializing the sensors
        _movement = false;				// Start out with no movement

    	// Initialize some of the sensor data variables
    	mAValues = new float[3];
    	mMValues = new float[3];
    	
    	// Set up listeners to detect movement of the phone
        mSensorManager = (SensorManager) bs.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        _backgroundService.registerReceiver(locationUpdate, new IntentFilter(LocationMonitor.LOCATION_UPDATE));
    }
    
    // This receives updates when the phone either enters the home or leaves the home
    private BroadcastReceiver locationUpdate = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	LocationMonitor.StateChange state = (LocationMonitor.StateChange) intent.getExtras().get("state");
        	
        	switch(state) {
	        	case ENTERING_HOME:
	        		registerSensors();		// The phone is now in the home, let's start tracking movement
	        		break;
	        		
	        	case LEAVING_HOME:
	        		unregisterSensors();	// Don't track movement when the phone is not in the home (power savings)
	        		break;
        	}     	
        }
    };   
    
    public void registerSensors() {
		mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    public void unregisterSensors() {
		mSensorManager.unregisterListener(this);
    }

    // We have an update on the sensor data.  We check that the movement of the phone
    // exceeds a threshold, and if so we consider the phone as actively moving, otherwise
    // we consider it to be stable (not moving).
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		// Save the respective values
        switch (event.sensor.getType ()){
	        case Sensor.TYPE_ACCELEROMETER:
	            mAValues = event.values.clone ();
	            break;
	        case Sensor.TYPE_MAGNETIC_FIELD:
	            mMValues = event.values.clone ();
	            break;
        }
				
		// For calculation of the accelerometer
		float x = mAValues[0];
		float y = mAValues[1];
		float z = mAValues[2];
		
		// Calculate the orientation
		float[] rot = new float[16];
        float[] orientationValues = new float[3];
        SensorManager.getRotationMatrix (rot, null, mAValues, mMValues);
        SensorManager.getOrientation (rot, orientationValues);
        orientationValues[0] = (float)Math.toDegrees (orientationValues[0]);
        orientationValues[1] = (float)Math.toDegrees (orientationValues[1]);
        orientationValues[2] = (float)Math.toDegrees (orientationValues[2]);

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
			
			// Send out a broadcast with the change
			ArrayList<Double> values = new ArrayList<Double>(9);
			values.add((double)x); values.add((double)y); values.add((double)z);
			values.add((double)orientationValues[0]); values.add((double)orientationValues[1]);  values.add((double)orientationValues[2]); 
			Intent i = new Intent();
			i.setAction(SENSOR_UPDATE);
			i.putExtra("sensor_vals", values);
			_backgroundService.sendBroadcast(i);
			
			if (deltaX > deltaY) {  // We moved horizontally
				if(BackgroundService._mainInterface!=null && BackgroundService.DEBUG) BackgroundService._mainInterface.findViewById(R.id.main_id).setBackgroundColor(Color.RED);
				_movement=true;
			} else if (deltaY > deltaX) {  // We moved vertically
				if(BackgroundService._mainInterface!=null && BackgroundService.DEBUG) BackgroundService._mainInterface.findViewById(R.id.main_id).setBackgroundColor(Color.RED);
				_movement=true;
			} else {
				if(BackgroundService._mainInterface!=null && BackgroundService.DEBUG) BackgroundService._mainInterface.findViewById(R.id.main_id).setBackgroundColor(Color.BLACK);
				_movement=false;
			}
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	
}
