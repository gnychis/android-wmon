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
    private static final int DATABASE_VERSION = 1;
    
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
    	_tables.get(SnapshotsTable.TABLE_NAME).insert(s);
    }
    
    //******* SNAPSHOT *************** READ HELPER FUNCTIONS ****************************//
    /** Get all snapshots from the database
     * @return an ArrayList of snapshots from the database, no restrictions.
     */
    public ArrayList<Snapshot> getSnapshots() {
    	ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
    	for(Object o : _tables.get(SnapshotsTable.TABLE_NAME).retrieve(null))
    		snapshots.add((Snapshot)o);
    	return snapshots;
    }
    
    /** Return a snapshot with the specified date.
     * @param d the date of the snapshot
     * @return the specified snapshot if it exists, null otherwise
     */
    public Snapshot getSnapshot(Date d) {
    	if(d==null)
    		return null;
    	ContentValues conditions = new ContentValues();
    	conditions.put("date", DBTable.dateFormat.format(d));
    	for(Object o : _tables.get(SnapshotsTable.TABLE_NAME).retrieve(conditions))
    		return (Snapshot) o;
    	return null;
    }
         
    //******* DEVICE *************** WRITE HELPER FUNCTIONS ****************************//
    /** Stores the given device in the database, this will either insert or update it.
     * @param d the Device to be inserted/updated.
     */
    public void storeDevice(Device d) {
    	updateDevice(d);
    }
    
    /** Updates the specified device in the database, inserting it if it doesn't exist.
     * @param d the device to be updated/inserted.
     */
    public void updateDevice(Device d) {
    	Device exists = getDevice(d.getKey());
    	
    	if(exists==null) {		// If it doesn't exist, just insert it
    		_tables.get(DevicesTable.TABLE_NAME).insert(d);
    	} else {  	
	    	// We never ever want to accidentally overwrite the key of a device
	    	// in the database with another key.  So, make sure to overwrite it.
	    	d.setKey(exists.getKey());
	    	_tables.get(DevicesTable.TABLE_NAME).update(d);
    	}
    	
    	// Now, we need to take care of all interfaces
    	for(Interface i : d.getInterfaces()) {
    		storeInterface(i);	// This will insert it, or update it.
    		associateInterfaceWithDevice(i._MAC, d.getKey());
    	}
    }
    
    /** Updates the database with the given set of devices.  It inserts
     * them if they do not exists, and updates them if they do.
     * @param devices the devices to insert/update
     */
    public void updateDevices(ArrayList<Device> devices) {
    	for(Device d : devices)
    		updateDevice(d);
    }
    
    
    //******** DEVICE ************ READ HELPER FUNCTIONS ****************************//
    /** Gets a deviceKey from the database for the device that contains an interface
     * with the specified MAC
     * @param MAC the MAC that an interface in the device must match
     * @return the deviceKey
     */
    private int getDeviceKeyFromMAC(String MAC) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	for(Object o : _tables.get(InterfacesTable.TABLE_NAME).retrieveField("deviceKey", conditions))
    		return ((Integer)o);
    	throw new SQLException("Could not find deviceKey");
    }
    
    /** Get a device from the database who has an interface with the specified MAC.
     * @param MAC the MAC that the device must match
     * @return the Device if it exists, null otherwise
     */
    public Device getDevice(String MAC) {
    	int deviceKey;
    	// First, get the interface key
    	try {
    		deviceKey = getDeviceKeyFromMAC(MAC);
    	} catch(Exception e) {
    		Log.e(TAG, "Could not get interface key", e);
    		return null;
    	}
    	return getDevice(deviceKey);
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
    
    public void storeInterface(Interface i) {
    	updateInterface(i);
    }
    
    /**  This will update an interface in the database, or store this interface
     * if it doesn't yet exist.
     * @param i	the Interface to update or insert.
     */
    public void updateInterface(Interface i) {
    	Interface exists = getInterface(i._MAC);
    	
    	if(exists==null) {		// If it doesn't exist, just insert it
    		_tables.get(InterfacesTable.TABLE_NAME).insert(i);
    		if(i.getClass()==WirelessInterface.class)
    			_tables.get(WirelessIfaceTable.TABLE_NAME).insert(i);
    		if(i.getClass()==WiredInterface.class)
    			_tables.get(WiredIfaceTable.TABLE_NAME).insert(i);
    		return;
    	}
    	
    	// We never ever want to accidentally overwrite the key of an interface
    	// in the database with another key.  So, make sure to overwrite it.
    	i.setKey(exists.getKey());
    	_tables.get(InterfacesTable.TABLE_NAME).update(i);
    	
    	if(i.getClass()==WirelessInterface.class)
    		_tables.get(WirelessIfaceTable.TABLE_NAME).update(i);
    	if(i.getClass()==WiredInterface.class)
    		_tables.get(WiredIfaceTable.TABLE_NAME).update(i);
    }
    
    /** Given a set of interfaces, update them in the data.
     * @param interfaces
     */
    public void updateInterfaces(ArrayList<Interface> interfaces) {
    	for(Interface iface : interfaces)
    		updateInterface(iface);
    }
    
    //******* INTERFACE ********** READ HELPER FUNCTIONS ****************************//
    /** This is mainly a helper function which allows you to get an ifaceKey given a MAC
     * address.
     * @param MAC the MAC address the interface must match
     * @return the ifaceKey of the given interface with MAC address
     */
    private int getInterfaceKeyFromMAC(String MAC) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	for(Object o : _tables.get(InterfacesTable.TABLE_NAME).retrieve(conditions))
    		return ((Interface)o).getKey();
    	throw new SQLException("Could not find ifaceKey");
    }
    
    /** Given a MAC address, return an Interface, WiredInterface, or WirelessInterface
     * which matches this MAC address.
     * @param MAC the given MAC address it must match
     * @return an Interface which can be either WirelessInterface or WiredInterface, also
     */
    public Interface getInterface(String MAC) {
    	int ifaceKey;
    	// First, get the interface key
    	try {
    		ifaceKey = getInterfaceKeyFromMAC(MAC);
    	} catch(Exception e) {
    		Log.e(TAG, "Could not get interface key", e);
    		return null;
    	}
    	return getInterface(ifaceKey);
    }
    
    /** Given an interface key, return an Interface which will actually be a
     * WirelessInterface, WiredInterface, or Interface depending on its type. This
     * is the most top-level and rich function.
     * @param ifaceKey must match this key
     * @return a WirelessInterface, WiredInterface, Interface or null.
     */
    public Interface getInterface(int ifaceKey) {
    	// If it's a wireless interface, go ahead and return that.
    	WirelessInterface wiface = getWirelessInterfaceFromKey(ifaceKey);
    	if(wiface!=null)
    		return wiface;
    	
    	// If it's a wired interface, go ahead and return that
    	WiredInterface wiredface = getWiredInterfaceFromKey(ifaceKey);
    	if(wiredface!=null)
    		return wiredface;
    	
    	return getInterfaceFromKey(ifaceKey);
    }
    
    /** Gets the parent Interface class representation of an interface with the
     * specified key.
     * @param ifaceKey the key the interface must match
     * @return an Interface if one existed with the associated ifaceKey
     */
    public Interface getInterfaceFromKey(int ifaceKey) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("ifaceKey", ifaceKey);
    	for(Object o : _tables.get(InterfacesTable.TABLE_NAME).retrieve(conditions))
    		return (Interface)o;
    	return null;
    }

    /** Given a specific interface key, return a wired interface *if* there is a
     * wired interface associated with this key.
     * @param ifaceKey the interface key
     * @return a WiredInterface if there was one associated with this key, null otherwise.
     */
    private WiredInterface getWiredInterfaceFromKey(int ifaceKey) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("ifaceKey", ifaceKey);
    	for(Object o : _tables.get(WiredIfaceTable.TABLE_NAME).retrieve(conditions))
    		return (WiredInterface)o;
    	return null;
    }
    
    /** Given a specific interface key, return a wireless interface *if* there
     * is a wireless interface associated with this key.
     * @param ifaceKey the interface key
     * @return a WirelessInterface if there was one associated with this key, null
     * otherwise.
     */
    private WirelessInterface getWirelessInterfaceFromKey(int ifaceKey) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("ifaceKey", ifaceKey);
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
    		interfaces.add(getInterface(((Interface)o).getKey()));
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
