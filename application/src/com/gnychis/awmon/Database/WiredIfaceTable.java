package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WiredInterface;

public class WiredIfaceTable extends DBTable {

	public static String TABLE_NAME = "WIRED_IFACE_DATA";
	private static String TABLE_KEY = "MAC";
	
	static List<Field> FIELDS = Arrays.asList(
    		new Field("gateway", 	Boolean.class,	false),
    		new Field("MAC", 		String.class,	true)
    		);
		
	public WiredIfaceTable(DBAdapter dba) {
		super(dba, TABLE_NAME, FIELDS, TABLE_KEY);
	}

	public ArrayList<Object> resultsToObjects(Cursor cursor) {
		ArrayList<Object> interfaces = new ArrayList<Object>();
		
		if(cursor!=null)
			cursor.moveToFirst();
		else
			return interfaces;
		
		do {
			String MAC = cursor.getString(1);
			Interface iface = _dbAdapter.getRawInterface(MAC);
			WiredInterface wiface = new WiredInterface(iface);
			wiface._gateway = 	cursor.getInt(0)>0;
			interfaces.add(wiface);
		} while (cursor.moveToNext());
		
		return interfaces;
	}
	
	@Override
	public ArrayList<ContentValues> getInsertContentValues(Object obj) {
		
		if(obj.getClass()!=WiredInterface.class)
			return null;
			
		WiredInterface iface = (WiredInterface) obj;
		ArrayList<ContentValues> list = new ArrayList<ContentValues>();
		ContentValues values = new ContentValues();
    	
    	for(Field field : _fields) {
    		String key = field._fieldName;
    		
    		if(field._fieldName=="gateway")
    			values.put(key, (iface.isGateway()) ? 1 : 0);
    		
    		if(field._fieldName=="MAC")
    			values.put(key, iface._MAC);
    	}
    	list.add(values);
    	return list;
	}

}
