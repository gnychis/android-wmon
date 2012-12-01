package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.gnychis.awmon.Core.Snapshot;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

public class SnapshotsTable extends DBTable {

	public static String TABLE_NAME = "SNAPSHOTS";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("date",			Date.class,		true),		// The date/time this information was recorded
    		new Field("anchorMAC",		String.class,	false),		// Interface MAC
    		new Field("MAC",			Integer.class,	false),		// The interface that this data is for
    		new Field("RSSI",			Integer.class,	false)		// The RSSI of the interface specified by ifaceKey
    		);	// "anchor" is a key from an interface
		
	public SnapshotsTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, null);
	}
	
	// This function is a little bit tricky because you need to first get the unique dates,
	// then for each unique date you need to separate the results in to their respective snapshots.
	public ArrayList<Object> resultsToObjects(Cursor cursor) { 
		
		HashMap<Date, Snapshot> snapshotMap = new HashMap<Date,Snapshot>();
		
		if(cursor!=null)
			cursor.moveToFirst();
		else
			return new ArrayList<Object>(snapshotMap.values());
		
		do {
			Date snapshotDate;
			try { snapshotDate = dateFormat.parse(cursor.getString(0));
					} catch(Exception e) { continue; }
			
			// If there is no snapshot for this date yet, create one
			if(!snapshotMap.containsKey(snapshotDate))
				snapshotMap.put(snapshotDate, new Snapshot());
			
			Snapshot snapshot = snapshotMap.get(snapshotDate);	// Get the snapshot instance
			
			// Set the anchor to null if the MAC is null, otherwise grab the interface from the MAC
			if(cursor.isNull(1))
				snapshot.setAnchor(null);
			else
				snapshot.setAnchor(_dbAdapter.getInterface(cursor.getString(1)));
			
			// The interface that this data is for.  If it's null, you don't add an interface or RSSI values.
			if(cursor.isNull(2))
				continue;
			
			// The MAC of the interface we are working with, should not be null since tested already
			String ifaceMAC = cursor.getString(2);
			
			// If the snapshot already contains an interface instance for this interface, grab it, otherwise
			// get it from the database and add it to the snapshot.
			Interface iface;
			if(snapshot.getInterface(ifaceMAC)!=null) {
				iface = snapshot.getInterface(ifaceMAC);
			} else {
				iface = _dbAdapter.getInterface(ifaceMAC);
				snapshot.add(iface);
			}
			
			// If the snapshot row has an RSSI value, then record it.  The Interface *should* be a wireless
			// interface but we double check it just to be sure.
			if(cursor.isNull(3) || iface.getClass()!=WirelessInterface.class)
				continue;
			
			int rssiVal = cursor.getInt(3);
			((WirelessInterface)iface).addRSSI(rssiVal);
			
		} while (cursor.moveToNext());
		
		return new ArrayList<Object>(snapshotMap.values());
	}

	@Override
	public ArrayList<ContentValues> getInsertContentValues(Object obj) {
		
		if(obj.getClass()!=Snapshot.class)
			return null;
			
		Snapshot snapshot = (Snapshot) obj;
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
		
		// Every entry that will be inserted has these few values filled out
		ContentValues header = new ContentValues();
    	for(Field field : _fields) {
    		String key = field._fieldName;
    		
    		if(field._fieldName=="date") {
    			if(snapshot.getSnapshotTimeString()==null)
    				header.putNull(key);
    			else
    				header.put(key, snapshot.getSnapshotTimeString());
    		}
    		
    		if(field._fieldName=="anchorMAC") {
    			if(snapshot.getAnchor()!=null)
    				header.put(key, snapshot.getAnchor()._MAC);
    			else
    				header.putNull(key);
    		}    				
    	}
    	
    	// If there were no interfaces sensed and absolutely no devices were on (very rare), but we need
    	// to at least record the header.  The rest will be null.
    	if(snapshot.getInterfaces().size()==0) {
    		list.add(header);
    		return list;
    	}
    
		// Otherwise, if there are interfaces, we record an entry for each of them and one entry for each 
    	// RSSI value if it is a wireless interface
		for(Interface iface : snapshot.getInterfaces()) {
			
			// Regardless of what it is, you always put the MAC value
			if(iface._MAC != null)
				header.put("MAC", iface._MAC);
			else
				header.putNull("MAC");
			
			// If the interface does not exist at all in the database, need to add it.  We do NOT update interfaces here.
			if(_dbAdapter.getInterface(iface._MAC)==null)
				_dbAdapter.storeInterface(iface);
		
			ContentValues values = new ContentValues(header);	// Copy the header in to each one
			
			// It is a wireless interface, and it has RSSI values to be recorded, we record an RSSI value for each
			if((iface.getClass() == WirelessInterface.class) && ((WirelessInterface)iface).rssiValues().size()!=0) {
				for(Integer i : ((WirelessInterface)iface).rssiValues()) {
					ContentValues rssiValue = new ContentValues(values);
					rssiValue.put("RSSI", i);
					list.add(rssiValue);
				}
			} else {
				list.add(values);
			}
		}
		
		return list;
	}
}
