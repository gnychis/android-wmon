package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.gnychis.awmon.BackgroundService.ScanManager;
import com.gnychis.awmon.Core.ScanRequest;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.InterfaceMerging.InterfaceMergingManager;
import com.gnychis.awmon.InterfaceScanners.InterfaceScanManager;
import com.gnychis.awmon.NameResolution.NameResolutionManager;

public class YourDevices extends Activity {

	public static final String TAG = "YourDevices";
	public static final boolean VERBOSE = true;

	//ArrayList that will hold the original Data
	ArrayList<HashMap<String, Object>> _deviceList;
	LayoutInflater inflater;
	ProgressDialog _pd;
	CustomAdapter _adapter;
    Handler _handler;
    
    ScanManager.ResultType _resultType;
    Object _scanResult;
    
    private Context _context;

	private ListView m_listview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.your_devices);
		_context = this;

		_deviceList=new ArrayList<HashMap<String,Object>>();
		_handler = new Handler();

		// Send out a request for a device scan.
		ScanRequest request = new ScanRequest();
		request.setNameResolution(true);
		request.setMerging(true);
		request.send(this);

		// Pop up a progress dialog
		_pd = ProgressDialog.show(this, "", "Scanning for devices", true, false);
		registerReceiver(_deviceScanReceiver, new IntentFilter(ScanManager.SCAN_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(InterfaceScanManager.INTERFACE_SCAN_RESULT));
		registerReceiver(incomingEvent, new IntentFilter(NameResolutionManager.NAME_RESOLUTION_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE));
	}
	
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	if(intent.getAction().equals(InterfaceScanManager.INTERFACE_SCAN_RESULT)) {
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Resolving device names", true, false);
        	}
        	if(intent.getAction().equals(NameResolutionManager.NAME_RESOLUTION_RESPONSE)) {
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Merging interfaces to devices", true, false);
        	}
        }
    };

	// A broadcast receiver to get messages from background service and threads
	private BroadcastReceiver _deviceScanReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		public void onReceive(Context context, Intent intent) {

			// First, get the response and see if the results are interfaces or devices.
			_resultType = (ScanManager.ResultType) intent.getExtras().get("type");
			_scanResult = intent.getExtras().get("result");

			if(_resultType == ScanManager.ResultType.INTERFACES) {
				ArrayList<Interface> deviceScanResult = (ArrayList<Interface>) intent.getExtras().get("result");
				_deviceList=new ArrayList<HashMap<String,Object>>();	// Reset the list

				// For each interface, add it to our list.
				HashMap<String , Object> temp;
				for(Interface iface : deviceScanResult) {
					temp=new HashMap<String, Object>();
					temp.put("name", Interface.simplifiedClassName(iface._type));
					temp.put("additional", iface._ouiName);    
					temp.put("key", iface.getKey());
					_deviceList.add(temp);	
					debugOut(iface.toString());
				}

			}

			if(_resultType == ScanManager.ResultType.DEVICES) {
				ArrayList<Device> deviceScanResult = (ArrayList<Device>) intent.getExtras().get("result");

				HashMap<String , Object> temp;
				for(Device device : deviceScanResult) {
					temp=new HashMap<String, Object>();
					temp.put("name", device.getName());
					temp.put("additional", device.getManufacturer()); 
					_deviceList.add(temp);  
					debugOut(device.toString());
				}
			}
			
			updateDeviceList();
			
			if(_pd!=null)
				_pd.dismiss();
		}
	}; 
	
	private void updateDeviceList() {
        _handler.post(new Runnable() {	// Must do this on the main UI thread...
            @Override
            public void run() {
        		_adapter=new CustomAdapter(_context, R.layout.device_list_item ,_deviceList); 
        		m_listview = (ListView) findViewById(android.R.id.list);
        		m_listview.setAdapter(_adapter);
        		inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        		_adapter.notifyDataSetChanged();
            }
          });
	}


	//define your custom adapter
	private class CustomAdapter extends ArrayAdapter<HashMap<String, Object>>
	{
		boolean[] checkBoxState;
		ViewHolder viewHolder;

		public CustomAdapter(Context context, int textViewResourceId,
				ArrayList<HashMap<String, Object>> devices) {
			super(context, textViewResourceId, devices); 
			checkBoxState=new boolean[devices.size()];
		}

		//class for caching the views in a row  
		private class ViewHolder
		{
			TextView name,additional;
			CheckBox checkBox;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			if(convertView==null)
			{
				convertView=inflater.inflate(R.layout.device_list_item, null);
				viewHolder=new ViewHolder();

				//cache the views
				viewHolder.name=(TextView) convertView.findViewById(R.id.name);
				viewHolder.additional=(TextView) convertView.findViewById(R.id.additional);
				viewHolder.checkBox=(CheckBox) convertView.findViewById(R.id.checkBox);

				//link the cached views to the convertview
				convertView.setTag( viewHolder);
			}
			else
				viewHolder=(ViewHolder) convertView.getTag();

			debugOut("Trying to get position(" + position + ") in _deviceList of size " + _deviceList.size());
			debugOut("... deviceList: " + _deviceList);
			debugOut("... deviceList(" + position + "): " + _deviceList.get(position));
			debugOut("... deviceList(" + position + ").get(\"name\"): " + _deviceList.get(position).get("name"));
			viewHolder.name.setText(_deviceList.get(position).get("name").toString());
			viewHolder.additional.setText("Additional: " + _deviceList.get(position).get("additional").toString());
			viewHolder.checkBox.setChecked(checkBoxState[position]);

			viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
					if(((CheckBox)v).isChecked())
						checkBoxState[position]=true;
					else
						checkBoxState[position]=false;

				}
			});

			//return the view to be displayed
			return convertView;
		}

	}

	@Override
	public void onBackPressed() {
		Intent i = new Intent(YourDevices.this, HomeLocation.class);
		startActivity(i);
		finish();
	}

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}

/*
		// Some temporary arrays for the data
		String names[]={"The Smith's Wifi","Creative D200 Speaker","Power Monitor Sensor","Bill's iPad",
				"Xbox 360","Gaming Controller","Don't Steal Our Wifi", "Something else"}; 

		String additional[]={"802.11n (40MHz)","Bluetooth","ZigBee",
				"802.11n","802.11n","Proprietary",
		"802.11g", "whatever"};

		//temporary HashMap for populating the 
		//Items in the ListView
		HashMap<String , Object> temp;

		//total number of rows in the ListView
		int noOfPlayers=names.length;

		//now populate the ArrayList devices
		for(int i=0;i<noOfPlayers;i++)
		{
			temp=new HashMap<String, Object>();

			temp.put("name", names[i]);
			temp.put("additional", additional[i]);    

			//add the row to the ArrayList
			_deviceList.add(temp);        
		}
 */
