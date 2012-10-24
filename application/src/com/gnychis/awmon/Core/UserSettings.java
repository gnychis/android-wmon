package com.gnychis.awmon.Core;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

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
    
    public boolean haveUserSettings() { return settings.getBoolean("initialized", false); }
    public void setHaveUserSettings() { sEditor.putBoolean("initialized", true); sEditor.commit(); }
    
    public boolean haveHomeLocation() { return settings.getBoolean("haveHomeLocation", false); }
    
    public void setPhoneIsInHome(boolean val) { sEditor.putBoolean("phoneIsInHome", val); sEditor.commit(); }
    public boolean phoneIsInHome() { return settings.getBoolean("phoneIsInHome", false); }
    
    public void setHomeLocation(Location l) {
    	Log.d("AWMonSettings", "Setting Location: (" + l.getLatitude() + "," + l.getLongitude() + ")");
    	Log.d("AWMonSettings", "Setting float locations: " + (float)l.getLatitude() + "," + (float)l.getLongitude() +")");
    	sEditor.putFloat("HomeLocLong", (float)l.getLongitude());
    	sEditor.putFloat("HomeLocLat", (float)l.getLatitude());
    	sEditor.putFloat("HomeLocAccuracy", (float)l.getAccuracy());
    	sEditor.putBoolean("haveHomeLocation",true);
    	sEditor.commit();
    }
    
    public void setLastLocation(Location l) {
    	sEditor.putFloat("LastLocLong", (float)l.getLongitude());
    	sEditor.putFloat("LastLocLat", (float)l.getLatitude());
    	sEditor.putFloat("LastLocAccuracy", (float)l.getAccuracy());
    	sEditor.commit();
    }
    
    public Location getLastLocation() { 
    	Location loc = new Location("LastLocation");
    	loc.setLatitude(settings.getFloat("LastLocLat", (float)40.443181));  // Easter Egg: default value
    	loc.setLongitude(settings.getFloat("LastLocLong", (float)-79.943060));
    	loc.setAccuracy(settings.getFloat("LastLocAccuracy", 100000));
    	return loc;
    }
    
    public Location getHomeLocation() {
    	if(!settings.getBoolean("haveHomeLocation", false))
    		return null;
    	
    	Location loc = new Location("Home");
    	loc.setLatitude(settings.getFloat("HomeLocLat", (float)40.443181));  // Easter Egg: default value
    	loc.setLongitude(settings.getFloat("HomeLocLong", (float)-79.943060));
    	loc.setAccuracy(settings.getFloat("HomeLocAccuracy", 100000));
    	return loc;
    }
    
    // Higher level settings
    public int getClientID() { return settings.getInt("randClientID",-1); }
    public void setClientID(int id) { sEditor.putInt("randClientID", id); sEditor.commit(); }
    public String getHomeSSID() { return settings.getString("homeSSID", null); }
    public void setHomeSSID(String ssid) { sEditor.putString("homeSSID", ssid); sEditor.commit(); }
    
    // For some wireless settings
    public int getHomeWifiFreq() { return settings.getInt("homeWifiFreq", -1); }
    public void setHomeWifiFreq(int freq) { sEditor.putInt("homeWifiFreq", freq); sEditor.commit(); }
    
    // Survey related Settings
    public int getAgeRange() { return settings.getInt("ageRange", -1); }
    public boolean getSurveyKitchen() { return settings.getBoolean("kitchen", false); }
    public boolean getSurveyBedroom() { return settings.getBoolean("bedroom", false); }
    public boolean getSurveyLivingRoom() { return settings.getBoolean("livingRoom", false); }
    public boolean getSurveyBathroom() { return settings.getBoolean("bathroom", false); }
    public void setAgeRange(int val) { sEditor.putInt("ageRange", val); sEditor.commit(); }
    public void setSurveyKitchen(boolean val) { sEditor.putBoolean("kitchen", val); sEditor.commit(); }
    public void setSurveyBedroom(boolean val) { sEditor.putBoolean("bedroom", val); sEditor.commit(); }
    public void setSurveyLivingRoom(boolean val) { sEditor.putBoolean("livingRoom", val); sEditor.commit(); }
    public void setSurveyBathroom(boolean val) { sEditor.putBoolean("bathroom", val); sEditor.commit(); }
    public void setSurvey(int age, boolean kitchen, boolean bedroom, boolean livingRoom, boolean bathroom) {
    	sEditor.putInt("ageRange", age);
    	sEditor.putBoolean("kitchen", kitchen);		
    	sEditor.putBoolean("bedroom", bedroom);
    	sEditor.putBoolean("livingRoom", livingRoom);
    	sEditor.putBoolean("bathroom", bathroom);
    	sEditor.commit();
    }
    
}
