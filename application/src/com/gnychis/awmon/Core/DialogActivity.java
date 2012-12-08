package com.gnychis.awmon.Core;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;

import com.gnychis.awmon.Database.DBAdapter;

public class DialogActivity {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 
	
	String _name;
	boolean _entering;
	Date _date;
	
	public DialogActivity(String name, boolean entering) {
		_name = name;
		_entering = entering;
		_date = new Date();
	}
	
	public String getDate() {
		if(_date==null)
			return null;
		return dateFormat.format(_date);
	}
	
	public String getName() { return _name; }
	public boolean getEntering() { return _entering; }
	
	public void saveInDatabse(Context c) {
		DBAdapter dbAdapter = new DBAdapter(c);
		dbAdapter.open();
		dbAdapter.storeDialogActivity(this);
		dbAdapter.close();
		
	}
}
