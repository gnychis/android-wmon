package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WiredInterface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

@SuppressWarnings("unchecked")
public class DBAdapter {
	
	public static final boolean VERBOSE = true;
	
    // Setup a context and database instance
    private final Context context;
    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;
    
    private static final String TAG = "DBAdapter";	// for debugging
    private static final String DATABASE_NAME = "awmon";
    private static final int DATABASE_VERSION = 1;
    
    HashMap<String,DBTable> _tables;
    
    void populateTables() {
    	_tables = new HashMap<String,DBTable>();
    	_tables.put(DevicesTable.TABLE_NAME, 		new DevicesTable(this));
    	_tables.put(InterfacesTable.TABLE_NAME, 	new InterfacesTable(this));
    	_tables.put(RSSIDataTable.TABLE_NAME,		new RSSIDataTable(this));
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
    
    //**************************** WRITE HELPER FUNCTIONS ****************************//
    public boolean storeInterface(Interface i) {
    	return insert(InterfacesTable.TABLE_NAME, i);
    }
    
    public boolean storeWirelessInterface(WirelessInterface wi) {
    	if(!insert(WirelessIfaceTable.TABLE_NAME, wi))
    		return false;
    	return storeInterface(wi);    	
    }
    
    public boolean storeWiredInterface(WiredInterface wi) {
    	if(!insert(WiredIfaceTable.TABLE_NAME, wi))
    		return false;
    	return storeInterface(wi);
    }
    
    //**************************** READ HELPER FUNCTIONS ****************************//
    public Interface getInterfaceFromMAC(String MAC) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("MAC", MAC);
    	for(Object o : retrieve(InterfacesTable.TABLE_NAME, conditions))
    		return (Interface)o;
    	return null;
    }
    
    public Interface getInterfaceFromKey(long key) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("ifaceKey", key);
    	for(Object o : retrieve(InterfacesTable.TABLE_NAME, conditions))
    		return (Interface)o;
    	return null;
    }
    
    public WiredInterface getWiredInterfaceFromMAC(String MAC) {
    	Interface iface = getInterfaceFromMAC(MAC);
    	if(iface==null)
    		return null;

    	return getWiredInterfaceFromKey(iface.getKey());
    }
    
    public WiredInterface getWiredInterfaceFromKey(long key) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("ifaceKey", key);
    	for(Object o : retrieve(WiredIfaceTable.TABLE_NAME, conditions))
    		return (WiredInterface)o;
    	return null;
    }
    
    public WirelessInterface getWirelessInterfaceFromKey(long key) {
    	ContentValues conditions = new ContentValues();
    	conditions.put("ifaceKey", key);
    	for(Object o : retrieve(WirelessIfaceTable.TABLE_NAME, conditions))
    		return (WirelessInterface)o;
    	return null;
    }
    
    public WirelessInterface getWirelessInterfaceFromMAC(String MAC) {
    	Interface iface = getInterfaceFromMAC(MAC);
    	if(iface==null)
    		return null;

    	return getWirelessInterfaceFromKey(iface.getKey());
    }
    
    public ArrayList<Object> retrieve(String tableName, ContentValues conditions) {
    	DBTable table = _tables.get(tableName);
    	
    	String qry = "SELECT * FROM " + table._tableName;
    	
    	if(conditions!=null) {
    		
    		qry += " WHERE ";
    		Set<Entry<String, Object>> s=conditions.valueSet();
    		Iterator itr = s.iterator();
    		while(itr.hasNext()) {
    			Map.Entry me = (Map.Entry)itr.next(); 
    			String key = me.getKey().toString();
    			Object value =  me.getValue();
    			qry += key + "='" + value.toString() + "'";
    			if(itr.hasNext())
    				qry += " and ";
    		}
    	}

    	qry += ";";
    	
    	Cursor res = db.rawQuery(qry,null);
    	
    	if(res==null)
    		return null;
    	
        if(!res.moveToFirst())
        	return null;
        
        return table.resultsToObjects(res);
    }
    
    public boolean insert(String tableName, Object o) {
    	DBTable table = _tables.get(tableName);
    	ContentValues values = table.getInsertContentValues(o);
    	long r=0;
    	try {
    	    r = db.insertOrThrow(tableName, null, values);
    	} catch (Exception e) {
    	    Log.e(TAG, "exception while adding to " + tableName, e);
    	    return false;
    	}
    	return ((r==-1) ? false : true);
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
