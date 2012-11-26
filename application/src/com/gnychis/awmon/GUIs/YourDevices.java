package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.Collections;
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
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.DeviceFiltering.DeviceFilteringManager;
import com.gnychis.awmon.InterfaceMerging.InterfaceMergingManager;
import com.gnychis.awmon.InterfaceScanners.InterfaceScanManager;
import com.gnychis.awmon.NameResolution.NameResolutionManager;

/**
 * The purpose of this GUI interface is to do a bulk scan for devices in range and then
 * to ask the user which of these devices belong to them via a checklist.  We can then
 * mark these devices as belonging to the user.
 * 
 * @author George Nychis (gnychis)
 */
public class YourDevices extends Activity {

	public static final String TAG = "YourDevices";
	public static final boolean VERBOSE = true;
    private Context _context;

	ArrayList<HashMap<String, Object>> _deviceList;		// This is bound to the actual list						
	ProgressDialog _pd;									// To show background service progress in scanning
	CustomAdapter _adapter;								// An adapter to our device list
    Handler _handler;									// A handler to make sure certain things are un on the main UI thread 
    
    Object _scanResult;									// The result from scanning with the background service
    ScanManager.ResultType _resultType;					// The result type (INTERFACES or DEVICES)

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.your_devices);
		_context = this;

		_deviceList=new ArrayList<HashMap<String,Object>>();
		_handler = new Handler();

		ScanRequest request = new ScanRequest();	// Instantiate a scan request
		request.setNameResolution(true);			// Enable name resolution
		request.setMerging(true);					// Merge interfaces in to devices
		request.setFiltering(true);					// Try to filter out devices that definitely do not belong to the user
		request.send(this);							// Send the request to the background service

		// Pop up a progress dialog and register receivers for progress being made by the scanning service
		_pd = ProgressDialog.show(this, "", "Scanning for devices", true, false);
	}
	
	public void registerReceivers() {
		registerReceiver(_deviceScanReceiver, new IntentFilter(ScanManager.SCAN_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(InterfaceScanManager.INTERFACE_SCAN_RESULT));
		registerReceiver(incomingEvent, new IntentFilter(NameResolutionManager.NAME_RESOLUTION_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(DeviceFilteringManager.DEVICE_FILTERING_RESPONSE));
	}
	
	@Override
	public void onResume() {
		super.onPause();
		registerReceivers();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(_deviceScanReceiver);
		unregisterReceiver(incomingEvent);
	}	
	
    /**
     *   The purpose of this function is to listen for progress being made by the background service in scanning
     *   and resolving devices within range.  This is simply to update the progress dialogue to show progress.
     */
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
    	@SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
        	if(intent.getAction().equals(InterfaceScanManager.INTERFACE_SCAN_RESULT)) {
        		updateListWithInterfaces((ArrayList<Interface>) intent.getExtras().get("result"), false);
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Resolving device names", true, false);
        	}
        	if(intent.getAction().equals(NameResolutionManager.NAME_RESOLUTION_RESPONSE)) {
        		updateListWithInterfaces((ArrayList<Interface>) intent.getExtras().get("result"), true);
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Merging interfaces to devices", true, false);
        	}
        	if(intent.getAction().equals(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE)) {
        		updateListWithDevices((ArrayList<Device>) intent.getExtras().get("result"));
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Filtering devices we strongly believe are not yours (to make this easier for you!)", true, false);
        	}
        }
    };

	/**
	 * This will trigger when the background scan for devices or interfaces has been completed, it will
	 * then trigger the list to be updated and displayed.
	 */
	private BroadcastReceiver _deviceScanReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		public void onReceive(Context context, Intent intent) {

			_resultType = (ScanManager.ResultType) intent.getExtras().get("type");
			_scanResult = intent.getExtras().get("result");

			//************************ INTERFACE RESULTS *************************//
			if(_resultType == ScanManager.ResultType.INTERFACES)
				updateListWithInterfaces((ArrayList<Interface>) _scanResult, false);

			//************************** DEVICE RESULTS *************************//
			if(_resultType == ScanManager.ResultType.DEVICES)
				updateListWithDevices((ArrayList<Device>) _scanResult);
			
			if(_pd!=null)			// If there is a progress dialog up, cancel it
				_pd.dismiss();
		}
	}; 
	
	/** 
	 * This updates the list with a set of interfaces.
	 * @param interfaces the interfaces to add to the list
	 */
	private void updateListWithInterfaces(ArrayList<Interface> interfaces, boolean useNames) {
		HashMap<String , Object> tempListItem;
		_deviceList=new ArrayList<HashMap<String,Object>>();
		Collections.shuffle(interfaces);
		for(Interface iface : interfaces) {
			tempListItem=new HashMap<String, Object>();
			
			// If we are using names, we use all devices in the list.
			if(useNames) {
				if(iface._ifaceName!=null)
					tempListItem.put("name", iface._ifaceName);
				if(iface._ouiName!=null)
					tempListItem.put("additional", iface._ouiName); 
			} else {
				if(iface.getClass() != WirelessInterface.class)	// For the sake of demonstration
					continue;
				tempListItem.put("name", Interface.simplifiedClassName(iface._type) + " Radio");
				tempListItem.put("additional", "Signal strength: " + ((WirelessInterface)iface).averageRSSI() + "dBm");
			}
			tempListItem.put("key", iface.getKey());
			_deviceList.add(tempListItem);	
			debugOut(iface.toString());
		}
		updateDeviceList();		// Update and populate the actual device list
	}
	
	private void updateListWithDevices(ArrayList<Device> devices) {
		HashMap<String , Object> tempListItem;
		_deviceList=new ArrayList<HashMap<String,Object>>();
		for(Device device : devices) {
			tempListItem=new HashMap<String, Object>();
			tempListItem.put("name", device.getName());
			tempListItem.put("additional", device.getManufacturer()); 
			_deviceList.add(tempListItem);  
			debugOut(device.toString());
		}
		updateDeviceList();
	}
	
	/** Gets a handle to the custom list, sets the data, and then notifies it that data is ready.*/
	private void updateDeviceList() {
        _handler.post(new Runnable() {	// Must do this on the main UI thread...
            @Override
            public void run() {
        		_adapter=new CustomAdapter(_context, R.layout.device_list_item ,_deviceList); 
        		ListView m_listview = (ListView) findViewById(android.R.id.list);
        		m_listview.setAdapter(_adapter);
        		_adapter.notifyDataSetChanged();
            }
          });
	}

	/**
	 * This is a custom list that incorporates a checkbox with each item.
	 * 
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
			TextView name,additional;
			CheckBox checkBox;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			if(convertView==null)
			{
				LayoutInflater inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			
			String name = (_deviceList.get(position).get("name")==null) ? "" : _deviceList.get(position).get("name").toString();
			String additional = (_deviceList.get(position).get("additional")==null) ? "" : _deviceList.get(position).get("additional").toString();
			
			// For long additional description, truncate it
			if(_deviceList.get(position).get("additional").toString().length()>37)
				additional = additional.substring(0, 37) + "...";
			
			// Set the actual items in the list with name and any additional info
			viewHolder.name.setText(name);
			viewHolder.additional.setText(additional);
			viewHolder.checkBox.setChecked(checkBoxState[position]);

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
