package com.gnychis.coexisyst;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

// A class to handle USB worker like things
public class USBMon extends AsyncTask<Context, Integer, String>
{
	Context parent;
	CoexiSyst coexisyst;
	String TAG = "USBMon";
	
	@Override
	protected void onCancelled()
	{
		Log.d(TAG, "USB monitor thread successfully canceled");
	}
	
	@Override
	protected String doInBackground( Context... params )
	{
		parent = params[0];
		coexisyst = (CoexiSyst) params[0];
		Log.d(TAG, "a new USB monitor was started");
		while(true) {
			try {
				
				int wispy_in_devlist=coexisyst.USBcheckForDevice(0x1781, 0x083f);
				int atheros_in_devlist=coexisyst.USBcheckForDevice(0x083a,0x4505);
				
				// Wispy related checks
				if(wispy_in_devlist==1 && coexisyst.wispy._device_connected==false) {
					publishProgress(Wispy.WISPY_CONNECT);
				} else if(wispy_in_devlist==0 && coexisyst.wispy._device_connected==true) {
					publishProgress(Wispy.WISPY_DISCONNECT);
				} else if(wispy_in_devlist==1 && coexisyst.wispy._device_connected==true && coexisyst.wispy._is_polling==false) {
					//Log.d(TAG, "determined that a re-poll is needed");
					//Thread.sleep( 1000 );
					//publishProgress(CoexiSyst.WISPY_POLL);
				}
				
				// Atheros related checks
				if(atheros_in_devlist==1 && coexisyst.ath._device_connected==false) {
					publishProgress(AtherosDev.ATHEROS_CONNECT);
				} else if(atheros_in_devlist==0 && coexisyst.ath._device_connected==true) {
					publishProgress(AtherosDev.ATHEROS_DISCONNECT);
				}
				
				
				Thread.sleep( 2000 );
				Log.d(TAG, "checking for USB devices");

			} catch (Exception e) {
				
				Log.e(TAG, "exception trying to sleep", e);
				return "OUT";
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
	 */
	@Override
	protected void onProgressUpdate(Integer... values)
	{
		super.onProgressUpdate(values);
		int event = values[0];
		
		if(event == Wispy.WISPY_CONNECT) {
			Log.d(TAG, "got update that WiSpy was connected");
			Toast.makeText(parent, "WiSpy device connected",
					Toast.LENGTH_LONG).show();	
			coexisyst.wispy._device_connected=true;
			
			// List the wispy devices
			coexisyst.textStatus.append("\n\nWiSpy Devices:\n");
			String devices[] = coexisyst.getWiSpyList();
			for (int i=0; i<devices.length; i++)
				coexisyst.textStatus.append(devices[i] + "\n");
			
			// Start the poll thread now
			coexisyst.wispyscan.execute(coexisyst);
			coexisyst.wispy._is_polling = true;
		}
		else if(event == Wispy.WISPY_DISCONNECT) {
			Log.d(TAG, "got update that WiSpy was connected");
			Toast.makeText(parent, "WiSpy device has been disconnected",
					Toast.LENGTH_LONG).show();
			coexisyst.wispy._device_connected=false;
			coexisyst.wispyscan.cancel(true);  // make sure to stop polling thread
		}
		else if(event == Wispy.WISPY_POLL) {
			Log.d(TAG, "trying to re-poll the WiSpy device");
			Toast.makeText(parent, "Re-trying polling",
					Toast.LENGTH_LONG).show();
			coexisyst.wispyscan.cancel(true);
			coexisyst.wispyscan = coexisyst.wispy.new WispyThread();
			coexisyst.wispyscan.execute(coexisyst);
			coexisyst.wispy._is_polling = true;
		}
		
		// Handling events of Atheros device
		if(event == AtherosDev.ATHEROS_CONNECT) {
			Log.d(TAG, "got update that Atheros card was connected");
			Toast.makeText(parent, "Atheros device connected", Toast.LENGTH_LONG).show();
			coexisyst.ath.connected();			
		}
		else if(event == AtherosDev.ATHEROS_DISCONNECT) {
			Log.d(TAG, "Atheros card now disconnected");
			Toast.makeText(parent, "Atheros device disconnected", Toast.LENGTH_LONG).show();
			coexisyst.ath.disconnected();
		}
	}
}
