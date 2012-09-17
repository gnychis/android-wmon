package com.gnychis.awmon.Interfaces;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.UserSettings;

public class Status extends Activity {
	
	UserSettings _settings;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.status);
	  
	  _settings = new UserSettings(this);
	  
	  ((TextView) findViewById(R.id.homeSSID)).append(_settings.getHomeSSID());
	}

}
