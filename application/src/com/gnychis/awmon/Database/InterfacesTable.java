package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;

public class InterfacesTable extends DBTable {

	public static String TABLE_NAME = "INTERFACES";
	private static String TABLE_KEY = "MAC";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("MAC", 		String.class, 	true, false),
    		new Field("IP", 		String.class, 	false, false),
    		new Field("ouiName", 	String.class, 	false, false),
    		new Field("ifaceName", 	String.class, 	false, false),
    		new Field("type", 		String.class, 	false, false),
    		new Field("deviceKey",	Integer.class,	false, false)
    		);
		
	public InterfacesTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, TABLE_KEY);
	}
	
	public ArrayList<Object> resultsToObjects(Cursor cursor) {
		ArrayList<Object> interfaces = new ArrayList<Object>();
		
		if(cursor!=null)
			cursor.moveToFirst();
		else
			return interfaces;
		
		do {
			Interface iface = new Interface();
			iface._MAC = 		cursor.getString(0);
			iface._IP = 		cursor.getString(1);
			iface._ouiName = 	cursor.getString(2);
			iface._ifaceName = 	cursor.getString(3);
			try {
				String type =   cursor.getString(4);
				if(type!=null)
					iface._type =		Class.forName(type);
			} catch(Exception e) { Log.e("DATABASE", "Error getting the Interface class", e); }
			
			interfaces.add(iface);
		} while (cursor.moveToNext());
		
		return interfaces;
	}
	
	@Override
	public ArrayList<ContentValues> getInsertContentValues(Object obj) {
		
		if(obj.getClass()!=Interface.class && obj.getClass().getSuperclass()!=Interface.class)
			return null;
					
		Interface iface = (Interface) obj;
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
		ContentValues values = new ContentValues();
    	
    	for(Field field : _fields) {
    		String key = field._fieldName;
    		
    		if(field._fieldName=="MAC")
    			values.put(key, iface._MAC);
    		
    		if(field._fieldName=="IP")
    			values.put(key, iface._IP);
    		
    		if(field._fieldName=="ouiName")
    			values.put(key, iface._ouiName);
    		
    		if(field._fieldName=="ifaceName")
    			values.put(key, iface._ifaceName);
    		
    		if(field._fieldName=="type" && iface._type!=null)
    			values.put(key, iface._type.getName());
    	}
    	list.add(values);
    	return list;
	}
}
