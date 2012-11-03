package com.gnychis.awmon.Core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import android.os.Parcel;
import android.os.Parcelable;

// A radio is a physical radio that exists in a device.  
public class Radio implements Parcelable {
	
	public enum Type {		// Possible types of radios that we support
		Wifi,
		ZigBee,
		Bluetooth,
	}
	
	public ArrayList<Integer> _RSSI;		// The RSSI of the device at the phone
	public String _MAC;						// The MAC address of the device
	public int _frequency;					// The frequency it operates on
	public Radio.Type _type;				// The type of device
	public String _SSID;					// If the device belongs to a SSID (e.g., "The Smith's Wifi")
	public String _BSSID;					// The BSSID (MAC) of the coordinator
	public String _IP;						// The IP address associated to the radio
	public String _ouiName;					// The associated manufacturer OUI name
	
	public Radio(Radio.Type type) {
		_RSSI = new ArrayList<Integer>();
		_MAC=null;
		_frequency = -1;
		_type=type;
		_SSID = null;
		_BSSID = null;
		_IP=null;
		_ouiName=null;
	}
	
	// Report the average RSSI
	public int averageRSSI() {
		
		Iterator<Integer> rssis = _RSSI.iterator();
		int sum=0;
		while(rssis.hasNext()) {
			int i = rssis.next().intValue();
			sum += i;
		}
		
		return sum / _RSSI.size();
	}
	
	  static public Comparator<Object> compareRSSI = new Comparator<Object>() {
		public int compare(Object arg0, Object arg1) {
			if(((Radio)arg0).averageRSSI() < ((Radio)arg1).averageRSSI())
				return 1;
			else if( ((Radio)arg0).averageRSSI() > ((Radio)arg1).averageRSSI())
				return -1;
			else
				return 0;
		}
	  };
	
	  // ********************************************************************* //
	  // This code is to make this class parcelable and needs to be updated if
	  // any new members are added to the Device class
	  // ********************************************************************* //
	    public int describeContents() {
	        return this.hashCode();
	    }

	    public void writeToParcel(Parcel dest, int parcelableFlags) {
	    	dest.writeSerializable(_RSSI);
	    	dest.writeString(_MAC);
	    	dest.writeInt(_frequency);
	    	dest.writeInt(_type.ordinal());
	    	dest.writeString(_SSID);
	    	dest.writeString(_BSSID);
	    	dest.writeString(_IP);
	    	dest.writeString(_ouiName);
	    }

	    public static final Parcelable.Creator<Radio> CREATOR = new Parcelable.Creator<Radio>() {
	    	public Radio createFromParcel(Parcel in) {
	    		return new Radio(in);
	    	}

			public Radio[] newArray(int size) {
				return new Radio[size];
			}
	    };

	    @SuppressWarnings("unchecked")
	    private Radio(Parcel source) {
	    	_RSSI = (ArrayList<Integer>) source.readSerializable();
	    	_MAC = source.readString();
	    	_frequency = source.readInt();
	        _type = Radio.Type.values()[source.readInt()];
	        _SSID = source.readString();
	        _BSSID = source.readString();
	        _IP = source.readString();
	        _ouiName = source.readString();
	    }
}
