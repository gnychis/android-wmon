package com.gnychis.awmon.BackgroundService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.gnychis.awmon.Core.ScanRequest;
import com.gnychis.awmon.Core.Snapshot;
import com.gnychis.awmon.Database.DBAdapter;
import com.gnychis.awmon.Database.DBAdapter.NameUpdate;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Device.Mobility;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.DeviceFiltering.DeviceFilteringManager;
import com.gnychis.awmon.HardwareHandlers.HardwareHandler;
import com.gnychis.awmon.InterfaceMerging.InterfaceMergingManager;
import com.gnychis.awmon.InterfaceScanners.InterfaceScanManager;
import com.gnychis.awmon.NameResolution.NameResolutionManager;

@SuppressWarnings("unchecked")
public class ScanManager {
	
	private static final String TAG = "ScanManager";
	private static final boolean VERBOSE = true;
	
	Context _parent;								// Access to the parent class for broadcasts
	HardwareHandler _hardwareHandler;				// To have access to the internal radios
	NameResolutionManager _nameResolutionManager;	// For resolving the names of interfaces
	ScanRequest _workingRequest;					// The most recent scan request we are working on
	InterfaceScanManager _ifaceScanManager;			// Scan for interfaces.
	InterfaceMergingManager _ifaceMergingManager;	// To merge interfaces in to devices
	DeviceFilteringManager _deviceFilteringManager;	// To filter devices that definitely do not belong to the user
	
	public static final String SCAN_REQUEST = "awmon.scanmanager.scan_request";
	public static final String SCAN_RESPONSE = "awmon.scanmanager.scan_response";

	State _state;
	public enum State {
		IDLE,
		SCANNING,
		NAME_RESOLUTION,
		INTERFACE_MERGING,
		DEVICE_FILTERING,
	}
	
	public enum ResultType {
		INTERFACES,
		DEVICES,
	}
	
	/** Parent is anything we can send a broadcast from.  HardwareHandler is needed to access the
	 * internal radios.  This allows us to see if the radio is connected and request a scan from them.
	  * 
	  * @param p  Any parent context.
	  * @param dh A device handler
	  *
	*/
	public ScanManager(Context p, HardwareHandler dh) {
		_state=State.IDLE;
		_parent=p;
		
		_hardwareHandler=dh;
		_nameResolutionManager = new NameResolutionManager(_parent);
		_ifaceScanManager = new InterfaceScanManager(dh);
		_ifaceMergingManager = new InterfaceMergingManager(_parent);
		_deviceFilteringManager = new DeviceFilteringManager(_parent);
		
		_parent.registerReceiver(incomingEvent, new IntentFilter(ScanManager.SCAN_REQUEST));
		_parent.registerReceiver(incomingEvent, new IntentFilter(InterfaceScanManager.INTERFACE_SCAN_RESULT));
		_parent.registerReceiver(incomingEvent, new IntentFilter(NameResolutionManager.NAME_RESOLUTION_RESPONSE));
		_parent.registerReceiver(incomingEvent, new IntentFilter(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE));
		_parent.registerReceiver(incomingEvent, new IntentFilter(DeviceFilteringManager.DEVICE_FILTERING_RESPONSE));
	}
	
	private void broadcastResults(ScanManager.ResultType type, ArrayList<?> results) {
		Intent i = new Intent();
		i.setAction(SCAN_RESPONSE);
		i.putExtra("type", type);
		i.putExtra("result", results);
		_parent.sendBroadcast(i);
	}
	
	/** This is an incoming scan request.  A ScanRequest must be passed with it so we know what kind of
	 * scan is being requested.*/
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	
        	// Before we do ANYTHING about scan states, etc.  We ALWAYS save incoming interface scans
        	// as snapshots and then rebroadcast out "HEY WE GOT A SNAPSHOT!"  This is so any other
        	// activities can use this data.
			if(intent.getAction().equals(InterfaceScanManager.INTERFACE_SCAN_RESULT)) {
				ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("result");
				makeSnapshot(interfaces);
			}
        	
        	switch(_state) {	// Based on the current state, decide what next to do
        	
        		/***************************** IDLE **********************************/
        		case IDLE:
        			
        			if(intent.getAction().equals(ScanManager.SCAN_REQUEST)) {
        				
        				debugOut("Got an incoming scan request in the idle state");
        				
        				// Get the type of scan request, then cache it as our active request
        				ScanRequest request = null;
        				if((request = (ScanRequest) intent.getExtras().get("request"))==null)
        					return;
        				_workingRequest = request;
        				debugOut("... doNameResolution: " + _workingRequest.doNameResolution()
        						 	+ "   doMerging: " + _workingRequest.doMerging());
        				
        				// Go ahead and switch out state to scanning, then send out the request
        				// for an interface scan.
        				Intent i = new Intent();
        				i.setAction(InterfaceScanManager.INTERFACE_SCAN_REQUEST);
        				_parent.sendBroadcast(i);
        				
        				_state=State.SCANNING;       // We are scanning now!	
        				debugOut("Sent the scan request to scan on the hardware");
        			}
        
    			break;
    			
    			/*************************** SCANNING ********************************/
        		case SCANNING:
        			
        			if(intent.getAction().equals(InterfaceScanManager.INTERFACE_SCAN_RESULT)) {
        				ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("result");
        				
        				debugOut("Received the results from the interface scan");
        				
        				// Now, we decide to do name resolution and merging.  If we do not do name resolution,
        				// then we do NOT merge.  This is because a significant portion of merging uses names.
        				if(!_workingRequest.doNameResolution()) {
        					broadcastResults(ScanManager.ResultType.INTERFACES, interfaces);
        					_state=State.IDLE;
        					debugOut("Name resolution was not set, returning to idle");
        					return;
        				}
        				
        				// Send the request to do name resolution on the interfaces, passing them along
        				_nameResolutionManager.requestNameResolution(interfaces);
        				
        				_state=ScanManager.State.NAME_RESOLUTION;
        				debugOut("Name resolution was set, we are now resolving!");
        			}
        			
    			break;
    			
    			/*************************** RESOLVING ********************************/
        		case NAME_RESOLUTION:
        			
        			if(intent.getAction().equals(NameResolutionManager.NAME_RESOLUTION_RESPONSE)) {
        				ArrayList<Interface> interfaces = (ArrayList<Interface>) intent.getExtras().get("result");
        				
        				debugOut("Receveived the interfaces from the name resolution manager");
        				
        				// If we are not doing merging (OK), then we just return the interfaces with names.
        				if(!_workingRequest.doMerging()) {
        					broadcastResults(ScanManager.ResultType.INTERFACES, interfaces);
        					_state=State.IDLE;
        					debugOut("Merging was not set, returning to the idle state");
        					return;
        				}
        				
        				// Send the request to do interface merging, passing them along
        				_ifaceMergingManager.requestMerge(interfaces);
        				
    					_state=ScanManager.State.INTERFACE_MERGING;
    					debugOut("Merging was set, let's try to merge the devices in to interfaces");
        			}
        			
        		break;
        		
        		/**************************** MERGING *********************************/
        		case INTERFACE_MERGING:
        			
        			if(intent.getAction().equals(InterfaceMergingManager.INTERFACE_MERGING_RESPONSE)) {
        				ArrayList<Device> devices = (ArrayList<Device>) intent.getExtras().get("result");
        				
        				debugOut("Receveived the devices from interface merging manager");
        				
        				if(!_workingRequest.doFiltering()) {
	        				broadcastResults(ScanManager.ResultType.DEVICES, devices);
	        				_state = State.IDLE;
	        				return;
        				}
        				_deviceFilteringManager.requestFiltering(devices);
        				
        				_state=ScanManager.State.DEVICE_FILTERING;
        				debugOut("Device filtering was set was set, let's try to filter some junk");
        			}
        			
        		break;
        		
        		/**************************** FILTERING *********************************/
        		case DEVICE_FILTERING:
        			
        			if(intent.getAction().equals(DeviceFilteringManager.DEVICE_FILTERING_RESPONSE)) {
        				ArrayList<Device> devices = (ArrayList<Device>) intent.getExtras().get("result");
        				
        				debugOut("Receveived the devices from filtering manager");
        					
	        			devices = updateDevices(devices);	        
	        			
	        			broadcastResults(ScanManager.ResultType.DEVICES, devices);
        				
	        			debugOut("Done with the chain, heading back to idle");
	        			_state = State.IDLE;
	        			
	        			return;
        			}
        			
        		break;
        		
        	}
    	}
    };
    
	
	/** This should only be accessed privately, and only accessed by the event manager
	 * @param interfaces
	 */
	private void makeSnapshot(ArrayList<Interface> interfaces) {
    	final int ANCHOR_RSSI_THRESHOLD = -30;   // dBm
    	
		Snapshot snapshot = new Snapshot();
		snapshot.setName(_workingRequest.getSnapshotName());
		snapshot.add(interfaces);
		
		// If a manual anchor was specified in the ScanRequest, save that with the snapshot
		if(_workingRequest.getAnchor()!=null) {
			snapshot.forceAnchor(_workingRequest.getAnchor());
		} else {
			Interface anchor=null;
			// Determine the anchor if there is one
			for(Interface iface : interfaces) {
				
				// Go through all of the wireless interfaces and see if one exceeds our threshold
				if(iface.getClass()==WirelessInterface.class) {
					
					boolean exceeded=false;
					
					for(int rssiVal : ((WirelessInterface)iface).rssiValues())
						if(rssiVal > ANCHOR_RSSI_THRESHOLD)
							exceeded=true;
					
					if(exceeded) {
						anchor=iface;
						break;
					}
					
				} // end if
			} // end for
			
			if(anchor!=null)
				snapshot.setAnchor(anchor);
		}
		
		storeSnapshot(snapshot);
		
		snapshot.broadcast(_parent);
	}
	
	public void storeSnapshot(Snapshot snapshot) {
		class StoreSnapshotThread implements Runnable { 
			Snapshot _snapshot;
			
			public StoreSnapshotThread(Snapshot snapshot) {
				_snapshot=snapshot;
			}
			
			@Override
			public void run() {
				// Let's store this badboy in the database now
				Date before = new Date();
				debugOut("Opening the database to write interfaces");
				DBAdapter dbAdapter = new DBAdapter(_parent);
				dbAdapter.open();
				debugOut("Now, storing the snapshots");
				dbAdapter.storeSnapshot(_snapshot);
				debugOut("Closing the database...");
				dbAdapter.close();
    			Date after = new Date();
    			debugOut("..done: " + (after.getTime()-before.getTime())/1000);			}
		}
		
		StoreSnapshotThread thread = new StoreSnapshotThread(snapshot);
		new Thread(thread).start();
	}
	
	public ArrayList<Device> updateDevices(ArrayList<Device> devices) {
		
		ArrayList<Device> newDevices = new ArrayList<Device>();
		
		Date before = new Date();
		debugOut("Opening the database");
		DBAdapter dbAdapter = new DBAdapter(_parent);
		dbAdapter.open();
		
		debugOut("Updating the devices...");
		for(Device d : devices) {
			ArrayList<Device> devsInDB = new ArrayList<Device>();
			
			// First, update the interfaces
			updateInterfaces(d.getInterfaces());
			
			for(Interface iface : d.getInterfaces()) {
				Device tmpDev = dbAdapter.getDevice(iface._MAC);
				if(tmpDev!=null)
					devsInDB.add( tmpDev );
			}
			
			// There are no devices for this, we can go ahead and "update" which will insert it
			if(devsInDB.size()==0) {
				dbAdapter.updateDevice(d, NameUpdate.SAFE_UPDATE);
				newDevices.add(d);
				continue;
			}
			
			// These interfaces already agree, just update the interfaces
			if(devsInDB.size()==1)		 {
				newDevices.add(devsInDB.get(0));	// Add the version from the DB to keep device keys consistent
    			continue;
			}
			
			// If it's greater than 1, we need to merge them
			if(devsInDB.size()>1) {
				Device mergedDev = new Device();

				ArrayList<Interface> interfaces = new ArrayList<Interface>();
				for(Device d2 : devsInDB) {
					
					// Save the important data and merge it from the devices
					if(d2.getInternal())
						mergedDev.setInternal(true);
					if(d2.getUserName()!=null)
						mergedDev.setUserName(d2.getUserName());
					if(d2.getMobility()!=Mobility.UNKNOWN)
						mergedDev.setMobility(d2.getMobility());
					
					interfaces.addAll(d2.getInterfaces());	// save this device's interfaces
					dbAdapter.deleteDevice(d2);				// Remove the old device
				}
				
				mergedDev.addInterfaces(interfaces);	// Add the merged interfaces
				dbAdapter.updateDevice(mergedDev, NameUpdate.UPDATE);
				newDevices.add(mergedDev);
			}	
		}
		
		dbAdapter.close();
		Date after = new Date();
		debugOut("..done: " + (after.getTime()-before.getTime())/1000);
		return newDevices;
	}
	
	public void updateInterfaces(List<Interface> interfaces) {
		class UpdateInterfacesThread implements Runnable { 
			List<Interface> _interfaces;
			
			public UpdateInterfacesThread(List<Interface> interfaces) {
				_interfaces = interfaces;
			}
			
			@Override
			public void run() {
				Date before = new Date();
				// Let's store this badboy in the database now
    			debugOut("Opening the database");
    			DBAdapter dbAdapter = new DBAdapter(_parent);
    			dbAdapter.open();
    			debugOut("Updating the interfaces...");
    			dbAdapter.updateInterfaces(_interfaces, NameUpdate.SAFE_UPDATE);
    			debugOut("Closing the database...");
    			dbAdapter.close();
    			Date after = new Date();
    			debugOut("..done: " + (after.getTime()-before.getTime())/1000);
			}
		}
		
		UpdateInterfacesThread thread = new UpdateInterfacesThread(interfaces);
		new Thread(thread).start();
	}
    
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
