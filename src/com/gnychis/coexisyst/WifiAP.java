package com.gnychis.coexisyst;

import java.io.Serializable;

public class WifiAP implements Serializable {

	public String _mac;
	public String _ssid;
	public int _rssi;
	public int _frequency;  // kilohertz
	
	public Packet _beacon;
}
