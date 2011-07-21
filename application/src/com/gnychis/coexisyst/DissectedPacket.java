package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.Hashtable;

public class DissectedPacket {

	public int _packet_type;	// the encapsulation
	public Hashtable<String,ArrayList<String>> _fields;
	
	public DissectedPacket(int encapsulation, Hashtable<String,ArrayList<String>> f) {
		_packet_type = encapsulation;
		_fields = f;
	}
}
