package com.gnychis.coexisyst;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

// To store raw packet information.  Can also piggyback a dissection pointer.
public class Packet implements Parcelable {
	public int _encap;
	public int _headerLen;
	public int _dataLen;
	public byte[] _rawHeader;
	public byte[] _rawData;
	public int _lqi;  // only for ZigBee
	public int _band;  // only for ZigBee also since there is no radiotap header
	
	public int _dissection_ptr;
		
	public Packet(int encap) {
		_rawHeader = null;
		_rawData = null;
		_encap = encap;
		_lqi = -1;
		_dissection_ptr = -1;
		_band = -1;
	}
	
	public int describeContents()
	{
		return this.hashCode();
	}
	
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(_encap);
        out.writeInt(_headerLen);
        out.writeInt(_dataLen);
        out.writeByteArray(_rawHeader);
        out.writeByteArray(_rawData);
        out.writeInt(_lqi);
        out.writeInt(_band);
        out.writeInt(-1);	// Cannot pass the dissection pointer, otherwise the GC will
        					// try to free the object more than once.  Once per copied object.
    }
    
    private Packet(Parcel in) {
        _encap = in.readInt();
        _headerLen = in.readInt();
        _dataLen = in.readInt();
        
        _rawHeader = new byte[_headerLen];
        _rawData = new byte[_dataLen];
        
        in.readByteArray(_rawHeader);
        in.readByteArray(_rawData);
        _lqi = in.readInt();
        _band = in.readInt();
        _dissection_ptr = in.readInt();
    }
	
    public static final Parcelable.Creator<Packet> CREATOR
    		= new Parcelable.Creator<Packet>() {
    	public Packet createFromParcel(Parcel in) {
    		return new Packet(in);
    	}

    	public Packet[] newArray(int size) {
    		return new Packet[size];
    	}
    };

    
	// Dissects the packet, wireshark-style.  Saves the pointer to
	// the dissection for the ability to pull fields.
	public boolean dissect() {
		
		if(_rawHeader == null || _rawData == null)
			return false;  // can't dissect something without the data
		
		if(_dissection_ptr != -1)  // packet is already dissected
			return true;

		_dissection_ptr = dissectPacket(_rawHeader, _rawData, _encap);
		
		if(_dissection_ptr == -1)
			return false;
		
		return true;
	}
	
	// Attempt to pull a field from the dissection
	public String getField(String f) {
		
		String result;
		
		if(_dissection_ptr == -1)
			if(!dissect())
				return null;
		
		result = wiresharkGet(_dissection_ptr, f);
		
		return result;
	}
	
	public void cleanDissection() {
		if(_dissection_ptr!=-1)
			dissectCleanup(_dissection_ptr);
		_dissection_ptr=-1;
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
		if(!dissect())  // will only dissect if needed
			return null;
		
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
	
	void nativeCrashed()
	{
		Log.d("Packet", "(JNIDEBUG) In nativeCrashed(): " + Integer.toString(_dissection_ptr) + ", writing crashed packet to /sdcard/crash.pcap");
		try {
			DataOutputStream os = new DataOutputStream(new FileOutputStream("/sdcard/crash.pcap"));
			byte pcap_header[] = {(byte)0xd4, (byte)0xc3, (byte)0xb2, (byte)0xa1, 		// magic number
					(byte)0x02, (byte)0x00, (byte)0x04,(byte) 0x00, 	// version numbers
					(byte)0x00, (byte)0x00, (byte)0x00,(byte) 0x00, 	// thiszone
					(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, 	// sigfigs
					(byte)0xff, (byte)0xff, (byte)0x00, (byte)0x00, 	// snaplen
					(byte)0x7f, (byte)0x00, (byte)0x00, (byte)0x00};  	// wifi
			
			os.write(pcap_header);
			os.write(_rawHeader);
			os.write(_rawData);
			os.close();
			Log.d("Packet", "Finished writing crashed packet");
			
		} catch(Exception e) { Log.e("nativeCrashed", "Exception trying to write crashed packet", e); }
		new RuntimeException("gcrashed here (native trace should follow after the Java trace)").printStackTrace();
	}
	
	// TODO: instead, create a class where all of the wireshark functions are static
	public native int dissectPacket(byte[] header, byte[] data, int encap);
	public native void dissectCleanup(int dissect_ptr);
	public native String wiresharkGet(int dissect_ptr, String param);
	public native String[] wiresharkGetAll(int dissect_ptr);
}
