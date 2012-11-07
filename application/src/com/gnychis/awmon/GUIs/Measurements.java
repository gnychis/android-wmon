package com.gnychis.awmon.GUIs;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.gnychis.awmon.R;
import com.gnychis.awmon.BackgroundService.MotionDetector;

public class Measurements extends Activity {

	WifiManager _wifi;
	
	boolean _measuring;
	boolean _active_orientationRSSI;
    private FileOutputStream _data_ostream;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  setContentView(R.layout.measurements);
		  _active_orientationRSSI=false;
		  _measuring=false;
		  _wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		  
		  // Initialize the button colors to "inactive"
		  ((Button) findViewById(R.id.Measurements_btn_OrientationRSSI)).setBackgroundColor(Color.GRAY);
	}
	
	public void clickedOrientationRSSI(View v) {
		if(!_active_orientationRSSI) {
			_active_orientationRSSI=true;
			_measuring=true;
			registerReceiver(sensorUpdate, new IntentFilter(MotionDetector.SENSOR_UPDATE));
			((Button) findViewById(R.id.Measurements_btn_OrientationRSSI)).setBackgroundColor(Color.RED);
			
			// Open a JSON file for logging
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			String currentDateandTime = sdf.format(new Date());
			try {
				_data_ostream = openFileOutput("orientationRSSI_" + currentDateandTime + ".json", Context.MODE_PRIVATE);
			} catch(Exception e) {finish();}
			
		} else {
			_active_orientationRSSI=false;
			_measuring=false;
			unregisterReceiver(sensorUpdate);
			try { _data_ostream.close(); } catch(Exception e) {finish();}
			((Button) findViewById(R.id.Measurements_btn_OrientationRSSI)).setBackgroundColor(Color.GRAY);
		}
	}
	
    private BroadcastReceiver sensorUpdate = new BroadcastReceiver() {
        @Override @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
        	ArrayList<Double> sensor_vals = (ArrayList<Double>) intent.getExtras().get("sensor_vals");
        	JSONObject jstate = new JSONObject();
        	try {
        		jstate.put("AccelX", sensor_vals.get(0));
        		jstate.put("AccelY", sensor_vals.get(1));
        		jstate.put("AccelZ", sensor_vals.get(2));
        		jstate.put("OrientX", sensor_vals.get(3));
        		jstate.put("OrientY", sensor_vals.get(4));
        		jstate.put("OrientZ", sensor_vals.get(5));
        		jstate.put("RSSI", _wifi.getConnectionInfo().getRssi());
        		_data_ostream.write(jstate.toString().getBytes());
        		_data_ostream.write("\n".getBytes());
        	} catch(Exception e) {finish();}
        }
    };   
    
	@Override
	public void onPause() {
		super.onPause();
	}	

	@Override
	public void onResume() {
		super.onResume();
	}
}
