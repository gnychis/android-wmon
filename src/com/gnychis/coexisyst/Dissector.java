package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;

import android.content.Context;
import android.os.AsyncTask;

///////////////////////////////////////////////////
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//        THIS CLASS IS NOT USED
// ...but kept for reference
///////////////////////////////////////////////////

// The goal is to make a thread for a process or for a scan to send
// all of the packets it needs dissected, and be able to have the thread
// do the work while the scanning thread does the scanning without being
// held up (for example).  
public class Dissector extends AsyncTask<Context, Integer, String> {
	
	public int _packets_in;
	public BlockingQueue<Packet> _incoming_queue;
	
	public Dissector() {
		_packets_in=0;
	}

	@Override
	protected String doInBackground( Context ... params )
	{
		
		
		return "OK";
	}
	
	// Dissect a packet if the condition holds true, where the 
	// condition[0] is the field, and condition[1] is the value.
	// This prevents full dissection (expensive) if not needed.
	public void dissectPacket(String[] condition) {
		
	}
		
	// Need to do special parsing to handle multiple "interpretation" fields
	// When you see a "wlan_mgt.tag.number", prepend the value to make a key such as "wlan_mgt.tag.number0"
	// ... then put all "wlan_mgt.tag.interpretation" until the next "wlan_mgt.tag.number" as values in this key.
	// A new unique key might be hit in between, so keep something like last_tagnum = "wlan_mgt.tag.number0"
	public Hashtable<String,ArrayList<String>> dissectAll(Packet rpkt) {
		
		String last_tag = "";
		
		if(rpkt._dissection_ptr == -1)
			rpkt._dissection_ptr = dissectPacket(rpkt._rawHeader, rpkt._rawData, rpkt._encap);
		
		// First dissect the entire packet, getting all fields
		String fields[] = wiresharkGetAll(rpkt._dissection_ptr);
		
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
	
	public native int dissectPacket(byte[] header, byte[] data, int encap);
	public native void dissectCleanup(int dissect_ptr);
	public native String wiresharkGet(int dissect_ptr, String param);
	public native void wiresharkTest(String filename);
	public native void wiresharkTestGetAll(String filename);
	public native String[] wiresharkGetAll(int dissect_ptr);
	public native void wiresharkGetAllTest(int dissect_ptr);
}
