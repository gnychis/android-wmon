package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

public class YourDevices extends Activity {

	public static final String TAG = "YourDevices";
	public static final boolean VERBOSE = true;

	//ArrayList that will hold the original Data
	ArrayList<HashMap<String, Object>> _deviceList;
	LayoutInflater inflater;
	ProgressDialog _pd;
	CustomAdapter _adapter;

	private ListView m_listview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.your_devices);

		m_listview = (ListView) findViewById(android.R.id.list);
		inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		_deviceList=new ArrayList<HashMap<String,Object>>();
		_adapter=new CustomAdapter(this, R.layout.device_list_item ,_deviceList); 
		m_listview.setAdapter(_adapter);

		// Send out a request for a device scan.
		ScanRequest request = new ScanRequest();
		request.setNameResolution(true);
		request.setMerging(true);
		request.send(this);

		// Pop up a progress dialog
		_pd = ProgressDialog.show(this, "", "Retrieving a list of devices in range", true, false);
		registerReceiver(_deviceScanReceiver, new IntentFilter(ScanManager.SCAN_RESPONSE));
	}

	// A broadcast receiver to get messages from background service and threads
	private BroadcastReceiver _deviceScanReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		public void onReceive(Context context, Intent intent) {

			// First, get the response and see if the results are interfaces or devices.
			ScanManager.ResultType resultType = (ScanManager.ResultType) intent.getExtras().get("type");

			if(resultType == ScanManager.ResultType.INTERFACES) {
				ArrayList<Interface> deviceScanResult = (ArrayList<Interface>) intent.getExtras().get("result");
				_deviceList=new ArrayList<HashMap<String,Object>>();	// Reset the list

				// For each interface, add it to our list.
				HashMap<String , Object> temp;
				for(Interface iface : deviceScanResult) {
					temp=new HashMap<String, Object>();
					temp.put("name", simplifiedClassName(iface._type));
					temp.put("additional", iface._ouiName);    
					_deviceList.add(temp);  
					
					debugOut("Got an interface (" + simplifiedClassName(iface.getClass()) + " - " + simplifiedClassName(iface._type) + "): " 
							+ iface._MAC 
							+ " - " + iface._IP
							+ " - " + iface._ifaceName
							+ " - " + iface._ouiName
							);
				}

			}

			if(resultType == ScanManager.ResultType.DEVICES) {
				ArrayList<Device> deviceScanResult = (ArrayList<Device>) intent.getExtras().get("result");

				HashMap<String , Object> temp;
				for(Device device : deviceScanResult) {
					temp=new HashMap<String, Object>();
					temp.put("name", device.getName());
					temp.put("additional", device.getManufacturer()); 
					_deviceList.add(temp);  
					
					debugOut("Got a device: " + device.getName());
					List<Interface> interfaces = device.getInterfaces();
					for(Interface iface : interfaces) {
						debugOut("... interface (" + simplifiedClassName(iface.getClass()) + " - " + simplifiedClassName(iface._type) + "): " 
								+ iface._MAC 
								+ " - " + iface._IP
								+ " - " + iface._ifaceName
								+ " - " + iface._ouiName
								);	
					}
				}
			}
			
			// Now, update the list
			_adapter.notifyDataSetChanged();
			
			if(_pd!=null)
				_pd.dismiss();
		}
	}; 


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


	public static String simplifiedClassName(Class<?> c) {
		String fullName = c.getName();
		String[] topName = fullName.split("\\.");
		if(topName.length==0)
			return fullName;
		return topName[topName.length-1];
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
