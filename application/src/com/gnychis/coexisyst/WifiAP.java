package com.gnychis.coexisyst;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class WifiAP implements Serializable {

	public String _mac;
	public String _mac2;
	public String _ssid;
	public boolean _dualband;
	public int _band;  // kilohertz
	public int _band2;
	ArrayList<Integer> _rssis;
	
	public Packet _beacon;
	
	public WifiAP() {
		_band=-1;
		_band2=-1;
		_dualband=false;
		
		_rssis = new ArrayList<Integer>();
	}
	
	// Report the average RSSI
	public int rssi() {
		
		Iterator<Integer> rssis = _rssis.iterator();
		int sum=0;
		while(rssis.hasNext()) {
			int i = rssis.next().intValue();
			sum += i;
		}
		
		return sum / _rssis.size();
	}
}
