package com.gnychis.coexisyst;

import java.io.Serializable;

public class WifiAP implements Serializable {

	public String _mac;
	public String _mac2;
	public String _ssid;
	public boolean _dualband;
	public int _rssi;
	public int _band;  // kilohertz
	public int _band2;
	
	public Packet _beacon;
	
	public WifiAP() {
		_band=-1;
		_band2=-1;
		_dualband=false;
	}
}
