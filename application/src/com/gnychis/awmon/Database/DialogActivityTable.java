package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.gnychis.awmon.Core.DialogActivity;

public class DialogActivityTable extends DBTable {

	public static String TABLE_NAME = "DIALOG_ACTIVITY";
	private static String TABLE_KEY = "activityKey";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("date",			Date.class,		true, false),		// The date/time this information was recorded
    		new Field("name",			String.class,   true, false),		// A name for the snapshot
    		new Field("entering",		Integer.class,	true, false),
    		new Field("elapsed", 		Integer.class,	false, false),
    		new Field("activityKey", 	Integer.class,  true, true)
    		);	// "anchor" is a key from an interface
		
	public DialogActivityTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, TABLE_KEY);
	}
	
	// This function is a little bit tricky because you need to first get the unique dates,
	// then for each unique date you need to separate the results in to their respective snapshots.
	public ArrayList<Object> resultsToObjects(Cursor cursor) { 
		return null;
	}

	@Override
	public ArrayList<ContentValues> getInsertContentValues(Object obj) {
		
		if(obj.getClass()!=DialogActivity.class)
			return null;
			
		DialogActivity dialogActivity = (DialogActivity) obj;
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
		
		// Every entry that will be inserted has these few values filled out
		ContentValues header = new ContentValues();
    	for(Field field : _fields) {
    		String key = field._fieldName;
    		
    		if(field._fieldName=="date")
    			header.put(key, dialogActivity.getDate());
    		
    		if(field._fieldName=="name")
    			header.put(key, dialogActivity.getName());

    		if(field._fieldName=="entering")
    			header.put(key, (dialogActivity.getEntering()) ? 1 : 0);
    		
    		if(field._fieldName=="elapsed")
    			header.put(key, dialogActivity.getElapsed());

    	}
    	
    	list.add(header);
    	
		return list;
	}
}
