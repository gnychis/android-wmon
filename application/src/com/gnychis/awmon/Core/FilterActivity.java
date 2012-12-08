package com.gnychis.awmon.Core;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import android.content.Context;

public class FilterActivity {
	
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 
	
	Date _date;
	int _totalRadios;
	int _filteredRadios;
	
	public FilterActivity(int totalRadios, int filteredRadios) {
		_date = new Date();
		_totalRadios=totalRadios;
		_filteredRadios=filteredRadios;
	}
	
	public String getDate() {
		if(_date==null)
			return null;
		return dateFormat.format(_date);
	}
	
	public int getTotalRadios() { return _totalRadios; }
	public int getFilteredRadios() { return _filteredRadios; }
	
	public void saveInDatabse(Context c) {
		FileOutputStream data_ostream;
		try {
			data_ostream = c.openFileOutput("filter_activity.json", Context.MODE_WORLD_READABLE | Context.MODE_APPEND);
		
			JSONObject json = new JSONObject();
			
			json.put("date", getDate());
			json.put("totalRadios", _totalRadios);
			json.put("filteredRadios", _filteredRadios);
			
			data_ostream.write(json.toString().getBytes());
			data_ostream.write("\n".getBytes()); 
			data_ostream.close();
		
		} catch(Exception e) {  }	
	}
}
