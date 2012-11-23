package com.gnychis.awmon.GUIs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.UserSettings;
import com.nullwire.trace.ExceptionHandler;

public class Welcome extends Activity {
	
    Spinner netlist;
	private UserSettings _settings;
	WifiManager _wifi;
	boolean _reverse_sort;
	private ProgressDialog _pd;
	private UpdateInterface _update_thread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.welcome);
	 
	  _settings = new UserSettings(this);	// Get a handle to the user settings
	  _wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	  
	  ExceptionHandler.register(this, "http://moo.cmcl.cs.cmu.edu/pastudy/"); 
	  
	  _reverse_sort=false;
      
      // Test if they have GPS enabled or disabled.  If it's disabled, we ask them to enable it to participate
      String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
      if (!provider.contains("network"))
      	buildAlertMessageNoGps();  
      
      // Spawn a thread to update the interface of the settings
      _update_thread = new UpdateInterface();
      _update_thread.execute(this);
	}
	
	// Use an AsyncTask to update the interface, because we may need to enable/disable Wifi which can
	// take a few seconds and would lock up the interface.  This keeps it moving smoothly.  This reads
	// the settings and fills in the checkbox and all drop down menus.
    protected class UpdateInterface extends AsyncTask<Context, Integer, ArrayList<String>> {
		Context parent;
		MainInterface awmon;
		
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            _pd = ProgressDialog.show(Welcome.this, "", "Retrieving Wifi networks, please wait...", true, false); 
    		Log.d("AWMonWelcome", "Getting the list of Wifi networks");
        }

		@Override
		protected ArrayList<String> doInBackground( Context ... params ) {
			ArrayList<String> spinnerArray = new ArrayList<String>();
			
			// This updates the pulldown menu with the list of networks that the user has associated to in the past.
			// The Wifi interface must be enabled to pull the list, otherwise it will come back blank.  So we briefly
			// enable it if it is disabled.
	        boolean wifi_enabled=_wifi.isWifiEnabled();
	        if(!wifi_enabled)
	        	_wifi.setWifiEnabled(true);
	        while(!_wifi.isWifiEnabled()) {}
	        
	        try { Thread.sleep(1000); } catch(Exception e) {}
	        
			// Create an instance to the Wifi manager and get a list of networks that the user
			// has associated to.  Pull this up as their list.
			List<WifiConfiguration> cfgNets = _wifi.getConfiguredNetworks();
			netlist = (Spinner) findViewById(R.id.network_list);
			if(!_reverse_sort)
				Collections.sort(cfgNets,netsort);
			else
				Collections.sort(cfgNets,netsort_reverse);
			for (WifiConfiguration config: cfgNets)
				spinnerArray.add(config.SSID.replaceAll("^\"|\"$", ""));
			spinnerArray.add("* I don't see my home network! *");
			
			// Disable the Wifi again if it was disabled before
			if(wifi_enabled==false)
				while(_wifi.isWifiEnabled())
					_wifi.setWifiEnabled(false);
			
			return spinnerArray;
		}
		
        @Override
        protected void onPostExecute(ArrayList<String> spinnerArray) {
        	
    		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(Welcome.this, android.R.layout.simple_spinner_item, spinnerArray);
    		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		netlist.setAdapter(spinnerArrayAdapter);
    		String homeSSID = _settings.getHomeSSID();
    		if(homeSSID!=null)
    			netlist.setSelection(spinnerArray.indexOf(homeSSID));
    		_pd.dismiss();
    		Log.d("AWMonWelcome", "Should have dismissed progress dialogue");
        }
		
    }
	
    // When the user clicks finished, we save some information locally.  The home network name is
    // only saved locally (so that our application can work), and it is never shared back with us.
    public void clickedFinished(View v) {
    	String home_ssid = (String) netlist.getSelectedItem();
    	
    	// If they do not see their home network, we ask them to first associate the phone to
    	// their home network.
    	if(home_ssid.equalsIgnoreCase("* I don't see my home network! *")) {
    		buildAlertMessageNoNetwork();
    		return;
    	}
    	
    	// Save their settings and set it to initialized
    	_settings.setHomeSSID(home_ssid);
    	_settings.setHaveUserSettings();
    	finish();
    }
    
    // On the press of the back button, only go back if the user has provided settings.
    // Otherwise, we block them from the main menu.
    @Override
    public void onBackPressed() {
    	if(_settings.haveUserSettings()==true)
    		finish();
    }
	
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your location services must be enabled for our study to work, do you want to enable them?\n\nIf you choose YES, you only need to enable 'Use wireless networks' and then click your back button.")
               .setCancelable(false)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                	   startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                       	dialog.cancel();
                       	finish();
                   }
               });
        final AlertDialog alert = builder.create();
        alert.show();
    }
    
    private void buildAlertMessageNoNetwork() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your phone must first be associated to your home wireless network.  Would you like to do this now?")
               .setCancelable(false)
               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                	   _reverse_sort=true;
                	   startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                   }
               })
               .setNegativeButton("No", new DialogInterface.OnClickListener() {
                   public void onClick(final DialogInterface dialog, final int id) {
                       	dialog.cancel();
                   }
               });
        final AlertDialog alert = builder.create();
        alert.show();
    }
    
    // For converting an incoming input stream to a string
    public static String convertStreamToString(InputStream is) {
  	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
  	    StringBuilder sb = new StringBuilder();
  	    String line = null;
  	    try {
  	        while ((line = reader.readLine()) != null) {
  	            sb.append(line + "\n");
  	        }
  	    } catch (IOException e) {
  	        e.printStackTrace();
  	    } finally {
  	        try {
  	            is.close();
  	        } catch (IOException e) {
  	            e.printStackTrace();
  	        }
  	    }
  	    return sb.toString();
  	}
	
    // This is a comparator to sort the networks on your phone, so that your home network is
    // more likely to be at the top of the list.
    Comparator<Object> netsort = new Comparator<Object>() {
    	public int compare(Object arg0, Object arg1) {
    		if(((WifiConfiguration)arg0).priority > ((WifiConfiguration)arg1).priority)
    			return 1;
    		else if( ((WifiConfiguration)arg0).priority < ((WifiConfiguration)arg1).priority)
    			return -1;
    		else
    			return 0;
    	}
      };
      
      Comparator<Object> netsort_reverse = new Comparator<Object>() {
      	public int compare(Object arg0, Object arg1) {
    		if(((WifiConfiguration)arg0).priority < ((WifiConfiguration)arg1).priority)
    			return 1;
    		else if( ((WifiConfiguration)arg0).priority > ((WifiConfiguration)arg1).priority)
    			return -1;
    		else
    			return 0;

      	}
      };
      
      @Override
      public void onPause() { super.onPause(); Log.d("AWMonWelcome", "onPause()"); }
      @Override
      public void onResume() { super.onResume(); 
      	Log.d("AWMonWelcome", "onResume()");
      	if((_update_thread.getStatus() == AsyncTask.Status.RUNNING) || _update_thread.getStatus() == AsyncTask.Status.PENDING)
      		return;
        _update_thread = new UpdateInterface();
        _update_thread.execute(this);
      }
}
