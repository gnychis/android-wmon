package com.gnychis.awmon.Core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;

public class Snapshot {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	private Date _snapshotDateTime; 			// The date/time of the snapshot
	private ArrayList<Interface> _interfaces;	// The interfaces that were "sensed" during the snapshot
	private Interface _anchor;					// The interface that the snapshot was anchored to
	
	public Snapshot() {
		_interfaces = new ArrayList<Interface>();	// The interfaces are not initialized
		_anchor=null;								// Anchor not initially set in this case
		_snapshotDateTime = new Date();				// The snapshot takes the current time
	}
	
	public Snapshot(Date timeOfSnapshot) {
		_interfaces = new ArrayList<Interface>();
		_anchor = null;
		_snapshotDateTime=timeOfSnapshot;
	}
	
	public Snapshot(Date timeofSnapshot, ArrayList<? extends Object> devicesOrInterfaces, Interface anchor) {
		_snapshotDateTime=timeofSnapshot;
		_anchor = anchor;
		_interfaces = new ArrayList<Interface>();
		add(devicesOrInterfaces);
	}

	/** Get the specific interface from the snapshot if it exists
	 * @param MAC the MAC that the interface should match
	 * @return the Interface, or null if it does not exist
	 */
	public Interface getInterface(String MAC) {
		for(Interface iface : _interfaces)
			if(iface._MAC.equals(MAC))
				return iface;
		return null;
	}
	
	/** Set the time of the snapshot instance.
	 * @param timeOfSnapshot
	 */
	public void setSnapshotTime(Date timeOfSnapshot) {
		_snapshotDateTime=timeOfSnapshot;
	}
	
	/** Get the date/time of the snapshot.
	 * @return a Date representation of the snapshot time
	 */
	public Date getSnapshotTime() {
		return _snapshotDateTime;
	}
	
	/** Gets a string representation of the snapshot date/time
	 * @return a string representation of the snapshot time.
	 */
	public String getSnapshotTimeString() {
		if(_snapshotDateTime==null)
			return null;
		return dateFormat.format(_snapshotDateTime);
	}
	
	/** Retrieve the interfaces that were sensed in the snapshot.
	 * @return an ArrayList of Interfaces from the snapshot.
	 */
	public ArrayList<Interface> getInterfaces() {
		return _interfaces;
	}
	
	/** Sets the anchor to the given interface.  It is possible to
	 * set this to null if this was a snapshot not taken near anything.
	 * @param iface
	 */
	public void setAnchor(Interface iface) {
		_anchor = iface;
	}
	
	/** Returns the current anchor of the snapshot
	 * @return
	 */
	public Interface getAnchor() {
		return _anchor;
	}
	
	/** Allows you to add interfaces to the snapshot via an ArrayList of Devices or Interfaces.
	 * @param devicesOrInterfaces an ArrayList of Devices or Interfaces
	 */
	public void add(ArrayList<? extends Object> devicesOrInterfaces) {
		for(Object o : devicesOrInterfaces) {
			if(o.getClass()==Device.class)
				_interfaces.addAll( ((Device)o).getInterfaces()  );
				
			if(o.getClass().getSuperclass()==Interface.class || o.getClass()==Interface.class) 
				_interfaces.add( (Interface)o );
		}
	}
	
	/** Add a Device or an Interface to the snapshot
	 * @param deviceOrInterface the Interface or Device
	 */
	public void add(Object deviceOrInterface) {
		if(deviceOrInterface.getClass()==Device.class)
			_interfaces.addAll( ((Device)deviceOrInterface).getInterfaces());
		if(deviceOrInterface.getClass().getSuperclass()==Interface.class || deviceOrInterface.getClass()==Interface.class)
			_interfaces.add( (Interface)deviceOrInterface );
	}
	
}
