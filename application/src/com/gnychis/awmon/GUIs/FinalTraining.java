package com.gnychis.awmon.GUIs;

import java.awt.Checkbox;
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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.BackgroundService.ScanManager;
import com.gnychis.awmon.Core.DialogActivity;
import com.gnychis.awmon.Core.ScanRequest;
import com.gnychis.awmon.Database.DBAdapter;
import com.gnychis.awmon.Database.DBAdapter.NameUpdate;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Device.Mobility;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

/**
 * The purpose of this GUI interface is to do a bulk scan for devices in range and then
 * to ask the user which of these devices belong to them via a checklist.  We can then
 * mark these devices as belonging to the user.
 * 
 * @author George Nychis (gnychis)
 */
public class FinalTraining extends Activity {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 

	public static final String TAG = "FinalTraining";
	public static final boolean VERBOSE = true;
    private Context _context;
    Date _activityStartTime;
    
    String _lastName;
    int _renamed;
    int _notRenamed;
    Device _thisDevice;
    int _lastPosition;
    Checkbox _lastCheckbox;
    int _mobile;
    
    String _snapshotName;
    
    FinalTraining _this;

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
		setContentView(R.layout.train_system);
		_context = this;
		_this = this;

		_handler = new Handler();
				
		DBAdapter dbAdapter = new DBAdapter(this);
		dbAdapter.open();
		_internalDevices = dbAdapter.getInternalDevices();
		dbAdapter.close();
		
		_renamed=0;
		_notRenamed=0;
		_mobile=0;
		
		Button b = (Button) findViewById(R.id.buttonContinue);
		b.setEnabled(false);
		updateListWithDevices(_internalDevices);		
		_resultType = ScanManager.ResultType.DEVICES;
	}
	
	public void startScan(String name) {
		_handler.post(new Runnable() {	// Must do this on the main UI thread...
			@Override
			public void run() {						
				// Pop up a progress dialog and register receivers for progress being made by the scanning service
				_pd = ProgressDialog.show(_context, "", "Scanning for devices", true, false);
			}
		});
		_snapshotName=name;
		
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
						request.makeSnapshot();
						request.setSnapshotName("Training " + _snapshotName);
						request.send(_context);						// Send the request to the background service
					}
				});
			}
		}, 1000);
	}
	
	public void registerReceivers() {
		registerReceiver(_deviceScanReceiver, new IntentFilter(ScanManager.SCAN_RESPONSE));
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
	}	

	/**
	 * This will trigger when the background scan for devices or interfaces has been completed, it will
	 * then trigger the list to be updated and displayed.
	 */
	private BroadcastReceiver _deviceScanReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		public void onReceive(Context context, Intent intent) {

			//_resultType = (ScanManager.ResultType) intent.getExtras().get("type");
			//_scanResult = intent.getExtras().get("result");

			//************************ INTERFACE RESULTS *************************//
			//if(_resultType == ScanManager.ResultType.INTERFACES)
			//	updateListWithInterfaces((ArrayList<Interface>) _scanResult, 0);

			//************************** DEVICE RESULTS *************************//
			//if(_resultType == ScanManager.ResultType.DEVICES)
			//	updateListWithDevices((ArrayList<Device>) _scanResult);
			
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
	
	/** Returns an item in the device list based on the name.
	 * @param name The name of the item
	 * @return the list item
	 */
	private int getListPositionByName(String name) {
		int i=0;
		for(HashMap<String, Object> item : _deviceList) {
			if(item.get("name").equals(name))
				return i;
			i++;
		}
		return -1;
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
        		_adapter=new CustomAdapter(_context, R.layout.device_list_item2 ,_deviceList); 
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
				convertView=inflater.inflate(R.layout.device_list_item2, null);
				viewHolder=new ViewHolder();

				//cache the views
				viewHolder.name=(TextView) convertView.findViewById(R.id.name);
				viewHolder.additional=(TextView) convertView.findViewById(R.id.additional);
				//viewHolder.checkBox=(CheckBox) convertView.findViewById(R.id.checkBox);
				//viewHolder.checkBox.setClickable(false);

				//link the cached views to the convertview
				convertView.setTag( viewHolder);
			}
			else {
				viewHolder=(ViewHolder) convertView.getTag();
				//viewHolder.checkBox=(CheckBox) convertView.findViewById(R.id.checkBox);
				//viewHolder.checkBox.setClickable(false);			
			}
			
			
			String name = (_deviceList.get(position).get("name")==null) ? "" : _deviceList.get(position).get("name").toString();
			String additional = (_deviceList.get(position).get("additional")==null) ? "" : _deviceList.get(position).get("additional").toString();
			
			HashMap<String,Object> item = getListItemByName(name);
			if(item.get("object").getClass()==Device.class) {
				Device device = (Device) item.get("object");
			}
			
			boolean allChecked=true;
			for(int i=0; i<_adapter.checkBoxState.length; i++)
				if(!_adapter.checkBoxState[i])
					allChecked=false;
			if(allChecked) {
				Button b = (Button) _this.findViewById(R.id.buttonContinue);
				b.setEnabled(true);
			}
				
			
			// For long additional description, truncate it
			if(additional.length()>37)
				additional = additional.substring(0, 37) + "...";
			
			// Set the actual items in the list with name and any additional info
			viewHolder.name.setText(name);
			viewHolder.additional.setText(additional);
			//viewHolder.checkBox.setChecked(checkBoxState[position]);
			//viewHolder.checkBox.setClickable(false);
			if(checkBoxState[position])
				convertView.setBackgroundColor(0x60FFFF00);
			
			viewHolder.name.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					
					if(_resultType != ScanManager.ResultType.DEVICES)
						return;

					// First, let's get the device that the user clicked on and its name
					String name = ((TextView)v).getText().toString();
					HashMap<String,Object> item = getListItemByName(name);
					Device device = (Device) item.get("object");
					_thisDevice = device;
					_lastPosition = getListPositionByName(name);
					if(checkBoxState[_lastPosition])
						return;
					
					AlertDialog.Builder alert = new AlertDialog.Builder(_context);

					alert.setTitle("Device Name");
					//alert.setMessage("Feel free to choose a different name for this device");

					// Set an EditText view to get user input 
					final EditText input = new EditText(_context);
					alert.setView(input);
					input.setText(name);
					_lastName = name;
					
					alert.setPositiveButton("Fixed: This Device is Always In This Location", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							String value = input.getText().toString();	
							if(value.equals(_lastName))
								_notRenamed++;
							else
								_renamed++;
							_thisDevice.setMobility(Mobility.FIXED);
							_handler.post(new Runnable() {	// Must do this on the main UI thread...
								@Override
								public void run() {						
									checkBoxState[_lastPosition]=true;
									_adapter.notifyDataSetChanged();
									_this.startScan(_lastName);
								}
							});
						}
					});

					alert.setNegativeButton("Mobile: This Devices Changes Locations", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							String value = input.getText().toString();
							if(value.equals(_lastName))
								_notRenamed++;
							else
								_renamed++;
							_thisDevice.setMobility(Mobility.MOBILE);
							_mobile++;
							_handler.post(new Runnable() {	// Must do this on the main UI thread...
								@Override
								public void run() {						
									checkBoxState[_lastPosition]=true;
									_adapter.notifyDataSetChanged();
									_this.startScan(_lastName);
								}
							});
						}
					});
					
					/*viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							if(((CheckBox)v).isChecked()) {
								checkBoxState[position]=true;
							} else {
								checkBoxState[position]=false;
							}
						}
					});*/

					alert.show();
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
    			
    			if(_nextWindow) {
    				
    				FileOutputStream data_ostream;
    				try {
    					data_ostream = _context.openFileOutput("final_training_activity.json", Context.MODE_WORLD_READABLE | Context.MODE_APPEND);
    				
    					JSONObject json = new JSONObject();
    					
    					json.put("date", dateFormat.format(new Date()));			
    					json.put("renamed", _renamed);
    					json.put("notRenamed", _notRenamed);
    					json.put("mobile", _mobile);
    					json.put("immobile", (_renamed+_notRenamed)-_mobile);
    					json.put("total", _renamed+_notRenamed);
    					
    					data_ostream.write(json.toString().getBytes());
    					data_ostream.write("\n".getBytes()); 
    					data_ostream.close();
    				
    				} catch(Exception e) {  }	
    				
	    			_handler.post(new Runnable() {	// Must do this on the main UI thread...
	    				@Override
	    				public void run() {						
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
		Intent i = new Intent(FinalTraining.this, TurnDevicesOn.class);
		startActivity(i);
		finish();
	}

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
