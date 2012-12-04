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
    		new Field("name",		String.class, 	false, false),
    		new Field("mobile",		Mobility.class, true, false),
    		new Field("deviceKey",	Integer.class, 	true, false),
    		new Field("internal",	Boolean.class,	true, false)
    		);
		
	public DevicesTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, TABLE_KEY);
	}
	
	public ArrayList<Object> resultsToObjects(Cursor cursor) {
		ArrayList<Object> devices = new ArrayList<Object>();
		
		if(cursor!=null)
			cursor.moveToFirst();
		else
			return devices;
		
		do {
			Device device = new Device();
			device.setUserName(cursor.getString(0));
			device.setMobility(Device.Mobility.values()[cursor.getInt(1)]);
			device.setKey(cursor.getInt(2));
			device.setInternal(((cursor.getInt(3)==1) ? true : false)); 
			device.addInterfaces(_dbAdapter.getInterfaces(device.getKey()));
			devices.add(device);
		} while (cursor.moveToNext());
		
		return devices;
	}

	@Override
	public ArrayList<ContentValues> getInsertContentValues(Object obj) {
		
		if(obj.getClass()!=Device.class)
			return null;
			
		Device device = (Device) obj;
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
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
    			values.put(key, (device.getInternal()) ? 1 : 0);
    	}
    	
    	list.add(values);
    	return list;
	}
}
