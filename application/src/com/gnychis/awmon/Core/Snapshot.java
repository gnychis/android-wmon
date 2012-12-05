package com.gnychis.awmon.Core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;

public class Snapshot implements Parcelable {
		
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	private String _name;						// A name for the snapshot, which doesn't have to be unique
	private Date _snapshotDateTime; 			// The date/time of the snapshot
	private ArrayList<Interface> _interfaces;	// The interfaces that were "sensed" during the snapshot
	private String _anchor;						// The MAC of the interface that wast the anchor
	private int _snapshotKey;					// Key for the snapshot
	
	public static final String SNAPSHOT_DATA = "awmon.scanmanager.snapshot";
	
	public void broadcast(Context c) {
		Intent i = new Intent();
		i.setAction(SNAPSHOT_DATA);
		i.putExtra("snapshot", this);
		c.sendBroadcast(i);
	}
	
	public Snapshot() {
		_interfaces = new ArrayList<Interface>();	// The interfaces are not initialized
		_anchor=null;								// Anchor not initially set in this case
		_snapshotDateTime = new Date();				// The snapshot takes the current time
		_name=null;
		_snapshotKey=-1;
	}
	
	public Snapshot(Date timeOfSnapshot) {
		_interfaces = new ArrayList<Interface>();
		_anchor = null;
		_snapshotDateTime=timeOfSnapshot;
		_snapshotKey=-1;
		_name=null;
	}
	
	public Snapshot(Date timeofSnapshot, ArrayList<? extends Object> devicesOrInterfaces, String anchor) {
		_snapshotDateTime=timeofSnapshot;
		_anchor = anchor;
		_name=null;
		_interfaces = new ArrayList<Interface>();
		_snapshotKey=-1;
		add(devicesOrInterfaces);
	}
	
	public Snapshot(ArrayList<? extends Object> devicesOrInterfaces, String anchor) {
		_snapshotDateTime = new Date();					// The snapshot takes the current time
		_anchor = anchor;
		_name=null;
		_interfaces = new ArrayList<Interface>();
		_snapshotKey=-1;
		add(devicesOrInterfaces);
	}
	public Snapshot(ArrayList<? extends Object> devicesOrInterfaces) {
		_snapshotDateTime = new Date();					// The snapshot takes the current time
		_interfaces = new ArrayList<Interface>();
		_name=null;
		_snapshotKey=-1;
		add(devicesOrInterfaces);
	}
	
	public void setSnapshotKey(int key) {
		_snapshotKey=key;
	}
	
	public int getSnapshotKey() {
		return _snapshotKey;
	}
	
	/** Set the name of a snapshot 
	 * @param name the specified name.
	 */
	public void setName(String name) {
		_name = name;
	}
	
	/** Get the name of the snapshot
	 * @return the name
	 */
	public String getName() {
		return _name;
	}

	/** Get the specific interface from the snapshot if it exists
	 * @param MAC the MAC that the interface should match
	 * @return the Interface, or null if it does not exist
	 */
	public Interface getInterface(String MAC) {
		for(Interface iface : _interfaces)
			if(iface._MAC!=null && MAC!=null && iface._MAC.equals(MAC))
				return iface;
		return null;
	}
	
	/** Set the time of the snapshot instance.
	 * @param timeOfSnapshot
	 */
	public void setSnapshotTime(Date timeOfSnapshot) {
		_snapshotDateTime=timeOfSnapshot;
	}
	
	/** Set the time of the snapshot instance by String.
	 * @param timeOfSnapshot
	 */
	public void setSnapshotTime(String timeString) {
		try { 
			Date snapshotDate = dateFormat.parse(timeString);
			_snapshotDateTime=snapshotDate;
		} catch(Exception e) {  }
	}
	
	public static Date getDateFromString(String timeString) {
		try { 
			Date snapshotDate = dateFormat.parse(timeString);
			return snapshotDate;
		} catch(Exception e) {  }
		return null;
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
	public boolean setAnchor(Interface iface) {
		return setAnchor(iface._MAC);
	}
	
	public void forceAnchor(String MAC) {
		_anchor=MAC;
	}
	
	/** Sets the anchor to the given MAC, but we must make sure that this
	 * interface is in fact in the scan results.
	 * @param MAC
	 * @return
	 */
	public boolean setAnchor(String MAC) {
		boolean haveit=false;
		for(Interface i : _interfaces)
			if(i._MAC!=null && MAC!=null && i._MAC.equals(MAC))
				haveit=true;
		if(haveit==false)
			return false;
		_anchor = MAC;
		return true;
	}
	
	/** Returns the current anchor MAC of the snapshot
	 * @return
	 */
	public String getAnchorMAC() {
		return _anchor;
	}
	
	/** Returns the current anchor of the snapshot
	 * @return
	 */
	public Interface getAnchor() {
		if(_anchor==null)
			return null;
		Interface anchor=null;
		for(Interface i : _interfaces)
			if(i._MAC!=null && _anchor!=null && i._MAC.equals(_anchor))
				anchor=i;
		return anchor;
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
	
	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public static final Parcelable.Creator<Snapshot> CREATOR = new Parcelable.Creator<Snapshot>() {
		public Snapshot createFromParcel(Parcel in) {
			return new Snapshot(in);
		}

		public Snapshot[] newArray(int size) {
			return new Snapshot[size];
		}
	};

	public void writeToParcel(Parcel dest, int parcelableFlags) {
		dest.writeString(_name);
		dest.writeString(this.getSnapshotTimeString());
		dest.writeList(_interfaces);
		dest.writeString(_anchor);
		dest.writeInt(_snapshotKey);
	}
	
	//@SuppressWarnings("unchecked")
	private Snapshot(Parcel source) {
		_name = source.readString();
		setSnapshotTime(source.readString());
		_interfaces = new ArrayList<Interface>();
    	source.readList(_interfaces, this.getClass().getClassLoader());
    	_anchor = source.readString();
    	_snapshotKey = source.readInt();
	}
}
