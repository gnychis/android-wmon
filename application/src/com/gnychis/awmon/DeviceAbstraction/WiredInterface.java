package com.gnychis.awmon.DeviceAbstraction;

import android.os.Parcel;
import android.os.Parcelable;

public class WiredInterface extends Interface implements Parcelable {
	
	@Override
	public Interface.Type getInterfaceType() { return Interface.Type.WIRED; }
	
	// Some variables up in here.
	public enum Type {		// Possible types of radios that we support
		Ethernet,
	}
	public WiredInterface.Type _wiredType;
	
	public void initVars(WiredInterface.Type type) {
		_wiredType = type;
	}

	public WiredInterface(WiredInterface.Type type) { super(); initVars(type); }
	public WiredInterface(Interface i,WiredInterface.Type type) { super(i); initVars(type); }

	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public void writeToParcel(Parcel dest, int parcelableFlags) {
		dest.writeInt(_wiredType.ordinal());
	}
	
	public static final Parcelable.Creator<WiredInterface> CREATOR = new Parcelable.Creator<WiredInterface>() {
		public WiredInterface createFromParcel(Parcel in) {
			return new WiredInterface(in);
		}

		public WiredInterface[] newArray(int size) {
			return new WiredInterface[size];
		}
	};

	//@SuppressWarnings("unchecked")
	private WiredInterface(Parcel source) {
		_wiredType = WiredInterface.Type.values()[source.readInt()];
	}
}
