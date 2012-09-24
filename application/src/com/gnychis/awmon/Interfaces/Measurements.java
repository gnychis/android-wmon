package com.gnychis.awmon.Interfaces;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;

import com.gnychis.awmon.R;

public class Measurements extends Activity {

	WifiManager _wifi;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		  super.onCreate(savedInstanceState);
		  setContentView(R.layout.measurements);
	}
	
	public void clickedOrientationRSSI(View v) {
		finish();
	}
}
