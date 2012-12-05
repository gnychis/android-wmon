package com.gnychis.awmon.GUIs;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.Snapshot;
import com.gnychis.awmon.Database.DBAdapter;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

public class SnapshotList extends Activity {

	public static final String TAG = "SnapshotList";
	public static final boolean VERBOSE = true;
    private Context _context;
    
	ArrayList<HashMap<String, Object>> _snapshotList;	// This is bound to the actual list		
	CustomAdapter _adapter;								// An adapter to our device list
    
    Handler _handler;									// A handler to make sure certain things are un on the main UI thread 
    
	ProgressDialog _pd;									// To show background service progress in scanning
	
	ArrayList<Snapshot> _snapshots;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.snapshot_list);
		_context = this;
		_handler = new Handler();
		
		_snapshotList=new ArrayList<HashMap<String,Object>>();
		
		_pd = ProgressDialog.show(_context, "", "Retrieving the list of snapshots", true, false);
		
		SnapshotGrab thread = new SnapshotGrab();
		thread.execute(this);
	}
    
	static final class SnapshotGrab extends AsyncTask<Context, ArrayList<Snapshot>, ArrayList<Snapshot>> {
		SnapshotList mainActivity;
		DBAdapter dbAdapter;
		
		@Override
		protected ArrayList<Snapshot> doInBackground(Context... params) {
			mainActivity=(SnapshotList)params[0];
			dbAdapter = new DBAdapter(mainActivity);
			debugOut("Opening the database...");
			dbAdapter.open();
			debugOut("Getting the snapshots");
			ArrayList<Snapshot> snapshots = dbAdapter.getSnapshotsMetadata();	
			debugOut("Closing the database...");
			dbAdapter.close();
			debugOut("...closed");
			return snapshots;
		}
		
	    @Override
	    protected void onPostExecute(ArrayList<Snapshot> snapshots) {  
	    	mainActivity.updateListWithSnapshots(snapshots);
	    	if(mainActivity._pd != null)
	    		mainActivity._pd.cancel();
	    }
	}
	
	/** This updates the list with a set of devices
	 * @param devices The devices to populate the list with
	 */
	private void updateListWithSnapshots(ArrayList<Snapshot> snapshots) {
		DBAdapter dbAdapter = new DBAdapter(this);
		_snapshots = new ArrayList<Snapshot>();
		dbAdapter.open();
		_snapshotList=new ArrayList<HashMap<String,Object>>();
		for(Snapshot snapshot : snapshots) {
			String snapshotTime = snapshot.getSnapshotTimeString();
			String snapshotName = snapshot.getName();
			String anchorName=null;
			Device anchorDevice = dbAdapter.getDevice(snapshot.getAnchorMAC());
			if(anchorDevice!=null)
				anchorName = anchorDevice.getName();
			_snapshotList.add(createListItem(snapshotTime, snapshotName, anchorName, snapshot));  
			_snapshots.add(snapshot);
			debugOut("Snapshot: " + snapshotTime + "  Anchor: " + anchorName);
		}
		dbAdapter.close();
		updateSnapshotList();
	}
	
	private HashMap<String,Object> createListItem(String date, String name, String anchor, Object o) {
		HashMap<String , Object> listItem = new HashMap<String, Object>();	
		listItem.put("name", name);
		listItem.put("date", date);
		listItem.put("anchor", anchor);
		return listItem;
	}
	
	/** Gets a handle to the custom list, sets the data, and then notifies it that data is ready.*/
	private void updateSnapshotList() {
        _handler.post(new Runnable() {	// Must do this on the main UI thread...
            @Override
            public void run() {
        		_adapter=new CustomAdapter(_context, R.layout.snapshot_list_item ,_snapshotList); 
        		ListView m_listview = (ListView) findViewById(android.R.id.list);
        		m_listview.setAdapter(_adapter);
        		_adapter.notifyDataSetChanged();
            }
          });
	}
	
	/** 
	 * @author Most of the CustomAdapter code was adopted from a posting I found on Google.  Can't remember link though.
	 */
	private class CustomAdapter extends ArrayAdapter<HashMap<String, Object>>
	{
		boolean[] checkBoxState;	// Keep track of each checkbox state for each item
		ViewHolder viewHolder;

		// On initialization, make sure to instantiate the checkbox resource
		public CustomAdapter(Context context, int textViewResourceId,
				ArrayList<HashMap<String, Object>> devices) {
			super(context, textViewResourceId, devices); 
			checkBoxState=new boolean[devices.size()];
		}

		//class for caching the views in a row  
		private class ViewHolder
		{
			TextView date,name,anchor;
			CheckBox checkBox;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			if(convertView==null)
			{
				LayoutInflater inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView=inflater.inflate(R.layout.snapshot_list_item, null);
				viewHolder=new ViewHolder();

				//cache the views
				viewHolder.date=(TextView) convertView.findViewById(R.id.date);
				viewHolder.name=(TextView) convertView.findViewById(R.id.name);
				viewHolder.anchor=(TextView) convertView.findViewById(R.id.anchor);
				viewHolder.checkBox=(CheckBox) convertView.findViewById(R.id.checkBox);

				//link the cached views to the convertview
				convertView.setTag( viewHolder);
			}
			else
				viewHolder=(ViewHolder) convertView.getTag();
			
			String date = (_snapshotList.get(position).get("date")==null) ? "" : _snapshotList.get(position).get("date").toString();
			String name = (_snapshotList.get(position).get("name")==null) ? "Name: <None>" : "Name: " + _snapshotList.get(position).get("name").toString();
			String anchor = (_snapshotList.get(position).	get("anchor")==null) ? "Anchor: <None>" : "Anchor: " + _snapshotList.get(position).get("anchor").toString();
			
			viewHolder.date.setText(date);
			viewHolder.name.setText(name);
			viewHolder.anchor.setText(anchor);
			viewHolder.checkBox.setChecked(checkBoxState[position]);
			
			viewHolder.date.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String date = ((TextView)v).getText().toString();
					debugOut("Clicked on snapshot: " + date);
					
					Intent i = new Intent(SnapshotList.this, SnapshotDetails.class);
					i.putExtra("date", date);
					startActivity(i);
					finish();
					
				}
			});
			
			viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if(((CheckBox)v).isChecked())
						checkBoxState[position]=true;
					else
						checkBoxState[position]=false;
				}
			});
			return convertView;
		}

	}
	
	public void clickedExport(View v) {
		debugOut("Got a click on export snapshot");
		
		_pd = ProgressDialog.show(_context, "", "Exporting, please wait...", true, false);
		
		ArrayList<Snapshot> exportSnapshots = new ArrayList<Snapshot>();
		for(int i=0; i<_adapter.checkBoxState.length; i++)
			if(_adapter.checkBoxState[i])
				exportSnapshots.add(_snapshots.get(i));
		
		class ExportThread implements Runnable { 
			ArrayList<Snapshot> _exportSnapshots;
			private FileOutputStream data_ostream;
			Context _context;
			
			public ExportThread(Context c, ArrayList<Snapshot> snapshots) {
				_context = c;
				_exportSnapshots=snapshots;
				SimpleDateFormat format = new SimpleDateFormat("EEE-MMM-dd-HH:mm:ss-zzz-yyyy");
				try {
				data_ostream = openFileOutput("snapshot_export_" + format.format(new Date()) + ".json", Context.MODE_WORLD_READABLE);
				} catch(Exception e) { debugOut("Unable to open output file on sdcard" + e); }
			}

			@Override
			public void run() {
				DBAdapter dbAdapter = new DBAdapter(_context);
				dbAdapter.open();
				JSONArray snapshotArray = new JSONArray();
				for(Snapshot snapshot : _exportSnapshots) {

					JSONObject json = new JSONObject();
					
					try {
						json.put("name", (snapshot.getName()!=null) ? snapshot.getName() : JSONObject.NULL);
						json.put("date", snapshot.getSnapshotTimeString());
						
						JSONArray jsonInterfaces = new JSONArray();
						
						for(Interface iface : dbAdapter.getSnapshotInterfaces(snapshot.getSnapshotKey())) {
							JSONObject ifaceObj = new JSONObject();
							
							Device device = dbAdapter.getDevice(iface._MAC);
							
							ifaceObj.put("name", (device!=null && device.getName()!=null) ? device.getName() : (iface.getName()!=null) ? iface.getName() : JSONObject.NULL);
							ifaceObj.put("internal", (device!=null && device.getInternal()) ? true : false);
							ifaceObj.put("protocol", Interface.simplifiedClassName(iface._type));
							ifaceObj.put("mac", iface._MAC);
							
							JSONArray RSSIVals = new JSONArray();
							if(iface.getClass()==WirelessInterface.class)
								for(int rssiVal : ((WirelessInterface)iface).rssiValues())
									RSSIVals.put(rssiVal);

							ifaceObj.put("RSSI", RSSIVals);
							jsonInterfaces.put(ifaceObj);
						}
						
						json.put("interfaces", jsonInterfaces);
						json.put("anchor", (snapshot.getAnchorMAC()!=null) ? snapshot.getAnchorMAC() : JSONObject.NULL);
						json.put("snapshotKey", snapshot.getSnapshotKey());

					} catch(Exception e) { Log.e("Snapshot", "Exception trying to load json object: ", e); }
					snapshotArray.put(json);
				}
				
		        try {
					data_ostream.write(snapshotArray.toString().getBytes());
					data_ostream.write("\n".getBytes()); 
		        	data_ostream.close();
		        } catch(Exception e) { Log.e("Snapshot", "Exception trying to write json object ", e); } 
		        
		        dbAdapter.close();
		        
		        _handler.post(new Runnable() {	// Must do this on the main UI thread...
		            @Override
		            public void run() {
		        		if(_pd!=null)
		        			_pd.cancel();
		            }
		          });
			}
		}

		Runnable thread = new ExportThread(this, exportSnapshots);
		new Thread(thread).start();
	}
	
	
	/*public JSONObject makeJSON() {

		
		return json;
	}*/
	
	@Override
	public void onBackPressed() {
		Intent i = new Intent(SnapshotList.this, Status.class);
		startActivity(i);
		finish();
	}

	private static void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
