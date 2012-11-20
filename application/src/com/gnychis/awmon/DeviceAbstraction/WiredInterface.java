package com.gnychis.awmon.DeviceAbstraction;

import android.os.Parcel;
import android.os.Parcelable;

public class WiredInterface extends Interface implements Parcelable {
	
	public boolean _gateway;
	
	public void initVars() {
		_gateway=false;
	}
	
	public boolean isGateway() { return _gateway; }

	public WiredInterface() { super(); initVars(); }
	public WiredInterface(Interface i) { super(i); initVars(); }

	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public static final Parcelable.Creator<WiredInterface> CREATOR = new Parcelable.Creator<WiredInterface>() {
		public WiredInterface createFromParcel(Parcel in) {
			return new WiredInterface(in);
		}

		public WiredInterface[] newArray(int size) {
			return new WiredInterface[size];
		}
	};

	public void writeToParcel(Parcel dest, int parcelableFlags) {
		writeInterfaceToParcel(dest, parcelableFlags);
		dest.writeInt( (_gateway) ? 1 : 0 );
	}

	//@SuppressWarnings("unchecked")
	private WiredInterface(Parcel source) {
		readInterfaceParcel(source);
		_gateway = (source.readInt()==1) ? true : false;
	}
}
