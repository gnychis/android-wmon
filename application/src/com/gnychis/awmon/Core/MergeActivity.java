package com.gnychis.awmon.Core;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import android.content.Context;

public class MergeActivity {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 
	
	Date _date;
	String _name;
	int _connected;
	
	public MergeActivity(String name, int connected) {
		_name = name;
		_connected=connected;
		_date = new Date();
	}
	
	public int getConnected() {
		return _connected;
	}
	
	public String getDate() {
		if(_date==null)
			return null;
		return dateFormat.format(_date);
	}
	
	public String getName() { return _name; }
	
}
