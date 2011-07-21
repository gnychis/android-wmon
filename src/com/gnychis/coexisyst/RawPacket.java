package com.gnychis.coexisyst;

// To store raw packet information.  Can also piggyback a dissection pointer.
public class RawPacket {
	public int _encap;
	public byte[] _rawHeader;
	public byte[] _rawData;
	
	public int _dissection_ptr;
	
	public RawPacket(int encap) {
		_encap = encap;
		_dissection_ptr = -1;
	}
	
	// On garbage collection (this raw data is no longer used), make sure to 
	// cleanup the dissection
	protected void finalize() throws Throwable {
	    try {
	    	
	    	if(_dissection_ptr!=-1)
	    		dissectCleanup(_dissection_ptr);

	    } finally {
	        super.finalize();
	    }
	}
	
	public native void dissectCleanup(int dissect_ptr);
}
