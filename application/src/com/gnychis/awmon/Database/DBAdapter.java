package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gnychis.awmon.Core.Snapshot;
import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WiredInterface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

@SuppressWarnings("unchecked")
public class DBAdapter {
	
	public static final boolean VERBOSE = true;
	
    // Setup a context and database instance
    private final Context context;
    private DatabaseHelper DBHelper;
    public SQLiteDatabase db;
    
    private static final String TAG = "DBAdapter";	// for debugging
    private static final String DATABASE_NAME = "awmon";
    private static final int DATABASE_VERSION = 2;
    
    public enum NameUpdate {
    	DO_NOT_UPDATE,	// Leave the naming alone
    	SAFE_UPDATE,	// Update the name if it is stored as null
    	UPDATE,			// Overwrite the name, regardless
    }
    
    HashMap<String,DBTable> _tables;
    
    void populateTables() {
    	_tables = new HashMap<String,DBTable>();
    	_tables.put(DevicesTable.TABLE_NAME, 		new DevicesTable(this));
    	_tables.put(InterfacesTable.TABLE_NAME, 	new InterfacesTable(this));
    	_tables.put(SnapshotsTable.TABLE_NAME,		new SnapshotsTable(this));
    	_tables.put(WiredIfaceTable.TABLE_NAME,		new WiredIfaceTable(this));
    	_tables.put(WirelessIfaceTable.TABLE_NAME,	new WirelessIfaceTable(this));
    }
    
    public DBAdapter(Context ctx) 
    {
        this.context = ctx;
        populateTables();  // This must happen first right after setting the context

        debugOut("Populated the tables, now creating the DatabaseHelper");
        DBHelper = new DatabaseHelper(context, new ArrayList<DBTable>(_tables.values()));
    }
    
    //******* SNAPSHOT *************** WRITE HELPER FUNCTIONS ****************************//
    public void storeSnapshot(Snapshot s) {  // We only insert snapshots, no updates.
    	
    	// First, we store the snapshot header data    	
    	_tables.get(SnapshotsTable.TABLE_NAME).insert(s);
    	
    	// Then, we read back the unique key that the database assigned
    	ContentValues conditions = new ContentValues();
    	conditions.put("date", s.getSnapshotTimeString());
    	int key=-1;
    	for(Object obj : _tables.get(SnapshotsTable.TABLE_NAME).retrieveField("snapshotKey", conditions, false)) {
    		key = (Integer)obj;
    		break;
    	}
    	
    	s.setSnapshotKey(key);
    	_tables.get(SnapshotsDataTable.TABLE_NAME).insert(s);
    }
    
    //******* SNAPSHOT *************** READ HELPER FUNCTIONS ****************************//
    /** Get all snapshots from the database
     * @return an ArrayList of snapshots from the database, no restrictions.
     */
    public ArrayList<Snapshot> getSnapshots() {
    	
    	// First, get the bare snapshots without their interface data
    	ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
    	for(Object o : _tables.get(SnapshotsTable.TABLE_NAME).retrieve(null))
    		snapshots.add((Snapshot)o);
    	
    	// Now, get the interface data for each snapshot
    	for(Snapshot snapshot : snapshots) {
    		ArrayList<Interface> interfaces = new ArrayList<Interface>();
	    	ContentValues conditions = new ContentValues();
	    	conditions.put("snapshotKey", snapshot.getSnapshotKey());
	    	for(Object o : _tables.get(SnapshotsDataTable.TABLE_NAME).retrieve(conditions))
	    		interfaces.add((Interface)o);
	    	snapshot.add(interfaces);
    	}
    	
    	return snapshots;
    }
    
    /** Return a snapshot with the specified date.
     * @param d the date of the snapshot
     * @return the specified snapshot if it exists, null otherwise
     */
    public Snapshot getSnapshot(Date d) {
    	if(d==null)
    		return null;
    	
    	Snapshot snapshot = null;
    	
    	// First, get the snapshot header data
    	ContentValues conditions = new ContentValues();
    	conditions.put("date", DBTable.dateFormat.format(d));
    	for(Object o : _tables.get(SnapshotsTable.TABLE_NAME).retrieve(conditions)) {
    		snapshot = ((Snapshot)o);
    		break;
    	}
    	
    	if(snapshot==null)
    		return null;
    	
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
    	conditions = new ContentValues();
    	conditions.put("snapshotKey", snapshot.getSnapshotKey());
    	for(Object o : _tables.get(SnapshotsDataTable.TABLE_NAME).retrieve(conditions))
    		interfaces.add((Interface)o);
    	snapshot.add(interfaces);
    	
    	return null;
    }
    
    //******* SNAPSHOT *************** DELETE HELPER FUNCTIONS ****************************//
    public boolean deleteSnapshot(Date d) {
    	if(d==null)
    		return false;
    	ContentValues conditions = new ContentValues();
    	conditions.put("date", DBTable.dateFormat.format(d));
    	return _tables.get(SnapshotsTable.TABLE_NAME).delete(conditions);	
    }
         
    //******* DEVICE *************** WRITE HELPER FUNCTIONS ****************************//
    /** Stores the given device in the database, this will either insert or update it.
     * @param d the Device to be inserted/updated.
     * @param nameUpdate this is whether or not the deviceName should be updated.  Interface names
     * will not change.
     */
    public void storeDevice(Device d, NameUpdate nameUpdate) {
    	updateDevice(d, nameUpdate);
    }
    
    /** Updates the specified device in the database, inserting it if it doesn't exist.
     * @param d the device to be updated/inserted.
     * @nameUpdate this specifies whether the device name should be updated.  This does NOT touch
     * interface names.  If you want to update interface names, you should call updateInterfaces(device.getInterfaces(), BLAH).
     */
    public void updateDevice(Device d, NameUpdate nameUpdate) {
    	Device exists = getDevice(d.getKey());
    	
    	if(exists==null) {		// If it doesn't exist, just insert it
    		_tables.get(DevicesTable.TABLE_NAME).insert(d);
    	} else {  	
    		
    		switch(nameUpdate) {
    			case UPDATE:			// If we are updating the device name, do not do anything.
    				break;
    				
    			case DO_NOT_UPDATE:		// If not updating the name, use the one from the database
    				d.setUserName(exists.getUserName());
    				break;
    				
    			case SAFE_UPDATE:		// If playing it safe, only update it if its null
    				if(exists.getUserName()!=null)
    					d.setUserName(exists.getUserName());
    				break;
    		}
    		
    		
	    	// We never ever want to accidentally overwrite the key of a device
	    	// in the database with another key.  So, make sure to overwrite it.
	    	d.setKey(exists.getKey());
	    	_tables.get(DevicesTable.TABLE_NAME).update(d);
    	}
    	
    	// Now, we need to take care of all interfaces
    	for(Interface i : d.getInterfaces()) {
    		storeInterface(i, NameUpdate.DO_NOT_UPDATE);		// This will insert it, or update it.
    		associateInterfaceWithDevice(i._MAC, d.getKey());
    	}
    }
    
    /** Updates the database with the given set of devices.  It inserts
     * them if they do not exists, and updates them if they do.
     * @param devices the devices to insert/update
     */
    public void updateDevices(ArrayList<Device> devices, NameUpdate nameUpdate) {
    	for(Device d : devices)
    		updateDevice(d, nameUpdate);
    }
    
    
    //******** DEVICE ************ READ HELPER FUNCTIONS ****************************//    
    /** Get a device from the database who has an interface with the specified MAC.
     * @param MAC the MAC that the device must match
     * @return the Device if it exists, null otherwise
     */
    public Device getDevice(String MAC) {
    	if(MAC==null)
    		return null;

    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	for(Object o : _tables.get(InterfacesTable.TABLE_NAME).retrieveField("deviceKey", conditions, false))
    		if(o!=null)
    			return getDevice(((Integer)o));
        	
    	return null;
    }
    
    /** Gets a device based on a device key
     * @param deviceKey the deviceKey that the device must match
     * @return the device that matches the key, null otherwise
     */
    public Device getDevice(int deviceKey) {
    	return getDeviceFromKey(deviceKey);
    }
    
    /** Gets a device from the database based on the specified device key
     * @param deviceKey the matching device key
     * @return the device that matches 'deviceKey', null otherwise
     */
    private Device getDeviceFromKey(int deviceKey) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("deviceKey", deviceKey);
    	for(Object o : _tables.get(DevicesTable.TABLE_NAME).retrieve(conditions))
    		return (Device) o;
    	return null;
    }
    
    /** Gets all of the internal devices from the database
     * @return a list of the internal devices
     */
    public ArrayList<Device> getInternalDevices() {
    	ArrayList<Device> devices = new ArrayList<Device>();
    	ContentValues conditions = new ContentValues();
    	conditions.put("internal", 1);
    	for(Object o : _tables.get(DevicesTable.TABLE_NAME).retrieve(conditions))
    		devices.add((Device)o);
    	return devices;
    }
    
    /** Gets all of the external devices from the database
     * @return a list of the external devices
     */
    public ArrayList<Device> getExternalDevices() {
    	ArrayList<Device> devices = new ArrayList<Device>();
    	ContentValues conditions = new ContentValues();
    	conditions.put("internal", 0);
    	for(Object o : _tables.get(DevicesTable.TABLE_NAME).retrieve(conditions))
    		devices.add((Device)o);
    	return devices;
    }
    
    /** Pulls all internal interfaces from the database and returns them
     * @return internal interfaces
     */
    public ArrayList<Interface> getInternalInterfaces() {
    	ArrayList<Device> devices = getInternalDevices();
    	ArrayList<Interface> interfaces = new ArrayList<Interface>();
    	for(Device d : devices)
    		interfaces.addAll(d.getInterfaces());
    	return interfaces;
    }
    
    /** Pulls all external interfaces from the database and returns them
     * @return external interfaces
     */
    public ArrayList<Interface> getExternalInterfaces() {
    	ArrayList<Device> devices = getExternalDevices();
    	ArrayList<Interface> interfaces = new ArrayList<Interface>();
    	for(Device d : devices)
    		interfaces.addAll(d.getInterfaces());
    	return interfaces;
    }
        
    //****** INTERFACE *********** WRITE HELPER FUNCTIONS ****************************//
    
    /** This will insert the interface if it doesn't exist, and will update it if it does
     * exist.  Must specify whether the interface name should be updated if it does exist.
     * @param i
     * @param nameUpdate this is whether or not the name should be updated for the interface
     */
    public void storeInterface(Interface i, NameUpdate nameUpdate) {
    	updateInterface(i, nameUpdate);
    }
    
    /** Unlike storeInterface, this will only insert it if it doesn't yet exist.
     * @param i
     * @return
     */
    public boolean insertInterface(Interface i) {
    	if(getInterface(i._MAC)==null) {
    		storeInterface(i, NameUpdate.UPDATE);
    		return true;
    	}
    	return false;
    }
    
    /** This will go through the list of interfaces and only insert the interfaces that
     * are not yet in the database.  Otherwise, nothing will happen.
     * @param ifaces
     */
    public void insertInterfaces(ArrayList<Interface> ifaces) {
    	for(Interface i : ifaces)
    		insertInterface(i);
    }
    
    /**  This will update an interface in the database, or store this interface
     * if it doesn't yet exist.
     * @param i	the Interface to update or insert.
     */
    public void updateInterface(Interface i, NameUpdate nameUpdate) {
    	Interface existing = getInterface(i._MAC);
    	
    	if(existing==null) {		// If it doesn't exist, just insert it
    		_tables.get(InterfacesTable.TABLE_NAME).insert(i);
    		if(i.getClass()==WirelessInterface.class)
    			_tables.get(WirelessIfaceTable.TABLE_NAME).insert(i);
    		if(i.getClass()==WiredInterface.class)
    			_tables.get(WiredIfaceTable.TABLE_NAME).insert(i);
    		return;
    	}
    	
    	// If we get to this point, the Interface must exist, let's update it.
    	// First, we must mangle the name appropriately.
    	switch(nameUpdate) {
    		case UPDATE:		// If the case is to update it, we leave it alone and allow the overwrite
    			break;
    		
    		case DO_NOT_UPDATE:	// If we do not want to update it, we pull the name in from the database instance
    			i._ifaceName = existing._ifaceName;
    			break;
    			
    		case SAFE_UPDATE:	// Safe updates mean to save the name only if one didn't exist
    			if(existing._ifaceName != null)
    				i._ifaceName = existing._ifaceName;
    			break;    				
    	}
    	
    	// Now that we've manged names, now we can actually update the interface
    	_tables.get(InterfacesTable.TABLE_NAME).update(i);
    
    	// Now, if the interface that was passed to us is Wireless and it is stored as
    	// a raw Interface, then let's insert the wireless data
    	if(i.getClass()==WirelessInterface.class && existing.getClass()==Interface.class) {
    		_tables.get(WirelessIfaceTable.TABLE_NAME).insert(i);
    		return;
    	}
    	
    	// If the existing interface was a WiredInterface, and the one that was passed was
    	// a WirelessInterface, we upgrade it!
    	if(i.getClass()==WirelessInterface.class && existing.getClass()==WiredInterface.class) {
    		_tables.get(WirelessIfaceTable.TABLE_NAME).insert(i);
    		removeWiredData(i._MAC);
    		return;
    	}
    	
    	// If the Interface passed was wireless, and it is a wireless interface, then we also update that
    	if(i.getClass()==WirelessInterface.class && existing.getClass()==WirelessInterface.class) {
    		_tables.get(WirelessIfaceTable.TABLE_NAME).update(i);
    		return;
    	}
    	
    	// If the Interface passed was wired, and it is stored as wired, we update that
    	if(i.getClass()==WiredInterface.class && existing.getClass()==WiredInterface.class) {
    		_tables.get(WiredIfaceTable.TABLE_NAME).update(i);
    		return;
    	}
    	
    	// Note that if the interface passed was wired, and it is stored as a wireless interface, we do
    	// NOT downgrade wireless to wired.
    }
    
    /** Given a set of interfaces, update them in the data.  We play it safe with naming in this case.
     * @param interfaces
     */
    public void updateInterfaces(ArrayList<Interface> interfaces, NameUpdate nameUpdate) {
    	for(Interface iface : interfaces)
    		updateInterface(iface, nameUpdate);
    }
    
    //******* INTERFACE *************** DELETE HELPER FUNCTIONS ****************************//
    // YOU CANNOT DELETE INTERFACES!!  This could break snapshots.
    public boolean removeWiredData(String MAC) {
    	if(MAC==null)
    		return false;
    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	return _tables.get(WiredIfaceTable.TABLE_NAME).delete(conditions);	
    }
    
    //******* INTERFACE ********** READ HELPER FUNCTIONS ****************************//
    /** Given a MAC address, return an Interface, WiredInterface, or WirelessInterface
     * which matches this MAC address.
     * @param MAC the given MAC address it must match
     * @return an Interface which can be either WirelessInterface or WiredInterface, also
     */
    public Interface getInterface(String MAC) {
    	Interface iface=getRawInterface(MAC);
    	if(iface==null)
    		return null;
    	
    	// If it's a wireless interface, go ahead and return that.
    	WirelessInterface wiface = getWirelessInterface(iface._MAC);
    	if(wiface!=null)
    		return wiface;
    	
    	// If it's a wired interface, go ahead and return that
    	WiredInterface wiredface = getWiredInterface(iface._MAC);
    	if(wiredface!=null)
    		return wiredface;
    	
    	return iface;
    }
    
    /** This will return the raw superclass Interface, and will NOT return a WiredInterface
     * or a WirelessInterface.  This should really only be used internally.
     * @param MAC
     * @return
     */
    public Interface getRawInterface(String MAC) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	for(Object o : _tables.get(InterfacesTable.TABLE_NAME).retrieve(conditions)) 
    		return (Interface)o;
    	return null;
    }

    /** Given a specific interface MAC, return a wired interface *if* there is a
     * wired interface associated with this key.
     * @param MAC the interface MAC
     * @return a WiredInterface if there was one associated with this key, null otherwise.
     */
    private WiredInterface getWiredInterface(String MAC) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	for(Object o : _tables.get(WiredIfaceTable.TABLE_NAME).retrieve(conditions))
    		return (WiredInterface)o;
    	return null;
    }
    
    /** Given a specific inter MMAC, return a wireless interface *if* there
     * is a wireless interface associated with this key.
     * @param MAC the MAC of the interface
     * @return a WirelessInterface if there was one associated with this key, null
     * otherwise.
     */
    private WirelessInterface getWirelessInterface(String MAC) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	for(Object o : _tables.get(WirelessIfaceTable.TABLE_NAME).retrieve(conditions))
    		return (WirelessInterface)o;
    	return null;
    }
    
    /** Associates the interface with the given MAC address with the a device
     * labeled with deviceKey.
     * @param MAC the MAC of the interface
     * @param deviceKey the key of the device
     * @return whether we were successful or not
     */
    public boolean associateInterfaceWithDevice(String MAC, int deviceKey) {
    	ContentValues condition = new ContentValues();
    	ContentValues values = new ContentValues();
    	condition.put("MAC", MAC);
    	values.put("deviceKey", deviceKey);
    	return _tables.get(InterfacesTable.TABLE_NAME).update(values, condition);
    }
    
    /** Removes a device association from an interface.
     * @param MAC The MAC address of the interface to remove the association.
     * @return true if successful, false otherwise.
     */
    public boolean removeDeviceAssociation(String MAC) {
    	ContentValues condition = new ContentValues();
    	ContentValues values = new ContentValues();
    	condition.put("MAC", MAC);
    	values.putNull("deviceKey");
    	return _tables.get(InterfacesTable.TABLE_NAME).update(values, condition);
    }
    
    /** Given a specified set of conditions, get the the interfaces that match the
     * the conditions.
     * @param conditions the conditions the interface should match
     * @return the list of interfaces that match the conditions
     */
    public ArrayList<Interface> getInterfaces(ContentValues conditions) {
    	ArrayList<Interface> interfaces = new ArrayList<Interface>();
    	for(Object o : _tables.get(InterfacesTable.TABLE_NAME).retrieve(conditions))
    		interfaces.add(getInterface(((Interface)o)._MAC));
    	return interfaces;
    }
    
    /** Get a list of interfaces from the specified deviceKey (associated to that device).
     * @param deviceKey the device's key the interfaces should be associated to
     * @return a list of interfaces
     */
    public ArrayList<Interface> getInterfaces(int deviceKey) {
    	ContentValues condition = new ContentValues();
    	condition.put("deviceKey", deviceKey);
    	return getInterfaces(condition);
    }

    //---opens the database---
    public DBAdapter open() throws SQLException 
    {
    	debugOut("Populated the tables, now creating the DatabaseHelper");
        db = DBHelper.getWritableDatabase();
        
        return this;
    }

    //---closes the database---    
    public void close() 
    {
        DBHelper.close();
    }

    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
    	ArrayList<DBTable> _dbTables;
        DatabaseHelper(Context context, ArrayList<DBTable> tables) { 
        	super(context, DATABASE_NAME, null, DATABASE_VERSION); 
        	_dbTables=tables;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	for(DBTable table : _dbTables) { 
        		db.execSQL(table.creationString());
        		debugOut("Created database table " + table._tableName);
        	}
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion + ", which will destroy all old data");
            for(DBTable table : _dbTables)
            	db.execSQL("DROP TABLE IF EXISTS " + table._tableName);

            onCreate(db);
        }
    } 
    
	private static void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
