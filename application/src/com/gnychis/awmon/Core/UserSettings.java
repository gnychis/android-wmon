package com.gnychis.awmon.Core;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSettings {
	
	public static final String PREFS_NAME = "AWMon";
	
    private Context _context;	// To our main class
	
    // The internal settings definitions
    private SharedPreferences.Editor sEditor;
    private SharedPreferences settings;
    
    public UserSettings(Context c) {   
    	_context = c;
        settings = _context.getSharedPreferences(PREFS_NAME, 0);
        sEditor = settings.edit();
    }
    
    public boolean haveUserSettings() {
    	return settings.getBoolean("initialized", false);
    }
    
    public int getClientID() {
    	return settings.getInt("randClientID",-1);
    }
    
    public String getHomeSSID() {
    	return settings.getString("homeSSID", null);
    }
    
    public int getAgeRange() {
    	return settings.getInt("ageRange", -1);
    }
    
    public boolean getSurveyKitchen() {
    	return (settings.getInt("kitchen", 0)==0) ? false : true;
    }
    
    public boolean getSurveyBedroom() {
    	return (settings.getInt("bedroom", 0)==0) ? false : true;
    }
    
    public boolean getSurveyLivingRoom() {
    	return (settings.getInt("livingRoom",0)==0) ? false : true;
    }
    
    public boolean getSurveyBathroom() {
    	return (settings.getInt("bathroom",0)==0) ? false : true;
    }
    
}
