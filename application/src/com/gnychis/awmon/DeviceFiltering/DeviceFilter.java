package com.gnychis.awmon.DeviceFiltering;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Device;

/**
 * The purpose of the device filters is to get a list of devices and filter out devices that are for some reason
 * invalid or they are guaranteed to not belong to the home user, ultimately cleaning up a "bulk list" of devices
 * within range of them. 
 * 
 * @author George Nychis (gnychis)
 */
public abstract class DeviceFilter extends AsyncTask<ArrayList<Device>, Integer, ArrayList<Device> > {

	public static final String DEVICE_FILTER_RESPONSE = "awmon.devicefiltering.filter_response";
	public static final String TAG = "DeviceFilter";
	public static final boolean VERBOSE = true;
	
	public enum FilterStrength {
		LIKELY_FILTER_OUT,		// The device should likely be filtered
		UNLIKELY_FILTER_OUT,	// It is unlikely that the device should be filtered
		UNDETERMINED,			// Cannot determine
		FILTER_OUT,				// Definitely filter it out
		DO_NOT_FILTER,			// Definitely do not filter the device out
	}
	
	Context _parent;		// Need the parent to do things like send broadcasts.
	
	public DeviceFilter(Context c) {
		_parent = c;
	}
	
	@Override @SuppressWarnings("incomplete-switch")
	protected ArrayList<Device> doInBackground( ArrayList<Device> ... params )
	{
		ArrayList<Device> devices = params[0];
		ArrayList<Device> filteredDevices = new ArrayList<Device>();
		debugOut("In the background thread for " + this.getClass().getName());
		
		debugOut("Running the filter for " + this.getClass().getName());
		for(Device device : devices) {
			switch(getFilterResult(device)) {
				case FILTER_OUT:
					filteredDevices.add(device);
				break;
			}
		}
		debugOut("... filter finished");

		// Now, apply the classification done by the heuristic to the graph
		debugOut("Updating the device list based on the classifications done by " + this.getClass().getName());
		for(Device filteredDev : filteredDevices)
			devices.remove(filteredDev);
		debugOut("... done");
		
		//try { Thread.sleep(10000); } catch(Exception e) {} // FIXME 

		return devices;
	}
	
    @Override
    protected void onPostExecute(ArrayList<Device> devices) {    	
		Intent i = new Intent();
		i.setAction(DEVICE_FILTER_RESPONSE);
		i.putExtra("filter", this.getClass());
		i.putExtra("result", devices);
		_parent.sendBroadcast(i);
    	debugOut("Finished the filtering for " + this.getClass().getName() + ", sent broadcast");
    }
	
	abstract public FilterStrength getFilterResult(Device device);
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
