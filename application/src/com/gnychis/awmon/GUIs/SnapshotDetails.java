package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.Snapshot;
import com.gnychis.awmon.Database.DBAdapter;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

public class SnapshotDetails extends Activity {
	
	private static final String TAG = "SnapshotDetails";
	private static final boolean VERBOSE = true;
    private Context _context;
    Date _activityStartTime;
	
	public String _date;
	
    Handler _handler;									// A handler to make sure certain things are un on the main UI thread 
    
	ProgressDialog _pd;									// To show background service progress in scanning
	
	int _row;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.snapshot_details_list);
		
		_context = this;
		_handler = new Handler();
		
		Bundle extras = getIntent().getExtras();
		_date = extras.getString("date");
		
		_pd = ProgressDialog.show(_context, "", "Reading in the snapshot...", true, false);
		
		SnapshotGrab thread = new SnapshotGrab();
		thread.execute(this);
	}
	
	static final class SnapshotGrab extends AsyncTask<Context, Boolean, Boolean> {
		SnapshotDetails mainActivity;
		DBAdapter dbAdapter;
		ArrayList<HashMap<String,String>> _tableRows;
		
		@Override
		protected Boolean doInBackground(Context... params) {
			mainActivity=(SnapshotDetails)params[0];
			dbAdapter = new DBAdapter(mainActivity);
			
			_tableRows = new ArrayList<HashMap<String,String>>();
			
			DBAdapter dbAdapter = new DBAdapter(mainActivity);
			dbAdapter.open();
			
			Snapshot snapshot = dbAdapter.getSnapshot(Snapshot.getDateFromString(mainActivity._date));
			final String anchor = (snapshot.getAnchorMAC()!=null && dbAdapter.getDevice(snapshot.getAnchorMAC())!=null) ? " " + dbAdapter.getDevice(snapshot.getAnchorMAC()).getName() + " (" + snapshot.getAnchorMAC() + ")" : " <None>";
			final String name = (snapshot.getName()!=null) ? " " + snapshot.getName() : " <None>";
			
	        mainActivity._handler.post(new Runnable() {	// Must do this on the main UI thread...
	            @Override
	            public void run() {
					((TextView)mainActivity.findViewById(R.id.anchor)).append( anchor );
					((TextView)mainActivity.findViewById(R.id.name)).append( name );
	            	((TextView)mainActivity.findViewById(R.id.date)).append(" " + mainActivity._date);
	            }
	        });
			
			final TableLayout table = (TableLayout)mainActivity.findViewById(R.id.maintable);

			final LayoutInflater inflater = mainActivity.getLayoutInflater();
			mainActivity._row=0;
			ArrayList<Interface> interfaces = snapshot.getInterfaces();
			Collections.sort(interfaces, WirelessInterface.compareRSSI);
			for(Interface iface : interfaces)
			{
				Device device = dbAdapter.getDevice(iface._MAC);
				
				final String nameString=(device!=null && device.getName()!=null) ? device.getName() : (iface.getName()!=null) ? iface.getName() : "";
				final String internalString=(device!=null && device.getInternal()) ? "Yes" : "No";
				final String protocolString=(iface._type!=null) ? Interface.simplifiedClassName(iface._type) : "";
				final String macString=iface._MAC;
				final String signalString=(iface.getClass()==WirelessInterface.class && ((WirelessInterface)iface).averageRSSI()!=-500) ? ((WirelessInterface)iface).averageRSSI() + "dBm" : "-";

		        mainActivity._handler.post(new Runnable() {	// Must do this on the main UI thread...
		            @Override
		            public void run() {
		            	
						final TableRow row = (TableRow)inflater.inflate(R.layout.snapshot_table_row, table, false);
						
						if(mainActivity._row%2==0)
							row.setBackgroundColor(0xff222222);
						
						((TextView)row.findViewById(R.id.name)).setText(nameString);
						((TextView)row.findViewById(R.id.internal)).setText(internalString);
						((TextView)row.findViewById(R.id.protocol)).setText(protocolString);
						((TextView)row.findViewById(R.id.mac)).setText(macString);
						((TextView)row.findViewById(R.id.signal)).setText(signalString);
		            			            	
						table.addView(row);
						mainActivity._row++;
		            }
		          });				
			}
			dbAdapter.close();
			
			return true;
		}
		
	    @Override
	    protected void onPostExecute(Boolean success) {  
	    	//mainActivity.updateListWithSnapshots(snapshots);
	    	if(mainActivity._pd != null)
	    		mainActivity._pd.cancel();
	    }
	}
	
	@Override
	public void onBackPressed() {
		Intent i = new Intent(SnapshotDetails.this, SnapshotList.class);
		startActivity(i);
		finish();
	}

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
