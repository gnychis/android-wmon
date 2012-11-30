package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Device.Mobility;

public class DevicesTable extends DBTable {
	
	public static String TABLE_NAME = "DEVICES";
	private static String TABLE_KEY = "deviceKey";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("name",		String.class, 	false),
    		new Field("mobile",		Mobility.class, true),
    		new Field("deviceKey",	Long.class, 	true),
    		new Field("internal",	Boolean.class,	true)
    		);
		
	public DevicesTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, TABLE_KEY);
	}
	
	public ArrayList<Object> resultsToObjects(Cursor cursor) { return null;}

	@Override
	public ContentValues getInsertContentValues(Object obj) {
		
		if(obj.getClass()!=Device.class)
			return null;
			
		Device device = (Device) obj;
		ContentValues values = new ContentValues();
    	
    	for(Field field : _fields) {
    		String key = field._fieldName;
    		
    		if(field._fieldName=="name")
    			values.put(key, device.getUserName());
    		
    		if(field._fieldName=="mobile")
    			values.put(key, device.getMobility().ordinal());
    		
    		if(field._fieldName=="deviceKey")
    			values.put(key, device.getKey());
    		
    		if(field._fieldName=="internal")
    			values.put(key, device.getInternal());
    	}
    	
    	return values;
	}
}
