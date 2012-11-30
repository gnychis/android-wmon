package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

public class RSSIDataTable extends DBTable {

	public static String TABLE_NAME = "RSSI_DATA";
	private static String TABLE_KEY = "ifaceKey";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("date", 		Date.class, 	true),
    		new Field("rssi",		Integer.class,	true),
    		new Field("ifaceKey",	Long.class,		true)
    		);
		
	public RSSIDataTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, TABLE_KEY);
	}
	
	public ArrayList<Object> resultsToObjects(Cursor cursor) { return null;}
	
	@Override
	public ContentValues getInsertContentValues(Object obj) {
		
		ContentValues values = new ContentValues();

		// FIXME
		
    	return values;
	}

}
