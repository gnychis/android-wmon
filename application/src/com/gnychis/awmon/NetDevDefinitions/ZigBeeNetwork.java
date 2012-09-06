package com.gnychis.awmon.NetDevDefinitions;

import java.util.ArrayList;
import java.util.Iterator;


import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("unchecked")
public class ZigBeeNetwork implements Parcelable {

	public String _mac;						// the source address (of the coordinator?)
	public String _pan;  					// the network address
	public int _band;  						// the channel
	public ArrayList<Integer> _lqis;		// link quality indicators (to all devices?)
	public ArrayList<ZigBeeDev> _devices;	// the devices in the network
		
    public void writeToParcel(Parcel out, int flags) {
    	out.writeString(_mac);
    	out.writeString(_pan);
    	
    	out.writeInt(_band);
    	out.writeSerializable(_lqis);
    	out.writeTypedList(_devices);
    }
    
    private ZigBeeNetwork(Parcel in) {
    	_devices = new ArrayList<ZigBeeDev>();
    	_mac = in.readString();
    	_pan = in.readString(); 	
    	_band = in.readInt();
    	_lqis = (ArrayList<Integer>) in.readSerializable();
    	in.readTypedList(_devices, ZigBeeDev.CREATOR);
    }
	
	public int describeContents()
	{
		return this.hashCode();
	}
	
    public static final Parcelable.Creator<ZigBeeNetwork> CREATOR = new Parcelable.Creator<ZigBeeNetwork>() {
    	public ZigBeeNetwork createFromParcel(Parcel in) {
    		return new ZigBeeNetwork(in);
    	}

		public ZigBeeNetwork[] newArray(int size) {
			return new ZigBeeNetwork[size];
		}
};
	
	public ZigBeeNetwork() {
		_band=-1;
		_lqis = new ArrayList<Integer>();
		_devices = new ArrayList<ZigBeeDev>();
	}
	
	// Report the average RSSI
	public int lqi() {
		
		Iterator<Integer> rssis = _lqis.iterator();
		int sum=0;
		while(rssis.hasNext()) {
			int i = rssis.next().intValue();
			sum += i;
		}
		
		return sum / _lqis.size();
	}
}
