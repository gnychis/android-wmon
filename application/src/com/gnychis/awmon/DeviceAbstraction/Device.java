package com.gnychis.awmon.DeviceAbstraction;

import java.util.ArrayList;
import java.util.List;


import android.os.Parcel;
import android.os.Parcelable;

// A device is a physical thing.  Like a laptop, an access point, etc.  It can
// have multiple radios attached.
public class Device implements Parcelable {
	
	public enum Mobility {		// Possible types of radios that we support
		UNKNOWN,
		MOBILE,
		FIXED,
	}

	List<WirelessInterface> _radios;	// Keep track of each radio detected
	private String _name;	// A name for the device
	Mobility _mobile;
		
	public Device() {
		_radios = new ArrayList<WirelessInterface>();
		_name = null;
		_mobile=Device.Mobility.UNKNOWN;
	}
	
	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
    public int describeContents() {
        return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
    	dest.writeTypedList(_radios);
    	dest.writeString(_name);
    	dest.writeInt(_mobile.ordinal());
    }

    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
    	public Device createFromParcel(Parcel in) {
    		return new Device(in);
    	}

		public Device[] newArray(int size) {
			return new Device[size];
		}
    };

    private Device(Parcel source) {
    	super();
    	source.readTypedList(_radios, WirelessInterface.CREATOR);
    	_name = source.readString();
    	_mobile = Device.Mobility.values()[source.readInt()];
    }

}
