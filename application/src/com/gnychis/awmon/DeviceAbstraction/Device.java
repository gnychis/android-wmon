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

	List<Interface> _interfaces;	// Keep track of each radio detected
	private String _name;			// A name for the device
	Mobility _mobile;
		
	public Device() {
		_interfaces = new ArrayList<Interface>();
		_name = null;
		_mobile=Device.Mobility.UNKNOWN;
	}
	
	public Device(List<Interface> interfaces) {
		_interfaces = interfaces;
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
    	dest.writeList(_interfaces);
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
    	_interfaces = new ArrayList<Interface>();
    	source.readList(_interfaces, this.getClass().getClassLoader());
    	_name = source.readString();
    	_mobile = Device.Mobility.values()[source.readInt()];
    }

}
