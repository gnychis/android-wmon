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

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.GUIs.MainInterface;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

public class OUI extends NameResolver {
	
	public static final String TAG = "OUI";
	
	Map<String,String> _ouiTable;

	@SuppressWarnings("unchecked")
	public OUI(NameResolutionManager nrm) {
		super(nrm, Arrays.asList(Wifi.class, Bluetooth.class, LAN.class));
		
		_ouiTable = new HashMap<String,String>();
		
		// Read in the OUI list
		try {
			// Open the file first
			DataInputStream in = new DataInputStream(new FileInputStream("/data/data/" + MainInterface._app_name + "/files/oui.txt"));
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
	
	public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedInterfaces) {
		for(Interface iface : supportedInterfaces) {
			String macPrefix = iface._MAC.replace("-", "").replace(":", "").substring(0, 5).toUpperCase();
			String companyName = _ouiTable.get(macPrefix);
			if(companyName!=null) {
				if(iface._ouiName==null)
					_resolved++;
				iface._ouiName = companyName;
			}
		}
		return supportedInterfaces;
	}
	
	public static String getOUI(String MAC) {
		Map<String,String> ouiTable = new HashMap<String,String>();
		
		// Read in the OUI list
		try {
			// Open the file first
			DataInputStream in = new DataInputStream(new FileInputStream("/data/data/" + MainInterface._app_name + "/files/oui.txt"));
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			// Go through and read each of the IDs and store them
			String strLine;
			 while ((strLine = br.readLine()) != null) {
				 String macPrefix = strLine.substring(0, 5);
				 String companyName = strLine.substring(22);
				 ouiTable.put(macPrefix, companyName);
			 }
			in.close();
		} 
		catch(Exception e) { Log.e(TAG, "Error opening OUI text file"); return null; }
		
		String macPrefix = MAC.replace("-", "").replace(":", "").substring(0, 5).toUpperCase();
		String companyName = ouiTable.get(macPrefix);
		if(companyName!=null)
			return companyName; 
		
		return null;
	}
}
