package com.gnychis.awmon.Core;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

// A device is a physical thing.  Like a laptop, an access point, etc.  It can
// have multiple radios attached.
public class Device implements Parcelable {

	List<Radio> _radios;	// Keep track of each radio detected
	String _name;			// A name for the device
	
	public Device() {
		_radios = new ArrayList<Radio>();
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
    	_radios = new ArrayList<Radio>();
    	source.readTypedList(_radios, Radio.CREATOR);
    	_name = source.readString();
    }

}
