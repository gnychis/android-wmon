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
		  ((TextView) findViewById(R.id.haveHomeLocation)).append("Yes (" + Double.toString(homeLoc.getLatitude())
				  														  + ","
				  														  + Double.toString(homeLoc.getLongitude())
				  														  + ")");
	  
	  // Make the location clickable to bring up a map
	  ((TextView) findViewById(R.id.haveHomeLocation)).setClickable(true);
	  
	  // Set whether or not the phone is in the home
	  if(_settings.phoneIsInHome())
		  ((TextView) findViewById(R.id.Status_txt_phoneIsInHome)).append("Yes");
	  else
		  ((TextView) findViewById(R.id.Status_txt_phoneIsInHome)).append("No");
	}

	// Check for clicks on various things on the status view
	public void onClick(View view) {
		
		switch(view.getId()) {
			case R.id.haveHomeLocation:
				Location homeLoc = _settings.getHomeLocation();
				String uri = String.format("geo:%f,%f?z=18&q=%f,%f(Home)", 
											homeLoc.getLatitude(), 
											homeLoc.getLongitude(),
											homeLoc.getLatitude(),
											homeLoc.getLongitude());
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				this.startActivity(intent);
				
				Log.d("AWMonStatus", "Got a click on the location");
				break;
		}
	}
}
