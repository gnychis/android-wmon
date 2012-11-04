package com.gnychis.awmon.DeviceAbstraction;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class Interface implements Parcelable {
	
	public enum Type {
		WIRELESS,
		WIRED,
	}

	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public void writeToParcel(Parcel dest, int parcelableFlags) {

	}

	public static final Parcelable.Creator<Interface> CREATOR = new Parcelable.Creator<Interface>() {
		public Interface createFromParcel(Parcel in) {
			return new Interface(in);
		}

		public Interface[] newArray(int size) {
			return new Interface[size];
		}
	};

	@SuppressWarnings("unchecked")
	private Interface(Parcel source) {

	}

}
