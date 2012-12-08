package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.BackgroundService.MotionDetector;
import com.gnychis.awmon.Core.UserSettings;

public class Status extends Activity implements OnClickListener {
	
	UserSettings _settings;
	WifiManager _wifi;
    Date _activityStartTime;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.status);
	  
	  _wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	  
	  _settings = new UserSettings(this);
	  
	  // Show the home SSID
	  ((TextView) findViewById(R.id.homeSSID)).append(_settings.getHomeSSID() + "  (" + String.valueOf(_settings.getHomeWifiFreq()) + "MHz)");
	  
	  // State whether or not we have a record of the home's location
	  Location homeLoc = _settings.getHomeLocation();
	  if(homeLoc==null)
		  ((TextView) findViewById(R.id.haveHomeLocation)).append("No");
	  else
		  ((TextView) findViewById(R.id.haveHomeLocation)).append("Yes\n(" 	+ Double.toString(homeLoc.getLatitude())
				  														  	+ ","
				  														  	+ Double.toString(homeLoc.getLongitude())
				  														  	+ ")\nAccuracy: " 
				  														  	+ Double.toString(homeLoc.getAccuracy()));
	  
	  // Make the location clickable to bring up a map
	  ((TextView) findViewById(R.id.haveHomeLocation)).setClickable(true);
	  ((TextView) findViewById(R.id.Status_txt_LastLocation)).setClickable(true);
	  
	  // Display information about the last location
	  Location lastLoc = _settings.getLastLocation();
	  ((TextView) findViewById(R.id.Status_txt_LastLocation)).append("\n(" 	+ Double.toString(lastLoc.getLatitude())
			  																+ ","
			  																+ Double.toString(lastLoc.getLongitude())
			  																+ ")\n    Accuracy: "
			  																+ Double.toString(_settings.getLastLocation().getAccuracy()));
	  if(_settings.haveHomeLocation()) {
		  ((TextView) findViewById(R.id.Status_txt_LastLocation)).append("\n    Distance: " 
				  																+ Double.toString(homeLoc.distanceTo(_settings.getLastLocation()))
				  																);
	  }
	  
	  // Set whether or not the phone is in the home
	  if(_settings.phoneIsInHome()) {
		  ((TextView) findViewById(R.id.Status_txt_phoneIsInHome)).append("Yes");
		  ((TextView) findViewById(R.id.Status_txt_lastRSSI)).setText("Last RSSI:   " + Integer.toString(_wifi.getConnectionInfo().getRssi()));
	  } else {
		  ((TextView) findViewById(R.id.Status_txt_phoneIsInHome)).append("No");
	  }
	  
	  
	}
	
    private BroadcastReceiver sensorUpdate = new BroadcastReceiver() {
        @Override @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
        	ArrayList<Double> sensor_vals = (ArrayList<Double>) intent.getExtras().get("sensor_vals");
        	((TextView) findViewById(R.id.Status_txt_X)).setText("X:  " + Double.toString(sensor_vals.get(0)));
        	((TextView) findViewById(R.id.Status_txt_Y)).setText("Y:  " + Double.toString(sensor_vals.get(1)));
        	((TextView) findViewById(R.id.Status_txt_Z)).setText("Z:  " + Double.toString(sensor_vals.get(2)));
        	((TextView) findViewById(R.id.Status_txt_OX)).setText("X:  " + Double.toString(sensor_vals.get(3)));
        	((TextView) findViewById(R.id.Status_txt_OY)).setText("Y:  " + Double.toString(sensor_vals.get(4)));
        	((TextView) findViewById(R.id.Status_txt_OZ)).setText("Z:  " + Double.toString(sensor_vals.get(5)));
        	((TextView) findViewById(R.id.Status_txt_lastRSSI)).setText("Last RSSI:   " + Integer.toString(_wifi.getConnectionInfo().getRssi()));
        }
    };   
    
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(sensorUpdate);
	}	

	@Override
	public void onResume() {
		super.onResume();		
		registerReceiver(sensorUpdate, new IntentFilter(MotionDetector.SENSOR_UPDATE));
	}
	
	public void clickedMeasurements(View v) {
		Intent i;
		i = new Intent(Status.this, Measurements.class);
        startActivity(i);
        finish();
	}
	
	public void clickedSnapshots(View v) {
		Intent i;
		i = new Intent(Status.this, SnapshotList.class);
		startActivity(i);
		finish();
	}

	// Check for clicks on various things on the status view
	public void onClick(View view) {
		String uri;
		Intent intent;
		switch(view.getId()) {
			case R.id.haveHomeLocation:
				Location homeLoc = _settings.getHomeLocation();
				uri = String.format("geo:%f,%f?z=22&q=%f,%f(Home)", 
											homeLoc.getLatitude(), 
											homeLoc.getLongitude(),
											homeLoc.getLatitude(),
											homeLoc.getLongitude());
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				this.startActivity(intent);				
				break;
				
			case R.id.Status_txt_LastLocation:
				Location lastLoc = _settings.getLastLocation();
				uri = String.format("geo:%f,%f?z=22&q=%f,%f(LastLocation)", 
						lastLoc.getLatitude(), 
						lastLoc.getLongitude(),
						lastLoc.getLatitude(),
						lastLoc.getLongitude());
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				this.startActivity(intent);
				break;
		}
	}
}
