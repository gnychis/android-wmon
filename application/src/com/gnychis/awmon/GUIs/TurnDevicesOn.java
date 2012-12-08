package com.gnychis.awmon.GUIs;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.gnychis.awmon.R;

public class TurnDevicesOn extends Activity {
	
	private final String TAG = "TurnDevicesOn";
    Date _activityStartTime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.turn_devices_on);
	}
	
	public void devicesOnClicked(View v) {
		Intent i = new Intent(TurnDevicesOn.this, YourDevices.class);
        startActivity(i);
    	finish();
	}
	
    @Override
    public void onBackPressed() {
    	
    	Intent i = new Intent(TurnDevicesOn.this, HomeLocation.class);
        startActivity(i);
    	finish();
    }
}
