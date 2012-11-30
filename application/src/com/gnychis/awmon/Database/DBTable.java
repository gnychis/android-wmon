package com.gnychis.awmon.Database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;

abstract public class DBTable {
	
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
		
		public Field(String name, Class<?> type, boolean notNull) {
			_fieldName = name;
			_type = type;
			_notNull=notNull;
		}
	}
	
	abstract public ContentValues getInsertContentValues(Object obj);
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
    		
    		if(field._type == Long.class)
    			fieldString += "integer";
    		
    		if(field._type.isEnum())
    			fieldString += "integer";
    		
    		if(field._type == Boolean.class)
    			fieldString += "integer";
    		
    		if(field._type == Integer.class)
    			fieldString += "integer";
    		
    		if(field._type == Date.class)
    			fieldString += "datetime";
    		
    		fieldString += ((field._notNull) ? " not null, " : ", ");
    		create += fieldString;
    	}
    	if(_key!=null)
    		create += "CONSTRAINT " + _tableName + "_id PRIMARY KEY(" + _key + "));";
    	else
    		create += ");";
    	return create;
	}
}
