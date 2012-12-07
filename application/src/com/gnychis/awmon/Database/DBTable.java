package com.gnychis.awmon.Database;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

abstract public class DBTable {
	
	private static final String TAG = "DBTable";
	private static final boolean VERBOSE = true;
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
	
	public String _tableName;
	public ArrayList<Field> _fields;
	public String _key;
	public DBAdapter _dbAdapter;
	
	public DBTable(DBAdapter dba, String name, List<Field> fields, String key) {
		_dbAdapter = dba;
		_tableName = name;
		_fields = new ArrayList<Field>(fields);
		_key = key;
	}

	public static class Field {
		public String _fieldName;
		public Class<?> _type;
		public boolean _notNull;
		public boolean _autoIncrement;
		
		public Field(String name, Class<?> type, boolean notNull, boolean autoinc) {
			_fieldName = name;
			_type = type;
			_notNull=notNull;
			_autoIncrement=autoinc;
		}
	}
	
	abstract public ArrayList<ContentValues> getInsertContentValues(Object obj);
	abstract public ArrayList<Object> resultsToObjects(Cursor cursor);
	
	/** Gets a string to pass to the database to create the table.
	 * @return the creation string
	 */
	public String creationString() {
    	String create = "create table " + _tableName;
    	
    	create += "(";
    	for(Field field : _fields) {
    		
    		String fieldString = field._fieldName + " "; 
    		
    		if(field._type == String.class)
    			fieldString += "varchar2";
    		
    		if(field._type.isEnum())
    			fieldString += "integer";
    		
    		if(field._type == Boolean.class)
    			fieldString += "integer";
    		
    		if(field._type == Integer.class)
    			fieldString += "integer";
    		
    		if(field._type == Date.class)
    			fieldString += "datetime";
    		
    		if(field._autoIncrement)
    			fieldString += " not null AUTO_INCREMENT, ";
    		else
    			fieldString += ((field._notNull) ? " not null, " : ", ");
    		
    		create += fieldString;
    	}
    	if(_key!=null) {
    		create += "CONSTRAINT " + _tableName + "_id PRIMARY KEY(" + _key + "));";
    	} else {
    		create = create.subSequence(0, create.length()-2) + ");";
    	}
    	debugOut("Creation String: " + create);
    	return create;
	}
	
	public boolean delete(ContentValues conditions) {
		
		if(conditions==null)
			return false;
		
		if(conditions.size()==0)
			return false;
		
    	DBTable table = this;
    	
    	String qry = "DELETE FROM " + table._tableName;
    	
    	if(conditions!=null && conditions.size()>0) {
    		
    		qry += " WHERE ";
    		Set<Entry<String, Object>> s=conditions.valueSet();
    		Iterator itr = s.iterator();
    		while(itr.hasNext()) {
    			Map.Entry me = (Map.Entry)itr.next(); 
    			String key = me.getKey().toString();
    			Object value =  me.getValue();
    			qry += key + "='" + value.toString().replace("'", "''") + "'";
    			if(itr.hasNext())
    				qry += " and ";
    		}
    	}

    	qry += ";";
    	
    	debugOut("Running delete query: " + qry);
    	Cursor cu = _dbAdapter.db.rawQuery(qry,null);
    	cu.moveToFirst();
    	cu.close();
    	
        return true;
	}
	
    public boolean update(ContentValues values, ContentValues condition) {
    	if(values==null)
    		return false;
    	
    	String qry = "UPDATE " + _tableName + " SET ";
    	
		Set<Entry<String, Object>> s=values.valueSet();
		Iterator itr = s.iterator();
		while(itr.hasNext()) {
			Map.Entry me = (Map.Entry)itr.next(); 
			String key = me.getKey().toString();
			Object value =  me.getValue();
			qry += key + "=" + ((value==null) ? "NULL" : "'" + value.toString().replace("'", "''") + "'");
			if(itr.hasNext())
				qry += ", ";
		}
		
    	if(condition!=null && condition.size()>0) {
    		
    		qry += " WHERE ";
    		s=condition.valueSet();
    		itr = s.iterator();
    		while(itr.hasNext()) {
    			Map.Entry me = (Map.Entry)itr.next(); 
    			String key = me.getKey().toString();
    			Object value =  me.getValue();
    			qry += key + "='" + value.toString().replace("'", "''") + "'";
    			if(itr.hasNext())
    				qry += " and ";
    		}
    	}
    	
    	qry += ";";
    	
    	Cursor cu = _dbAdapter.db.rawQuery(qry,null);
    	cu.moveToFirst();
    	cu.close();
    	
    	return true;
    }
    
    public ArrayList<Object> retrieveField(String fieldName, ContentValues conditions, boolean unique) {
    	
    	Field field=null;
    	
    	for(Field f : _fields)
    		if(f._fieldName==fieldName)
    			field=f;
    	
    	if(field==null)
    		return new ArrayList<Object>();
    	
    	DBTable table = this;
    	
    	String qry = "SELECT " + ((unique) ? "DISTINCT " : "" ) + fieldName + " FROM " + table._tableName;
    	
    	if(conditions!=null && conditions.size()>0) {
    		
    		qry += " WHERE ";
    		Set<Entry<String, Object>> s=conditions.valueSet();
    		Iterator itr = s.iterator();
    		while(itr.hasNext()) {
    			Map.Entry me = (Map.Entry)itr.next(); 
    			String key = me.getKey().toString();
    			Object value =  me.getValue();
    			qry += key + "='" + value.toString().replace("'", "''") + "'";
    			if(itr.hasNext())
    				qry += " and ";
    		}
    	}
    	
    	qry += ";";
    	
    	Cursor res = _dbAdapter.db.rawQuery(qry,null);
    	
    	if(res==null)
    		return new ArrayList<Object>();
    	
        if(!res.moveToFirst())
        	return new ArrayList<Object>();
        
        ArrayList<Object> objects = new ArrayList<Object>();
		do {
			objects.add(fieldToObject(res, 0));
		} while (res.moveToNext());
		res.close();
		return objects;
    }
    
    public Object fieldToObject(Cursor cursor, int index) {
    	Object o=null;
    	
    	switch(cursor.getType(index)) {
    		case Cursor.FIELD_TYPE_INTEGER:
    			o = cursor.getInt(index);
    			break;
    		
    		case Cursor.FIELD_TYPE_STRING:
    			o = cursor.getString(index);
    			break;    			
    	}
    	
    	return o;
    }
    
    public ArrayList<Object> retrieve(ContentValues conditions) {
    	DBTable table = this;
    	
    	String qry = "SELECT * FROM " + table._tableName;
    	
    	if(conditions!=null && conditions.size()>0) {
    		
    		qry += " WHERE ";
    		Set<Entry<String, Object>> s=conditions.valueSet();
    		Iterator itr = s.iterator();
    		while(itr.hasNext()) {
    			Map.Entry me = (Map.Entry)itr.next(); 
    			String key = me.getKey().toString();
    			Object value =  me.getValue();
    			qry += key + "='" + value.toString().replace("'", "''") + "'";
    			if(itr.hasNext())
    				qry += " and ";
    		}
    	}

    	qry += ";";
    	
    	Cursor res = _dbAdapter.db.rawQuery(qry,null);
    	
    	if(res==null)
    		return new ArrayList<Object>();
    	
        if(!res.moveToFirst())
        	return new ArrayList<Object>();
        ArrayList<Object> results = table.resultsToObjects(res);
        res.close();
        return results;
    }
    
    public boolean update(Object o) {
    	DBTable table = this;
    	ArrayList<ContentValues> insertions = table.getInsertContentValues(o);
    	for(ContentValues values : insertions) {
	    	ContentValues condition = new ContentValues(values);
	    	
	    	// I wish there was a better way to do this...
	    	for(String vkey : values.keySet())
	    		if(vkey!=table._key)
	    			condition.remove(vkey);
	    	
	    	update(values, condition);
    	}
    	return true;
    }
    
    public boolean update(Object o, List<String> ignores) {
    	DBTable table = this;
    	ArrayList<ContentValues> insertions = table.getInsertContentValues(o);
    	for(ContentValues values : insertions) {
	    	ContentValues condition = new ContentValues(values);
	    	
	    	// I wish there was a better way to do this... this gets the key
	    	for(String vkey : values.keySet())
	    		if(vkey!=table._key)
	    			condition.remove(vkey);
	    	
	    	for(String ignore : ignores)
	    		values.remove(ignore);
	    	
	    	update(values, condition);
    	}
    	return true;
    }
        
    public boolean insert(Object o) {
    	DBTable table = this;
    	ArrayList<ContentValues> insertions = table.getInsertContentValues(o);
    	_dbAdapter.db.beginTransaction();
    	for(ContentValues values : insertions) {
	    	try {
	    	    _dbAdapter.db.insertOrThrow(_tableName, null, values);
	    	} catch (Exception e) {
	    	    Log.e(TAG, "exception while adding to " + _tableName, e);
	    	    return false;
	    	}
    	}
    	_dbAdapter.db.setTransactionSuccessful();
    	_dbAdapter.db.endTransaction();
    	return true;
    }
    
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
