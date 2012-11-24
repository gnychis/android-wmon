package com.gnychis.awmon.GUIs;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.gnychis.awmon.R;

public class YourDevices extends Activity {

	//ArrayList that will hold the original Data
	ArrayList<HashMap<String, Object>> devices;
	LayoutInflater inflater;
	
	private ListView m_listview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.your_devices);
		
		m_listview = (ListView) findViewById(android.R.id.list);
		inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Some temporary arrays for the data
		String names[]={"The Smith's Wifi","Creative D200 Speaker","Power Monitor Sensor","Bill's iPad",
				"Xbox 360","Gaming Controller","Don't Steal Our Wifi", "Something else"}; 

		String additional[]={"802.11n (40MHz)","Bluetooth","ZigBee",
				"802.11n","802.11n","Proprietary",
		"802.11g", "whatever"};
		
		devices=new ArrayList<HashMap<String,Object>>();

		//temporary HashMap for populating the 
		//Items in the ListView
		HashMap<String , Object> temp;

		//total number of rows in the ListView
		int noOfPlayers=names.length;

		//now populate the ArrayList devices
		for(int i=0;i<noOfPlayers;i++)
		{
			temp=new HashMap<String, Object>();

			temp.put("name", names[i]);
			temp.put("additional", additional[i]);    

			//add the row to the ArrayList
			devices.add(temp);        
		}

		final CustomAdapter adapter=new CustomAdapter(this, R.layout.device_list_item ,devices); 

		//finally,set the adapter to the default ListView
		m_listview.setAdapter(adapter);


	}


	//define your custom adapter
	private class CustomAdapter extends ArrayAdapter<HashMap<String, Object>>
	{
		boolean[] checkBoxState;
		ViewHolder viewHolder;

		public CustomAdapter(Context context, int textViewResourceId,
				ArrayList<HashMap<String, Object>> players) {
			super(context, textViewResourceId, players); 
			checkBoxState=new boolean[players.size()];
		}

		//class for caching the views in a row  
		private class ViewHolder
		{
			TextView name,additional;
			CheckBox checkBox;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			if(convertView==null)
			{
				convertView=inflater.inflate(R.layout.device_list_item, null);
				viewHolder=new ViewHolder();

				//cache the views
				viewHolder.name=(TextView) convertView.findViewById(R.id.name);
				viewHolder.additional=(TextView) convertView.findViewById(R.id.additional);
				viewHolder.checkBox=(CheckBox) convertView.findViewById(R.id.checkBox);

				//link the cached views to the convertview
				convertView.setTag( viewHolder);


			}
			else
				viewHolder=(ViewHolder) convertView.getTag();

			viewHolder.name.setText(devices.get(position).get("name").toString());
			viewHolder.additional.setText("Additional: " + devices.get(position).get("additional").toString());
			viewHolder.checkBox.setChecked(checkBoxState[position]);

			viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {

				public void onClick(View v) {
					if(((CheckBox)v).isChecked())
						checkBoxState[position]=true;
					else
						checkBoxState[position]=false;

				}
			});

			//return the view to be displayed
			return convertView;
		}

	}

    @Override
    public void onBackPressed() {
    	Intent i = new Intent(YourDevices.this, HomeLocation.class);
        startActivity(i);
    	finish();
    }
}
