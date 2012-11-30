package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

public class SnapshotsTable extends DBTable {

	public static String TABLE_NAME = "SNAPSHOTS";
	private static String TABLE_KEY = "date";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("date",		Date.class,		true),
    		new Field("anchor",		String.class,	true)
    		);	// "anchor" is a key from an interface
		
	public SnapshotsTable(DBAdapter dba) {
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
