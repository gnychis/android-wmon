package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.gnychis.awmon.Core.Snapshot;

public class SnapshotsTable extends DBTable {

	public static String TABLE_NAME = "SNAPSHOTS";
	private static String TABLE_KEY = "snapshotKey";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("date",			Date.class,		true, false),		// The date/time this information was recorded
    		new Field("name",			String.class,   false, false),		// A name for the snapshot
    		new Field("anchorMAC",		String.class,	false, false),		// Interface MAC
    		new Field("snapshotKey",	Integer.class,  true, false)		// Auto increment
    		);	// "anchor" is a key from an interface
		
	public SnapshotsTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, TABLE_KEY);
	}
	
	// This function is a little bit tricky because you need to first get the unique dates,
	// then for each unique date you need to separate the results in to their respective snapshots.
	public ArrayList<Object> resultsToObjects(Cursor cursor) { 
		
		ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
				
		if(cursor!=null)
			cursor.moveToFirst();
		else
			return new ArrayList<Object>(snapshots);
		
		do {
			Date snapshotDate;
			try { snapshotDate = dateFormat.parse(cursor.getString(0));
					} catch(Exception e) { continue; }

			Snapshot snapshot = new Snapshot(snapshotDate);
			snapshot.setSnapshotKey(cursor.getInt(3));
			
			// Get the name of the snapshot
			if(cursor.isNull(1))
				snapshot.setName(null);
			else
				snapshot.setName(cursor.getString(1));
			
			// Set the anchor to null if the MAC is null, otherwise grab the interface from the MAC
			if(cursor.isNull(2))
				snapshot.forceAnchor((String)null);
			else
				snapshot.forceAnchor(cursor.getString(2));
			
			snapshots.add(snapshot);
		} while (cursor.moveToNext());
		
		return new ArrayList<Object>(snapshots);
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
    		
    		if(field._fieldName=="name") {
    			if(snapshot.getName()!=null)
    				header.put(key, snapshot.getName());
    			else
    				header.putNull(key);
    		}
    	}
    	
    	list.add(header);
    	
		return list;
	}
}
