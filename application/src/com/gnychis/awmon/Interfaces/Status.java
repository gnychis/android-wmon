package com.gnychis.awmon.Interfaces;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.UserSettings;

public class Status extends Activity implements OnClickListener {
	
	UserSettings _settings;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.status);
	  
	  _settings = new UserSettings(this);
	  
	  // Show the home SSID
	  ((TextView) findViewById(R.id.homeSSID)).append(_settings.getHomeSSID());
	  
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
	  if(_settings.phoneIsInHome())
		  ((TextView) findViewById(R.id.Status_txt_phoneIsInHome)).append("Yes");
	  else
		  ((TextView) findViewById(R.id.Status_txt_phoneIsInHome)).append("No");
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
