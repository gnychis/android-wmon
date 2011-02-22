package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;


public class ManageDevices extends ExpandableListActivity {

	private static final String TAG = "ManageDevices";
    private static final String NAME = "NAME";
    private static final String MAC = "MAC";
    private static final String CMAC = "CMAC";
    private static final String PROTOCOL = "PROTOCOL";
    
    public List<Map<String, String>> groupData;
    public List<List<Map<String, String>>> childData;
    
    DBAdapter db;
    
    public int last_group;
    public int last_child;
    
    private ExpandableListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  //setContentView(R.layout.devlist);
	  //setup_list();
	  db = new DBAdapter(this);
	  db.open();
	  setup_groups();
	  
	}
	
	public void setup_groups() {
		
        groupData = new ArrayList<Map<String, String>>();
        childData = new ArrayList<List<Map<String, String>>>();
        
        // Make groups out of the networks
        List<String> networks = db.getNetworks();
        for( String net : networks ) {
        	String proto = db.getProtocolOfNet(net);
            Map<String, String> curGroupMap = new HashMap<String, String>();
            groupData.add(curGroupMap);
            curGroupMap.put(NAME, net);  
            curGroupMap.put(PROTOCOL, "Network Type: " + proto);
            
            // Get all of the devices in the network
            List<String> devices = db.getDevicesInNet(net);
            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
            for (String dev : devices) {
                Map<String, String> curChildMap = new HashMap<String, String>();
                children.add(curChildMap);
                curChildMap.put(NAME, "Name: Access Point");
                curChildMap.put(MAC, "MAC Address: " + dev);
                curChildMap.put(CMAC, dev);
            }
            childData.add(children);
        }
        
        // Set up our adapter
        mAdapter = new SimpleExpandableListAdapter(
                this,
                groupData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] { NAME, PROTOCOL },
                new int[] { android.R.id.text1, android.R.id.text2 },
                childData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] { NAME, MAC },
                new int[] { android.R.id.text1, android.R.id.text2 }
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
    	
		final String[] options = {"Contending Networks", "Contending Devices", "Unmanage"};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose an Action");
		builder.setItems(options, new DialogInterface.OnClickListener() {
			
			// Wait for a user to click on an item in the list
		    public void onClick(DialogInterface dialog, int item) {
		    	switch(item) {
		    	case 0:
		    		Toast.makeText(getApplicationContext(), "Computing contending networks...", Toast.LENGTH_SHORT).show();
		    		break;
		    	case 1:
		    		Toast.makeText(getApplicationContext(), "Computing contending devices...", Toast.LENGTH_SHORT).show();
		    		break;
		    	case 2:
		    		if(unmanage())
		    			Toast.makeText(getApplicationContext(), "Device is now unmanaged...", Toast.LENGTH_SHORT).show();
		    		else 
		    			Toast.makeText(getApplicationContext(), "Error unmanaging device!", Toast.LENGTH_SHORT).show();
		    		setup_groups();
		    		break;
		    	}
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
        return true;
    }
    
    // Use the last clicked item (device) to determine what to unmanage
    boolean unmanage() {
    	String mac = childData.get(last_group).get(last_child).get(CMAC);
    	//Log.d(TAG, "Unmanging from network: " + groupData.get(last_group).get(NAME));
    	Log.d(TAG, "Unamanging device: " + mac);
    	return db.deleteDevice(mac);
    }
	
	
	/*
	public void setup_list() {
		Log.d(TAG, "Setting up the list...");
		String t[] = new String[5];
		t[0] = "First";
		t[1] = "Second";
		t[2] = "Third";
		t[3] = "Fourth";
		t[4] = "Fifth";
		
		//t = coexisyst.netlts_80211();
		setListAdapter(new ArrayAdapter<String>(this, R.layout.devices_list_item1 , t));
		
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			 public void onItemClick(AdapterView<?> parent, View view,
					 	int position, long id) {
			      // When clicked, show a toast with the TextView text
			      Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
			          Toast.LENGTH_SHORT).show();
			 }
		});
			  
	}*/
	
	
}
