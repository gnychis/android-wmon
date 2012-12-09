package com.gnychis.awmon.GUIs;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.BackgroundService.ScanManager;
import com.gnychis.awmon.Core.DialogActivity;
import com.gnychis.awmon.Core.ScanRequest;
import com.gnychis.awmon.Database.DBAdapter;
import com.gnychis.awmon.Database.DBAdapter.NameUpdate;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.DeviceFiltering.DeviceFilteringManager;
import com.gnychis.awmon.InterfaceMerging.InterfaceMergingManager;
import com.gnychis.awmon.InterfaceScanners.InterfaceScanManager;
import com.gnychis.awmon.NameResolution.NameResolutionManager;
import com.gnychis.awmon.NameResolution.NameResolver;
import com.gnychis.awmon.NameResolution.SSDP;

/**
 * The purpose of this GUI interface is to do a bulk scan for devices in range and then
 * to ask the user which of these devices belong to them via a checklist.  We can then
 * mark these devices as belonging to the user.
 * 
 * @author George Nychis (gnychis)
 */
public class YourDevices extends Activity {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 

	public static final String TAG = "YourDevices";
	public static final boolean VERBOSE = true;
    private Context _context;
    Date _activityStartTime;

	ArrayList<HashMap<String, Object>> _deviceList;		// This is bound to the actual list			
	ArrayList<Device> _devices;							// To keep track of our current device list
	ProgressDialog _pd;									// To show background service progress in scanning
	CustomAdapter _adapter;								// An adapter to our device list
    Handler _handler;									// A handler to make sure certain things are un on the main UI thread 
    
    Object _scanResult;									// The result from scanning with the background service
    ScanManager.ResultType _resultType;					// The result type (INTERFACES or DEVICES)
    
    ArrayList<Device> _internalDevices;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.your_devices);
		_context = this;

		_handler = new Handler();
				
		DBAdapter dbAdapter = new DBAdapter(this);
		dbAdapter.open();
		_internalDevices = dbAdapter.getInternalDevices();
		dbAdapter.close();
		
		if(_internalDevices.size()==0) {
			startScan();
		} else {
			_resultType = ScanManager.ResultType.DEVICES;
			updateListWithDevices(_internalDevices);
		}
	}
	
	public void startScan() {
		_handler.post(new Runnable() {	// Must do this on the main UI thread...
			@Override
			public void run() {						
				// Pop up a progress dialog and register receivers for progress being made by the scanning service
				_pd = ProgressDialog.show(_context, "", "Scanning for devices", true, false);
			}
		});
		
		// Wait a small period of time before triggering the scans.  This allows the GUI to kind of bring itself up
		// and show all of the items before we lock up some resources.
		Timer scan_timer = new Timer();
		scan_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				_handler.post(new Runnable() {	// Must do this on the main UI thread...
					@Override
					public void run() {						
						ScanRequest request = new ScanRequest();	// Instantiate a scan request
						request.setNameResolution(true);			// Enable name resolution
						request.setMerging(true);					// Merge interfaces in to devices
						request.setFiltering(true);					// Try to filter out devices that definitely do not belong to the user
						request.send(_context);						// Send the request to the background service
					}
				});
			}
		}, 1000);
	}
	
	public void registerReceivers() {
		registerReceiver(_deviceScanReceiver, new IntentFilter(ScanManager.SCAN_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(InterfaceScanManager.INTERFACE_SCAN_RESULT));
		registerReceiver(incomingEvent, new IntentFilter(NameResolutionManager.NAME_RESOLUTION_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(NameResolver.NAME_RESOLVER_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE));
		registerReceiver(incomingEvent, new IntentFilter(DeviceFilteringManager.DEVICE_FILTERING_RESPONSE));
	}
	
	@Override
	public void onResume() {
		super.onResume();	
		_activityStartTime = new Date();
		(new DialogActivity(TAG, true)).saveInDatabse(this);
		registerReceivers();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		(new DialogActivity(TAG, false, _activityStartTime, new Date())).saveInDatabse(this);
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
        		updateListWithInterfaces((ArrayList<Interface>) intent.getExtras().get("result"), 0);
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Resolving device names", true, false);
        	}
        	if(intent.getAction().equals(NameResolutionManager.NAME_RESOLUTION_RESPONSE)) {
        		updateListWithInterfaces((ArrayList<Interface>) intent.getExtras().get("result"), 2);
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Merging interfaces to devices", true, false);
        	}
        	if(intent.getAction().equals(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE)) {
        		updateListWithDevices((ArrayList<Device>) intent.getExtras().get("result"));
        		_pd.dismiss();
        		_pd = ProgressDialog.show(_context, "", "Filtering devices we strongly believe are not yours (to make this easier for you!)", true, false);
        	}
        	if(intent.getAction().equals(NameResolver.NAME_RESOLVER_RESPONSE) && (Class<?>) intent.getExtras().get("resolver") == SSDP.class) {
        		updateListWithInterfaces((ArrayList<Interface>) intent.getExtras().get("result"), 1);
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
				updateListWithInterfaces((ArrayList<Interface>) _scanResult, 0);

			//************************** DEVICE RESULTS *************************//
			if(_resultType == ScanManager.ResultType.DEVICES)
				updateListWithDevices((ArrayList<Device>) _scanResult);
			
			if(_pd!=null)			// If there is a progress dialog up, cancel it
				_pd.dismiss();
		}
	}; 
	
	
	/** This updates the list with a set of devices
	 * @param devices The devices to populate the list with
	 */
	private void updateListWithDevices(ArrayList<Device> devices) {
		_deviceList=new ArrayList<HashMap<String,Object>>();
		_devices = new ArrayList<Device>();
		for(Device device : devices) {
			_devices.add(device);
			_deviceList.add(createListItem(device.getName(), device.getAdditional(_context), device));  
			debugOut(device.toString());
		}
		updateDeviceList();
	}
	
	/** 
	 * This updates the list with a set of interfaces.
	 * @param interfaces the interfaces to add to the list
	 */
	private void updateListWithInterfaces(ArrayList<Interface> interfaces, int useNames) {
		_deviceList=new ArrayList<HashMap<String,Object>>();
		Collections.shuffle(interfaces);
		
		for(Interface iface : interfaces) {		// For each interface
			
			String name = "";			// We need a name
			String additional = "";		// And some additional information
			
			if(useNames==0) {
				if(iface.getClass() != WirelessInterface.class)	// For the sake of demonstration
					continue;
				name = Interface.simplifiedClassName(iface._type) + " Radio";
				additional = "Signal strength: " + ((WirelessInterface)iface).averageRSSI() + "dBm";
			}
			
			if(useNames==1) {
				name = Interface.simplifiedClassName(iface._type);
				additional = iface._ouiName;
			}
			
			if(useNames==2) {							// If we are using names, we use all devices in the list.
				if(iface._ifaceName!=null)
					name = iface._ifaceName;		// Use the specified interface name
				if(iface._ouiName!=null)
					additional =  iface._ouiName; 	// And list the OUI name also for the sake of it
			}
			
			_deviceList.add(createListItem(name, additional, iface));
			debugOut(iface.toString());
		}
		updateDeviceList();		// Update and populate the actual device list
	}
	
	/** This checks the list to see if this name is already used.
	 * @param name The name to check
	 * @return true if the name is in use, false otherwise.
	 */
	private boolean isListNameUsed(String name) {
		for(HashMap<String, Object> item : _deviceList)
			if(item.get("name").equals(name))
				return true;
		return false;
	}
	
	/** Returns an item in the device list based on the name.
	 * @param name The name of the item
	 * @return the list item
	 */
	private HashMap<String,Object> getListItemByName(String name) {
		for(HashMap<String, Object> item : _deviceList)
			if(item.get("name").equals(name))
				return item;
		return null;
	}
	
	/** This method takes a name of a device or interface that would be inserted
	 * in to the list, and makes sure it is un ique by returning a new name if it is not
	 * unique.  Otherwise, it uses that name and puts it in the hash map.
	 * @param name The name that wishes to be inserted.
	 * @param o The object to be inserted corresponding to the name
	 * @return The actual name used in String format
	 */
	private HashMap<String,Object> createListItem(String name, String additional, Object o) {
		HashMap<String , Object> listItem = new HashMap<String, Object>();
		int i = 1;
		
		String uniqueName = name;
		while(isListNameUsed(uniqueName)) {
			uniqueName = name + " (" + i +")";
			i++;
		}
		
		listItem.put("name", uniqueName);
		listItem.put("additional", additional);
		listItem.put("object", o);
		listItem.put("objectType", o.getClass());
		
		return listItem;
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
			
			if(_resultType==ScanManager.ResultType.DEVICES)
				for(int i=0; i<checkBoxState.length; i++)
					if(_devices.get(i).getInternal())
						checkBoxState[i]=true;
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
			
			viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if(((CheckBox)v).isChecked())
						checkBoxState[position]=true;
					else
						checkBoxState[position]=false;
				}
			});
			
			String name = (_deviceList.get(position).get("name")==null) ? "" : _deviceList.get(position).get("name").toString();
			String additional = (_deviceList.get(position).get("additional")==null) ? "" : _deviceList.get(position).get("additional").toString();
			
			// For long additional description, truncate it
			if(additional.length()>37)
				additional = additional.substring(0, 37) + "...";
			
			// Set the actual items in the list with name and any additional info
			viewHolder.name.setText(name);
			viewHolder.additional.setText(additional);
			viewHolder.checkBox.setChecked(checkBoxState[position]);
			
			viewHolder.name.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					
					if(_resultType != ScanManager.ResultType.DEVICES)
						return;

					// First, let's get the device that the user clicked on and its name
					String name = ((TextView)v).getText().toString();
					HashMap<String,Object> item = getListItemByName(name);
					Device device = (Device) item.get("object");
					
					final Dialog dialog = new Dialog(_context);
					dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
					dialog.setContentView(R.layout.device_info_box);
					
					// Set the device name
					TextView deviceNameText = (TextView) dialog.findViewById(R.id.deviceName);
					deviceNameText.setText(Html.fromHtml("<h2>" + name + "</h2>"));
					
					// Set the manufacturer
					TextView manufacturerText = (TextView) dialog.findViewById(R.id.deviceManufacturer);
					manufacturerText.setText(Html.fromHtml(device.getManufacturer()));
					
					// Add all of the devices to the text box
					TextView deviceInfo = (TextView) dialog.findViewById(R.id.deviceInfo);
					deviceInfo.setText("");
					for(Interface iface : device.getInterfaces())
						deviceInfo.append(Html.fromHtml(iface.toFormattedString() + "\n"));
					
					Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
					dialogButton.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					});
					dialog.show();
				}
			});

			return convertView;
		}

	}
	
	public void clickedRescan(View v) {
		debugOut("Got a reclick");
		saveDeviceSelections(true,false);
	}
	
	public void clickedFinish(View v) {
		debugOut("Got a click on finished");
		saveDeviceSelections(false, true);
	}
	
	void saveDeviceSelections(boolean triggerScan, boolean nextWindow) {
		_pd = ProgressDialog.show(_context, "", "Storing your selections, please wait...", true, false);
		
		for(int i=0; i<_adapter.checkBoxState.length; i++) {
			if(_adapter.checkBoxState[i])
				_devices.get(i).setInternal(true);
			else
				_devices.get(i).setInternal(false);					
		}
		
		class UpdateDevicesThread implements Runnable { 
			ArrayList<Device> _devices;
			boolean _triggerScan;
			boolean _nextWindow;
			
			public UpdateDevicesThread(ArrayList<Device> devices, boolean triggerScan, boolean nextWindow) {
				_devices = devices;
				_triggerScan=triggerScan;
				_nextWindow=nextWindow;
			}
			
			@Override
			public void run() {
				Date before = new Date();
				// Let's store this badboy in the database now
    			debugOut("Opening the database");
    			DBAdapter dbAdapter = new DBAdapter(_context);
    			dbAdapter.open();
    			debugOut("Updating the devices...");
    			dbAdapter.updateDevices(_devices, NameUpdate.SAFE_UPDATE);
    			debugOut("Closing the database...");
    			dbAdapter.close();
    			Date after = new Date();
    			debugOut("..done: " + (after.getTime()-before.getTime())/1000);
    			if(_pd!=null)
    				_pd.cancel();
    			if(_triggerScan)
    				startScan();
    			
    			if(_nextWindow) {
    				
    				FileOutputStream data_ostream;
    				try {
    					data_ostream = _context.openFileOutput("your_devices_activity.json", Context.MODE_WORLD_READABLE | Context.MODE_APPEND);
    				
    					JSONObject json = new JSONObject();
    					
    					json.put("date", dateFormat.format(new Date()));
    					
    					int checked=0;
    					int unchecked=0;
    					for(int i=0; i<_adapter.checkBoxState.length; i++)
    						if(_adapter.checkBoxState[i])
    							checked++;
    						else
    							unchecked++;				
    					json.put("checked", checked);
    					json.put("unchecked", unchecked);
    					json.put("total", checked+unchecked);
    					
    					data_ostream.write(json.toString().getBytes());
    					data_ostream.write("\n".getBytes()); 
    					data_ostream.close();
    				
    				} catch(Exception e) {  }	
    				
	    			_handler.post(new Runnable() {	// Must do this on the main UI thread...
	    				@Override
	    				public void run() {						
	    	    			Intent i = new Intent(YourDevices.this, MissingDevices.class);
	    	    	        startActivity(i);
	    	    	    	finish();
	    				}
	    			});
    			}
			}
		}
		
		UpdateDevicesThread thread = new UpdateDevicesThread(_devices, triggerScan, nextWindow);
		new Thread(thread).start();
	}

	@Override
	public void onBackPressed() {
		Intent i = new Intent(YourDevices.this, TurnDevicesOn.class);
		startActivity(i);
		finish();
	}

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
