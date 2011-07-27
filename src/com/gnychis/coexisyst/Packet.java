package com.gnychis.coexisyst;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;

// To store raw packet information.  Can also piggyback a dissection pointer.
public class Packet implements Serializable {
	public int _encap;
	public byte[] _rawHeader;
	public byte[] _rawData;
	
	public int _dissection_ptr;
		
	public Packet(int encap) {
		_rawHeader = null;
		_rawData = null;
		_encap = encap;
		_dissection_ptr = -1;
	}
	
	// Dissects the packet, wireshark-style.  Saves the pointer to
	// the dissection for the ability to pull fields.
	public boolean dissect() {
		
		if(_rawHeader == null || _rawData == null)
			return false;  // can't dissect something without the data
		
		if(_dissection_ptr != -1)  // packet is already dissected
			return true;

		_dissection_ptr = dissectPacket(_rawHeader, _rawData, _encap);
		
		return true;
	}
	
	// Attempt to pull a field from the dissection
	public String[] getField(String f) {
		
		String[] result;
		
		if(_dissection_ptr == -1)
			return null;
		
		result = wiresharkGet(_dissection_ptr, f);
		
		if(result[0].equals(""))
			return null;
		
		return result;
	}
	
	// On garbage collection (this raw data is no longer used), make sure to 
	// cleanup the dissection memory.
	protected void finalize() throws Throwable {
	    try {
	    	
	    	if(_dissection_ptr!=-1)
	    		dissectCleanup(_dissection_ptr);

	    } finally {
	        super.finalize();
	    }
	}
	
	public Hashtable<String,ArrayList<String>> getAllFields()
	{
		dissect();  // will only dissect if needed
		String last_tag = "";
		
		// First dissect the entire packet, getting all fields
		String fields[] = wiresharkGetAll(_dissection_ptr);
		
		// Now, store all of the fields in a hash table, where each element accesses
		// an array.  This is done since fields can have multiple values
		Hashtable<String,ArrayList<String>> pkt_fields;
		pkt_fields = new Hashtable<String,ArrayList<String>>();
		
		// Each field is a descriptor and value, split by a ' '
		for(int i=0; i<fields.length;i++) {
			String spl[] = fields[i].split(" ", 2); // spl[0]: desc, spl[1]: value
			
			// If it is wlan_mgt.tag.interpretation, start to save the interpretations
			if(spl[0].equals("wlan_mgt.tag.number")) {
				last_tag = spl[0] + spl[1];
				continue;
			}
			
			// No need to save these
			if(spl[0].equals("wlan_mgt.tag.length"))
				continue;
			
			// Append the tag interpretation to the last tag, we reuse code by
			// switching spl[0] to last_tag.  The value of the interpretation will
			// therefore be appended to wlan_mgt.tag.numberX
			if(spl[0].equals("wlan_mgt.tag.interpretation"))
				spl[0] = last_tag;
			
			// Check the hash table for the key, and then append the value in the
			// list of values associated.
			if(pkt_fields.containsKey(spl[0])) {
				ArrayList<String> l = pkt_fields.get(spl[0]);
				l.add(spl[1]);
			} else {
				ArrayList<String> l = new ArrayList<String>();
				l.add(spl[1]);
				pkt_fields.put(spl[0], l);
			}
		}
		
		return pkt_fields;
	}
	
	// TODO: instead, create a class where all of the wireshark functions are static
	public native int dissectPacket(byte[] header, byte[] data, int encap);
	public native void dissectCleanup(int dissect_ptr);
	public native String[] wiresharkGet(int dissect_ptr, String param);
	public native String[] wiresharkGetAll(int dissect_ptr);
}
