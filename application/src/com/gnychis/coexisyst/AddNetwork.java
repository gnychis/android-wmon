package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;


public class AddNetwork extends ExpandableListActivity {

	private static final String TAG = "ManageDevices";
    private static final String NAME = "NAME";
    private static final String MAC = "MAC";
    private static final String NETID = "NETID";
    private static final String CMAC = "CMAC";
    //private static final String PROTOCOL = "PROTOCOL";
    private static final String DESCRIPTION = "DESCRIPTION";
    private static final String RSSI = "RSSI";
    private static final String FREQ = "FREQ";
    
    ArrayList<WifiAP> netlist_80211;

    public List<Map<String, String>> groupData;
    public List<List<Map<String, String>>> childData;
    
    DBAdapter db;
    
    public int last_group;
    public int last_child;
    
    private ExpandableListAdapter mAdapter;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  Bundle i = getIntent().getExtras();
	  netlist_80211 = (ArrayList<WifiAP>)i.get("com.gnychis.coexisyst.80211");
	  db = new DBAdapter(this);
	  db.open();
	  setup_groups();
	  
	}
	
	public void setup_groups() {
		
        groupData = new ArrayList<Map<String, String>>();
        childData = new ArrayList<List<Map<String, String>>>();
        
        //////////////// 802.11 Networks
        Map<String, String> curGroupMap = new HashMap<String, String>();
        groupData.add(curGroupMap);
        curGroupMap.put(NAME, "WiFi");  
        curGroupMap.put(DESCRIPTION, "Description: 802.11 networks");
        
        List<Map<String, String>> children = new ArrayList<Map<String, String>>();
		for(WifiAP result : netlist_80211) {
            Map<String, String> curChildMap = new HashMap<String, String>();
            children.add(curChildMap);
            curChildMap.put(NAME, result._ssid);
            
            if(!result._dualband)
            	curChildMap.put(MAC, "MAC: " + result._mac);
            else
            	curChildMap.put(MAC, "MAC: " + result._mac + " / " + result._mac2);
            
            curChildMap.put(RSSI, "RSSI: " + result.rssi() + "dBm");
            
            if(!result._dualband)
            	curChildMap.put(FREQ, "Frequencies: " + result._band + "KHz");
            else
            	curChildMap.put(FREQ, "Frequencies: " + result._band + "KHz / " + result._band2 + "KHz");
            
            curChildMap.put(CMAC, result._mac);	// clear string mac
		}
		childData.add(children);

        //////////////// 802.15.4 Networks
        curGroupMap = new HashMap<String, String>();
        groupData.add(curGroupMap);
        curGroupMap.put(NAME, "ZigBee");  
        curGroupMap.put(DESCRIPTION, "Description: 802.15.4 networks");
        children = new ArrayList<Map<String, String>>();
        childData.add(children);
        
		
        // Set up our adapter
        mAdapter = new SimpleExpandableListAdapter(
                this,
                groupData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] { NAME, DESCRIPTION },
                new int[] { android.R.id.text1, android.R.id.text2 },
                childData,
                R.layout.four_line_list_item,
                new String[] { NAME, MAC, RSSI, FREQ },
                new int[] { R.id.text1, R.id.text2, R.id.text3, R.id.text4 }
                );
        
        setListAdapter(mAdapter);
        registerForContextMenu(getExpandableListView());
    }
	
    public boolean onChildClick(
            ExpandableListView parent, 
            View v, 
            int groupPosition,
            int childPosition,
            long id) {
    	
    	last_group = groupPosition;
    	last_child = childPosition;
    	
	 	AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Name the Network");
		//alert.setMessage("Message");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setText(childData.get(last_group).get(last_child).get(NAME));
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String user_input = input.getText().toString();
			String ssid = childData.get(last_group).get(last_child).get(NAME);
			String bssid = childData.get(last_group).get(last_child).get(CMAC);
		  
			// First, do a lookup on the table to see if it exists
			int managed = db.getNetwork(bssid, ssid);
			
			// TODO: add the ability to remanage the master by adding the network
			
			if(managed!=-1) {	// Cannot add a network that is already managed
				Toast.makeText(getApplicationContext(), ssid + " is already managed.", Toast.LENGTH_LONG).show();
			} else {		// Add the network the list of managed networks 
				
				long res = db.insertNetwork(user_input, bssid, ssid, DBAdapter.PTYPE_80211, 0);
				if(res == -1) {
					Toast.makeText(getApplicationContext(), "Error inserting " + ssid + " in to the database.", Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(getApplicationContext(), "CoexiSyst is now managing " + user_input, Toast.LENGTH_LONG).show();
					// Since we successfully added the network, let's add the access point as a device
					int netid = db.getNetwork(bssid, ssid);
					if(db.insertNetDev(netid, bssid, "Access Point", DBAdapter.PTYPE_80211, 0)==-1) {
						Toast.makeText(getApplicationContext(), "Error inserting access point", Toast.LENGTH_LONG).show();
					}
				}
			}
		  }
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    return;
		  }
		});

		alert.show();
        return true;
    }
    
    // Use the last clicked item (device) to determine what to unmanage
    boolean unmanage() {
    	boolean r = false;
    	String mac = childData.get(last_group).get(last_child).get(CMAC);
    	String netid = childData.get(last_group).get(last_child).get(NETID);
    	//Log.d(TAG, "Unmanging from network: " + groupData.get(last_group).get(NAME));
    	Log.d(TAG, "Unamanging device: " + mac);
    	try {
    	 r = db.deleteDevice(netid, mac);
    	} catch(Exception e) {		
    		Log.e(TAG, "Exception trying to delete device", e);
    	}
    	return r;
    }
}
