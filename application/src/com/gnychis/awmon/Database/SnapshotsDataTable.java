package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.gnychis.awmon.Core.Snapshot;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

public class SnapshotsDataTable extends DBTable {

	public static String TABLE_NAME = "SNAPSHOT_DATA";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("snapshotKey",	Integer.class,  true, false),		// Snapshot key
    		new Field("MAC",			String.class,	false, false),		// The interface that this data is for
    		new Field("RSSI",			Integer.class,	false, false)		// The RSSI of the interface specified by ifaceKey
    		);	// "anchor" is a key from an interface
		
	public SnapshotsDataTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, null);
	}
	
	// This function is a little bit tricky because you need to first get the unique dates,
	// then for each unique date you need to separate the results in to their respective snapshots.
	public ArrayList<Object> resultsToObjects(Cursor cursor) { 
				
		Snapshot snapshot = new Snapshot();
		
		if(cursor!=null)
			cursor.moveToFirst();
		else
			return new ArrayList<Object>(snapshot.getInterfaces());
		
		do {
			
			// The interface that this data is for.  If it's null, you don't add an interface or RSSI values.
			if(cursor.isNull(1))
				continue;
			
			// The MAC of the interface we are working with, should not be null since tested already
			String ifaceMAC = cursor.getString(1);
			
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
			if(cursor.isNull(2) || iface.getClass()!=WirelessInterface.class)
				continue;
			
			int rssiVal = cursor.getInt(2);
			((WirelessInterface)iface).addRSSI(rssiVal);
			
		} while (cursor.moveToNext());
		
		return new ArrayList<Object>(snapshot.getInterfaces());
	}

	@Override
	public ArrayList<ContentValues> getInsertContentValues(Object obj) {
		
		if(obj.getClass()!=Snapshot.class)
			return null;
			
		Snapshot snapshot = (Snapshot) obj;
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
		
		// This stores any interfaces that do not yet exist in the database.  It will NOT update interfaces that already exist.
		_dbAdapter.insertInterfaces(snapshot.getInterfaces());
		
		// If there are interfaces, we record an entry for each of them and one entry for each 
    	// RSSI value if it is a wireless interface
		for(Interface iface : snapshot.getInterfaces()) {
			
			ContentValues values = new ContentValues();	// Copy the header in to each one
			
			values.put("snapshotKey", snapshot.getSnapshotKey());
			
			// Regardless of what it is, you always put the MAC value
			if(iface._MAC != null)
				values.put("MAC", iface._MAC);
			else
				values.putNull("MAC");
		
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
