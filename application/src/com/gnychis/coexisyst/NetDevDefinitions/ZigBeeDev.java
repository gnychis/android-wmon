package com.gnychis.coexisyst.NetDevDefinitions;

import java.util.ArrayList;
import java.util.Iterator;

import com.gnychis.coexisyst.Core.Packet;

import android.os.Parcel;
import android.os.Parcelable;

@SuppressWarnings("unchecked")
public class ZigBeeDev implements Parcelable {

	public String _mac;			// the source address
	public String _pan;  		// the network address
	public int _band;  			// the channel
	public ArrayList<Integer> _lqis;	// link quality indicators
	
	public Packet _beacon;
	
    public void writeToParcel(Parcel out, int flags) {
    	out.writeString(_mac);
    	out.writeString(_pan);
    	
    	out.writeInt(_band);
    	out.writeSerializable(_lqis);
    	out.writeParcelable(_beacon, 0);
    }
    
    private ZigBeeDev(Parcel in) {
    	_mac = in.readString();
    	_pan = in.readString(); 	
    	_band = in.readInt();
    	_lqis = (ArrayList<Integer>) in.readSerializable();
    	
    	_beacon = in.readParcelable(Packet.class.getClassLoader());
    }
	
	public int describeContents()
	{
		return this.hashCode();
	}
	
    public static final Parcelable.Creator<ZigBeeDev> CREATOR = new Parcelable.Creator<ZigBeeDev>() {
    	public ZigBeeDev createFromParcel(Parcel in) {
    		return new ZigBeeDev(in);
    	}

		public ZigBeeDev[] newArray(int size) {
			return new ZigBeeDev[size];
		}
    };
	
	public ZigBeeDev() {
		_band=-1;
		_lqis = new ArrayList<Integer>();
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
