package com.gnychis.awmon.GUIs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.gnychis.awmon.R;
import com.gnychis.awmon.Core.Snapshot;
import com.gnychis.awmon.Database.DBAdapter;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

public class SnapshotDetails extends Activity {
	
	private static final String TAG = "SnapshotDetails";
	private static final boolean VERBOSE = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.snapshot_details_list);
		
		Bundle extras = getIntent().getExtras();
		String date = extras.getString("date");
		
		((TextView)findViewById(R.id.date)).append(" " + date);
		
		DBAdapter dbAdapter = new DBAdapter(this);
		dbAdapter.open();
		
		Snapshot snapshot = dbAdapter.getSnapshot(Snapshot.getDateFromString(date));
		((TextView)findViewById(R.id.anchor)).append( (snapshot.getAnchorMAC()!=null) ? " " + snapshot.getAnchorMAC() : " <None>" );

		TableLayout table = (TableLayout)findViewById(R.id.maintable);

		LayoutInflater inflater = getLayoutInflater();
		int i=0;
		for(Interface iface : snapshot.getInterfaces())
		{
			String nameString=(iface._ifaceName!=null) ? iface._ifaceName : "";
			String internalString="No";
			String protocolString=(iface._type!=null) ? Interface.simplifiedClassName(iface._type) : "";
			String macString=iface._MAC;
			String signalString=(iface.getClass()==WirelessInterface.class && ((WirelessInterface)iface).averageRSSI()!=-500) ? ((WirelessInterface)iface).averageRSSI() + "dBm" : "-";
			
			TableRow row = (TableRow)inflater.inflate(R.layout.snapshot_table_row, table, false);
			
			((TextView)row.findViewById(R.id.name)).setText(nameString);
			((TextView)row.findViewById(R.id.internal)).setText(internalString);
			((TextView)row.findViewById(R.id.protocol)).setText(protocolString);
			((TextView)row.findViewById(R.id.mac)).setText(macString);
			((TextView)row.findViewById(R.id.signal)).setText(signalString);
			
			if(i%2==0)
				row.setBackgroundColor(0xff222222);

			table.addView(row);
			i++;
		}
		dbAdapter.close();
	}
	
	@Override
	public void onBackPressed() {
		Intent i = new Intent(SnapshotDetails.this, SnapshotList.class);
		startActivity(i);
		finish();
	}

	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
