package com.gnychis.awmon.GUIs;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.gnychis.awmon.R;
import com.gnychis.awmon.BackgroundService.MotionDetector;
import com.gnychis.awmon.Core.DialogActivity;
import com.gnychis.awmon.Core.ScanRequest;
import com.gnychis.awmon.Core.Snapshot;

public class Measurements extends Activity {

	private static final String TAG = "Measurements";
    Date _activityStartTime;

	WifiManager _wifi;

	boolean _measuring;
	boolean _active_orientationRSSI;
	private FileOutputStream _data_ostream;
	private ProgressDialog _pd;
	Handler _handler;									// A handler to make sure certain things are un on the main UI thread 

	Context _context;
	String _name;
	
	int _scansLeft;
	ScanRequest _workingRequest;

	State _state;
	public enum State {
		IDLE,
		ORIENTATION,
		SNAPSHOT,
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_context = this;
		_state = State.IDLE;
		setContentView(R.layout.measurements);
		_active_orientationRSSI=false;
		_measuring=false;
		_wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		_handler = new Handler();
		_name="";

		// Initialize the button colors to "inactive"
		((Button) findViewById(R.id.Measurements_btn_OrientationRSSI)).setBackgroundColor(Color.GRAY);
	}

	public void clickedSnapshot(View v) {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Snapshot Name");
		alert.setMessage("Choose a name for the snapshot");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		input.setText(_name);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				triggerSnapshot(value);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		alert.show();
	}

	public void clickedSnapshotDelayed(View v) {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Snapshot Name");
		alert.setMessage("Choose a name for the snapshot");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		input.setText(_name);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				triggerSnapshotDelayed(value);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		alert.show();
	}
	
	public void clickedMultiSnapshot(View v) {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Snapshot Name");
		alert.setMessage("Choose a name for the snapshot");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		input.setText("5 " + _name);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				int numScans = Integer.parseInt(input.getText().toString().split(" ")[0]);
				String name = input.getText().toString().substring(input.getText().toString().indexOf(" "));
				triggerMultiSnapshot(name, numScans);
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});

		alert.show();
	}

	public void triggerSnapshot(String name) {
		_state = State.SNAPSHOT;
		_name=name;
		_pd = ProgressDialog.show(Measurements.this, "", "Taking a snapshot, please wait...", true, false); 
		_workingRequest = new ScanRequest();
		_workingRequest.makeSnapshot();
		if(name.equals(""))
			name=null;
		_workingRequest.setSnapshotName(name);
		_workingRequest.send(this);
		_scansLeft=0;
	}
	
	public void triggerMultiSnapshot(String name, int numScans) {
		_state = State.SNAPSHOT;
		_name=name;
		_pd = ProgressDialog.show(Measurements.this, "", "Taking a snapshot, please wait...", true, false); 
		_workingRequest = new ScanRequest();
		_workingRequest.makeSnapshot();
		if(name.equals(""))
			name=null;
		_workingRequest.setSnapshotName(name);
		_workingRequest.send(this);
		_scansLeft=numScans-1;
	}

	public void triggerSnapshotDelayed(String name) {
		_state = State.SNAPSHOT;
		_pd = ProgressDialog.show(Measurements.this, "", "Delayed 5 seconds, please wait...", true, false); 
		_name =name;
		Timer scan_timer = new Timer();
		scan_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				_handler.post(new Runnable() {	// Must do this on the main UI thread...
					@Override
					public void run() {		
						Vibrator vibrator;
						vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						vibrator.vibrate(500);
						if(_pd!=null)
							_pd.cancel();
						_pd = ProgressDialog.show(Measurements.this, "", "Taking a snapshot, please wait...", true, false); 

						_workingRequest = new ScanRequest();
						_workingRequest.makeSnapshot();
						if(_name.equals(""))
							_name=null;
						_workingRequest.setSnapshotName(_name);
						_workingRequest.send(_context);
						_scansLeft=0;
					}
				});

			}
		}, 5000);		// Wait one scan time on the home AP's channel

	}

	@Override
	public void onResume() { 
		super.onResume(); 
		_activityStartTime = new Date();
		(new DialogActivity(TAG, true)).saveInDatabse(this);
		registerReceiver(incomingEvent, new IntentFilter(Snapshot.SNAPSHOT_DATA));

	}
	public void onPause() { 
		super.onPause(); 
		(new DialogActivity(TAG, false, _activityStartTime, new Date())).saveInDatabse(this);
		Log.d(TAG, "onPause()"); 
		unregisterReceiver(incomingEvent);
	}

	private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {

			// If we sent a snapshot request and are getting one in
			if(_state.equals(State.SNAPSHOT) && intent.getAction().equals(Snapshot.SNAPSHOT_DATA)) {
				Snapshot snapshot = (Snapshot) intent.getExtras().get("snapshot");

				Vibrator vibrator;
				vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(500);
				
				if(_scansLeft>0) {
					_workingRequest.send(_context);
					_scansLeft--;
				} else {
					if(_pd!=null)
						_pd.dismiss();
					_state = State.IDLE;
				}
			}
		}
	};

	public void clickedOrientationRSSI(View v) {
		if(!_active_orientationRSSI) {
			_state=State.ORIENTATION;
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
			_state=State.IDLE;
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
	public void onBackPressed() {
		Intent i = new Intent(Measurements.this, Status.class);
		startActivity(i);
		finish();
	}
}
