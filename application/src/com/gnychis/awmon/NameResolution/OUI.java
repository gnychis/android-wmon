package com.gnychis.awmon.NameResolution;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.gnychis.awmon.Core.Radio;
import com.gnychis.awmon.Interfaces.MainInterface;

public class OUI extends NameResolver {
	
	public static final String TAG = "OUI";
	
	Map<String,String> _ouiTable;

	public OUI(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Radio.Type.Bluetooth, Radio.Type.Wifi));
		
		_ouiTable = new HashMap<String,String>();
		
		// Read in the OUI list
		try {
			// Open the file first
			FileInputStream fstream = new FileInputStream("/data/data/" + MainInterface._app_name + "/files/oui.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			// Go through and read each of the IDs and store them
			String strLine;
			 while ((strLine = br.readLine()) != null) {
				 String macPrefix = strLine.substring(0, 5);
				 String companyName = strLine.substring(22);
				 _ouiTable.put(macPrefix, companyName);
			 }
			in.close();
		} 
		catch(Exception e) { Log.e(TAG, "Error opening OUI text file"); }
	}
	
	public ArrayList<Radio> resolveSupportedRadios(ArrayList<Radio> supportedRadios) {
		for(Radio dev : supportedRadios) {
			String macPrefix = dev._MAC.replace("-", "").replace(":", "").substring(0, 5).toUpperCase();
			String companyName = _ouiTable.get(macPrefix);
			if(companyName!=null)
				dev._ouiName = companyName; 
		}
		return supportedRadios;
	}
}
