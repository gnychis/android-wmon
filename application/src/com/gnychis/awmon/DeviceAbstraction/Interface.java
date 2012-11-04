package com.gnychis.awmon.DeviceAbstraction;

import android.os.Parcel;
import android.os.Parcelable;

public class Interface implements Parcelable {
	
	public enum Type {
		WIRELESS,
		WIRED,
	}
	
	Interface.Type _interfaceType;				// Wired or wireless? Has to be one or the other.
	public String _MAC;							// The MAC address of the interface, or some address.
	public String _IP;							// The IP address associated to the interface (null if none)
	public String _ouiName;						// The associated manufacturer OUI name (null if none)

	public Interface(Interface.Type t) {
		_interfaceType=t;
		_MAC=null;
		_IP=null;
		_ouiName=null;
	}

	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public void writeToParcel(Parcel dest, int parcelableFlags) {
		dest.writeInt(_interfaceType.ordinal());
		dest.writeString(_MAC);
    	dest.writeString(_IP);
    	dest.writeString(_ouiName);
	}

	public static final Parcelable.Creator<Interface> CREATOR = new Parcelable.Creator<Interface>() {
		public Interface createFromParcel(Parcel in) {
			return new Interface(in);
		}

		public Interface[] newArray(int size) {
			return new Interface[size];
		}
	};

	private Interface(Parcel source) {
		_interfaceType = Interface.Type.values()[source.readInt()];
		_MAC = source.readString();
        _IP = source.readString();
        _ouiName = source.readString();
	}

}
