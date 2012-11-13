package com.gnychis.awmon.Core;

import android.os.Parcel;
import android.os.Parcelable;

public class ScanRequest implements Parcelable  {
	
	boolean _doNameResoution;
	boolean _doMerging;

	public ScanRequest() {
		_doNameResoution=false;
		_doMerging=false;
	}
	
	public void setNameResolution(boolean value) { _doNameResoution=value; }
	public void setMerging(boolean value) { _doMerging=value; }
	public boolean doNameResolution() { return _doNameResoution; }
	public boolean doMerging() { return _doMerging; }
	
	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public static final Parcelable.Creator<ScanRequest> CREATOR = new Parcelable.Creator<ScanRequest>() {
		public ScanRequest createFromParcel(Parcel in) {
			return new ScanRequest(in);
		}

		public ScanRequest[] newArray(int size) {
			return new ScanRequest[size];
		}
	};


	public void writeToParcel(Parcel dest, int parcelableFlags) {
		dest.writeInt(_doNameResoution ? 1 : 0 );
		dest.writeInt(_doMerging ? 1 : 0 );
	}
	
	//@SuppressWarnings("unchecked")
	private ScanRequest(Parcel source) {
		_doNameResoution = ((source.readInt()==1) ? true : false );
		_doMerging = ((source.readInt()==1) ? true : false );
	}

}
