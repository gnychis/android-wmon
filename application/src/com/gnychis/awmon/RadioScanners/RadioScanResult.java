package com.gnychis.awmon.RadioScanners;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.gnychis.awmon.Core.Radio;

// The purpose of this class is to make a type/class that is easily passable through
// intents around the threads.
public class RadioScanResult implements Parcelable {
	public Radio.Type hwType;
	public ArrayList<Radio> devices;
	
    public RadioScanResult(Radio.Type hwt, ArrayList<Radio> devs) {
    	devices = new ArrayList<Radio>();
    	hwType = hwt;
    	devices = devs;
    }

    // ********************************************************************* //
    // This code is to make this class parcelable and needs to be updated if
    // any new members are added to the Device class
    // ********************************************************************* //
    public int describeContents() {
        return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
    	dest.writeInt(hwType.ordinal());
    	dest.writeTypedList(devices);
    }

    public static final Creator<RadioScanResult> CREATOR = new Creator<RadioScanResult>() {
        public RadioScanResult createFromParcel(Parcel source) {
            return new RadioScanResult(source);
        }
        public RadioScanResult[] newArray(int size) {
            return new RadioScanResult[size];
        }
    };

    private RadioScanResult(Parcel source) {
        hwType = Radio.Type.values()[source.readInt()];
        devices = new ArrayList<Radio>();
        source.readTypedList(devices, Radio.CREATOR);
    }
}