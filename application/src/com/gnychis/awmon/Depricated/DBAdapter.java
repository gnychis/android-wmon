package com.gnychis.awmon.Core;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

// Can tell a device is mobile by locations and/or different networks its associated with
public class DBAdapter 
{

    // Setup a context and database instance
    private final Context context;
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;
    
    private static final String TAG = "DBAdapter";	// for debugging
    private static final String DATABASE_NAME = "coexisyst";
    private static final int DATABASE_VERSION = 1;
    
    // Protocol types
    public static final int PTYPE_80211 = 0;
    public static final int PTYPE_BTOOTH = 1;
    
    // Networks
    public static final String DB_TABLE_NETS = "networks";
    public static final String NETKEY_NET_ID = "id";
    public static final String NETKEY_NET_NAME = "net_name";
    public static final String NETKEY_NET_ESSID = "essid";
    public static final String NETKEY_NET_MASTER = "mac";
    public static final String NETKEY_PROTOCOL = "protocol";
    public static final String NETKEY_PROTOCOL_VER = "protocol_ver";
    private static final String DBT_CREATE_NETS =
        "create table " + DB_TABLE_NETS 
        + " (" 
        + NETKEY_NET_ID + " integer primary key autoincrement, "
        + NETKEY_NET_NAME + " varchar2, "
        + NETKEY_NET_ESSID + " varchar2 not null, "
        + NETKEY_NET_MASTER + " char(23), "
        + NETKEY_PROTOCOL + " tinyint not null, "
        + NETKEY_PROTOCOL_VER + " tinyint not null);";

	// Attempting to be generic across technologies
	// For main network/device management
	// The key is DEVKEY_MAC
    public static final String DB_TABLE_DEVICES = "devices";
    public static final String DEVKEY_NETID = "net_id";	// the network the device is a part of, allowing devices to be parts of multiple networks
    public static final String DEVKEY_MAC = "mac";	// the "host" of the network's mac, can be used to lookup protocol
    public static final String DEVKEY_NAME = "name";
    public static final String DEVKEY_PROTOCOL = "protocol";		  // devices can support a different subset of protocols than the network
    public static final String DEVKEY_PROTOCOL_VER = "protocol_ver";  // can be used as a bitmask to represent protocol versions
    private static final String DBT_CREATE_DEVICES =
        "create table " + DB_TABLE_DEVICES 
        + " (" 
        + DEVKEY_NETID + " integer not null, "
        + DEVKEY_MAC + " char(23) not null, "
        + DEVKEY_NAME + " varchar2 not null, "
        + DEVKEY_PROTOCOL + " tinyint not null, "
        + DEVKEY_PROTOCOL_VER + " tinyint not null, "
        + "CONSTRAINT dev_id PRIMARY KEY (" + DEVKEY_MAC + "," + DEVKEY_NETID + "));";
    
    // Frequency list
    public static final String DB_TABLE_FREQS = "freqs";
    public static final String FREQKEY_MAC = "mac";
    public static final String FREQKEY_FREQ = "freq";
    public static final String FREQKEY_COUNT = "count";
    private static final String DBT_CREATE_FREQS =
        "create table " + DB_TABLE_FREQS
        + " (" 
        + FREQKEY_MAC + " char(23) not null, "
        + FREQKEY_FREQ + " integer not null, "
        + FREQKEY_COUNT + " integer not null, "
        + "CONSTRAINT freq_id PRIMARY KEY (" + FREQKEY_MAC + "," + FREQKEY_FREQ + "));";
    
    // Top level spectrum table reference (viewed by device, sample taken when close to device)
    // The device table can be used with SPECKEY_DEV to lookup protocol and network
    // The key is SPECKEY_DEV and SPECKEY_DATE
    public static final String DB_TABLE_MEASUREMENTS = "measurements";
    public static final String SPEC_MEASUREMENT_DEV = "mac";
    public static final String SPEC_MEASUREMENT_DATE = "date";	// The time/date of the reading
    public static final String SPEC_MEASUREMENT_LOC = "loc";
    public static final String SPEC_MEASUREMENT_STRENGTH = "strength"; // of the device measuring at
    public static final String SPEC_MEASUREMENT_VIEWID = "viewid";  	// reference to interference table
    private static final String DBT_CREATE_MEASUREMENTS =
        "create table " + DB_TABLE_MEASUREMENTS
        + " (" 
        + SPEC_MEASUREMENT_DEV + " char(23) not null, "
        + SPEC_MEASUREMENT_DATE + " bigint not null, "
        + SPEC_MEASUREMENT_LOC + " varchar2 not null, "
        + SPEC_MEASUREMENT_STRENGTH + " integer not null, "
        + SPEC_MEASUREMENT_VIEWID + " integer not null, "			// does this need to grow to 64bit?
        + "CONSTRAINT meas_id PRIMARY KEY (" + SPEC_MEASUREMENT_DEV + "," + SPEC_MEASUREMENT_DATE + "));";

    // Devices/Networks in the spectrum at the device (the device's view across spectrum)
    // The key is SPECVIEW_INTID + SPECVIEW_MAC
    public static final String DB_TABLE_SPECVIEWS = "specviews";
    public static final String SPECVIEWS_VIEWID = "int_id";	// this can be used to look up the main device and time of measurement
    public static final String SPECVIEWS_MAC = "mac";
    public static final String SPECVIEWS_PROTOCOL = "protocol";
    public static final String SPECVIEWS_PROTOCOL_VER = "protocol_ver";
    public static final String SPECVIEWS_FREQ = "freq";
    public static final String SPECVIEWS_STRENGTH = "strength";
    private static final String DBT_CREATE_SPECVIEWS =
        "create table " + DB_TABLE_SPECVIEWS
        + " (" 
        + SPECVIEWS_VIEWID + " integer not null, "
        + SPECVIEWS_MAC + " char(23) not null, "
        + SPECVIEWS_PROTOCOL + " tinyint not null, "
        + SPECVIEWS_PROTOCOL_VER + " tinyint not null, "
        + SPECVIEWS_FREQ + " integer not null, "
        + SPECVIEWS_STRENGTH + " integer not null, "			// does this need to grow to 64bit?
        + "CONSTRAINT view_id PRIMARY KEY (" + SPECVIEWS_VIEWID + "," + SPECVIEWS_MAC + "));";

    public DBAdapter(Context ctx) 
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }
        
    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	Log.d(TAG,"Creating databases...");
        	Log.d(TAG,DB_TABLE_NETS);
        	Log.d(TAG,DBT_CREATE_DEVICES);
        	Log.d(TAG,DBT_CREATE_FREQS);
        	Log.d(TAG,DBT_CREATE_MEASUREMENTS);
        	Log.d(TAG,DBT_CREATE_SPECVIEWS);
        	
        	db.execSQL(DBT_CREATE_NETS);
            db.execSQL(DBT_CREATE_DEVICES);
            db.execSQL(DBT_CREATE_FREQS);
            db.execSQL(DBT_CREATE_MEASUREMENTS);
            db.execSQL(DBT_CREATE_SPECVIEWS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_NETS);
            db.execSQL("DROP TABLE IF EXISTS " + DBT_CREATE_DEVICES);
            db.execSQL("DROP TABLE IF EXISTS " + DBT_CREATE_FREQS);
            db.execSQL("DROP TABLE IF EXISTS " + DBT_CREATE_MEASUREMENTS);
            db.execSQL("DROP TABLE IF EXISTS " + DBT_CREATE_SPECVIEWS);
            onCreate(db);
        }
    }    
    
    //---opens the database---
    public DBAdapter open() throws SQLException 
    {
    	Log.d(TAG,"Trying to open database...");
        db = DBHelper.getWritableDatabase();
        
        return this;
    }

    //---closes the database---    
    public void close() 
    {
        DBHelper.close();
    }
    
    // Insert a network in to the management database
    public long insertNetwork(String name, String master, String essid, int protocol, int protocol_ver)
    {
    	long r=-1;
    	ContentValues initialValues = new ContentValues();
    	//initialValues.put(NETKEY_NET_ID, );  // auto-increment
    	initialValues.put(NETKEY_NET_NAME, name);
    	initialValues.put(NETKEY_NET_MASTER, master);
    	initialValues.put(NETKEY_NET_ESSID, essid);
    	initialValues.put(NETKEY_PROTOCOL, protocol);
    	initialValues.put(NETKEY_PROTOCOL_VER, protocol_ver);
    	
    	try {
    	    r = db.insertOrThrow(DB_TABLE_NETS, null, initialValues);
    	} catch (Exception e) {
    	    Log.e(TAG, "exception while adding to network database", e);
    	}
    	return r;
    }
    
    // Insert a device in to the management database
    public long insertNetDev(int netid, String mac, String dev_name, int protocol, int protocol_ver)
    {
    	long r = -1;
    	ContentValues initialValues = new ContentValues();
    	initialValues.put(DEVKEY_NETID, netid);
    	initialValues.put(DEVKEY_MAC, mac);
    	initialValues.put(DEVKEY_NAME, dev_name);
    	initialValues.put(DEVKEY_PROTOCOL, protocol);
    	initialValues.put(DEVKEY_PROTOCOL_VER, protocol_ver);
    	try {
    		r = db.insert(DB_TABLE_DEVICES, null, initialValues);
    	} catch(Exception e) {
    		Log.e(TAG,"exception while adding to device database", e);
    	}
    	return r;
    }
    
    // Check if a network is managed, get the ID if it is, otherwise return -1
    public int getNetwork(String mac, String essid)
    {
    	String qry = String.format("SELECT %s FROM %s WHERE %s='%s' and %s='%s';", 
    			NETKEY_NET_ID, DB_TABLE_NETS, NETKEY_NET_MASTER, mac, NETKEY_NET_ESSID, essid);
    	
    	Cursor res = db.rawQuery(qry,null);
        if (res != null) {
            res.moveToFirst();
        }
        
        if(res.getCount()==0)
        	return -1;
    	
    	Log.d(TAG, "Result: " + res.getString(0));
    	
    	return res.getInt(0);
    }
    
    public List<String> getAllDevices() {
    	List<String> devices = new ArrayList<String>();
    	
    	String qry = String.format("SELECT * FROM %s;", 
    			DB_TABLE_DEVICES);
    	
    	Cursor res = db.rawQuery(qry,null);
        if (res != null) {
            res.moveToFirst();
        }
        
        do {
        	devices.add(res.getString(0));
        } while (res.moveToNext());
           
        return devices;
    }
    

    
    /*
    public String getProtocolOfNet(String network) {
    	
    	String qry = String.format("SELECT %s FROM %s WHERE %s='%s';", 
    			NETKEY_PROTOCOL, DB_TABLE_NETS, NETKEY_, network);
    	
    	Cursor res = db.rawQuery(qry,null);
        if (res != null) {
            res.moveToFirst();
        }
        
        switch(res.getInt(0)) {
        case PTYPE_80211:
        	return "802.11";
        case PTYPE_BTOOTH:
        	return "Bluetooth";
        default:
        	return "Unknown";
        }
    	
    }*/
    
    public Cursor getNetworks() {
    	
	    Cursor mCursor =
	            db.query(true, DB_TABLE_NETS, new String[] {
	            		NETKEY_NET_ID,
	            		NETKEY_NET_NAME, 
	            		NETKEY_NET_ESSID,
	            		NETKEY_NET_MASTER,
	            		NETKEY_PROTOCOL,
	            		NETKEY_PROTOCOL_VER
	            		}, 
	            		null, 
	            		null,
	            		null, 
	            		null, 
	            		null, 
	            		null);
	    if (mCursor != null) {
	        mCursor.moveToFirst();
	    }
	    return mCursor;
    }
    
    public Cursor getDevicesInNet(int netid) {
    	
	    Cursor mCursor =
	            db.query(true, DB_TABLE_DEVICES, new String[] {
	            		DEVKEY_NETID,
	            		DEVKEY_MAC, 
	            		DEVKEY_NAME,
	            		DEVKEY_PROTOCOL,
	            		DEVKEY_PROTOCOL_VER
	            		}, 
	            		DEVKEY_NETID + "=" + netid, 
	            		null,
	            		null, 
	            		null, 
	            		null, 
	            		null);
	    if (mCursor != null) {
	        mCursor.moveToFirst();
	    }
	    return mCursor;
    }
    
    
    public boolean deleteDevice(String netid, String mac) {
    	boolean r = false;    	
        r = db.delete(DB_TABLE_DEVICES, DEVKEY_MAC + "='" + mac + "' and " + DEVKEY_NETID + "=" + netid, null) > 0;
        
        // Purge networks with no devices
        if(r) {
        	Cursor dev = getDevicesInNet(Integer.parseInt(netid));
        	if(dev.getCount()==0) {
        		r = db.delete(DB_TABLE_NETS, NETKEY_NET_ID + "=" + netid.toString(), null) > 0;
        	}
        }
        	
        return r;
    }
    
    /*
    //---insert a title into the database---
    public long insertTitle(String isbn, String title, String publisher) 
    {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_ISBN, isbn);
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_PUBLISHER, publisher);
        return db.insert(DATABASE_TABLE, null, initialValues);
    }

    //---deletes a particular title---
    public boolean deleteTitle(long rowId) 
    {
        return db.delete(DATABASE_TABLE, KEY_ROWID + 
        		"=" + rowId, null) > 0;
    }

    //---retrieves all the titles---
    public Cursor getAllTitles() 
    {
        return db.query(DATABASE_TABLE, new String[] {
        		KEY_ROWID, 
        		KEY_ISBN,
        		KEY_TITLE,
                KEY_PUBLISHER}, 
                null, 
                null, 
                null, 
                null, 
                null);
    }

    //---retrieves a particular title---
    public Cursor getTitle(long rowId) throws SQLException 
    {
        Cursor mCursor =
                db.query(true, DATABASE_TABLE, new String[] {
                		KEY_ROWID,
                		KEY_ISBN, 
                		KEY_TITLE,
                		KEY_PUBLISHER
                		}, 
                		KEY_ROWID + "=" + rowId, 
                		null,
                		null, 
                		null, 
                		null, 
                		null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    //---updates a title---
    public boolean updateTitle(long rowId, String isbn, 
    String title, String publisher) 
    {
        ContentValues args = new ContentValues();
        args.put(KEY_ISBN, isbn);
        args.put(KEY_TITLE, title);
        args.put(KEY_PUBLISHER, publisher);
        return db.update(DATABASE_TABLE, args, 
                         KEY_ROWID + "=" + rowId, null) > 0;
    }
    */
}