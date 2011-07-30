package com.gnychis.coexisyst;

import java.util.ArrayList;
import java.util.Iterator;

import android.os.Parcel;
import android.os.Parcelable;

public class WifiAP implements Parcelable {

	public String _mac;
	public String _mac2;
	public String _ssid;
	public boolean _dualband;
	public int _band;  // kilohertz
	public int _band2;
	ArrayList<Integer> _rssis;
	
	//public Packet _beacon;
	
    public void writeToParcel(Parcel out, int flags) {
    	out.writeString(_mac);
    	out.writeString(_mac2);
    	out.writeString(_ssid);
    	
    	if(_dualband)
    		out.writeInt(1);
    	else
    		out.writeInt(0);
    	
    	out.writeInt(_band);
    	out.writeInt(_band2);
    	out.writeSerializable(_rssis);
    	//out.writeParcelable(_beacon, 0);
    }
    
    private WifiAP(Parcel in) {
    	_mac = in.readString();
    	_mac2 = in.readString();
    	_ssid = in.readString();
    	
    	if(in.readInt()==1)
    		_dualband=true;
    	else
    		_dualband=false;
    	
    	_band = in.readInt();
    	_band2 = in.readInt();
    	_rssis = (ArrayList<Integer>) in.readSerializable();
    	
    	//_beacon = in.readParcelable(null);
    }
	
	public int describeContents()
	{
		return this.hashCode();
	}
	
    public static final Parcelable.Creator<WifiAP> CREATOR = new Parcelable.Creator<WifiAP>() {
    	public WifiAP createFromParcel(Parcel in) {
    		return new WifiAP(in);
    	}

		public WifiAP[] newArray(int size) {
			return new WifiAP[size];
		}
};
	
	public WifiAP() {
		_band=-1;
		_band2=-1;
		_dualband=false;
		
		_rssis = new ArrayList<Integer>();
	}
	
	// Report the average RSSI
	public int rssi() {
		
		Iterator<Integer> rssis = _rssis.iterator();
		int sum=0;
		while(rssis.hasNext()) {
			int i = rssis.next().intValue();
			sum += i;
		}
		
		return sum / _rssis.size();
	}
}
