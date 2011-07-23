package com.gnychis.coexisyst;

import java.io.Serializable;

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
	public String getField(String f) {
		
		String result;
		
		if(_dissection_ptr == -1)
			return null;
		
		result = wiresharkGet(_dissection_ptr, f);
		
		if(result.equals(""))
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
	
	// TODO: instead, create a class where all of the wireshark functions are static
	public native int dissectPacket(byte[] header, byte[] data, int encap);
	public native void dissectCleanup(int dissect_ptr);
	public native String wiresharkGet(int dissect_ptr, String param);
}
