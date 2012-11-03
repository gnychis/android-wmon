package com.gnychis.awmon.DeviceScanners;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.gnychis.awmon.Core.Radio;
import com.gnychis.awmon.DeviceHandlers.InternalRadio;

// The purpose of this class is to make a type/class that is easily passable through
// intents around the threads.
public class DeviceScanResult implements Parcelable {
	public InternalRadio.Type hwType;
	public ArrayList<Radio> devices;
	
    public DeviceScanResult(InternalRadio.Type hwt, ArrayList<Radio> devs) {
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

    public static final Creator<DeviceScanResult> CREATOR = new Creator<DeviceScanResult>() {
        public DeviceScanResult createFromParcel(Parcel source) {
            return new DeviceScanResult(source);
        }
        public DeviceScanResult[] newArray(int size) {
            return new DeviceScanResult[size];
        }
    };

    private DeviceScanResult(Parcel source) {
        hwType = InternalRadio.Type.values()[source.readInt()];
        devices = new ArrayList<Radio>();
        source.readTypedList(devices, Radio.CREATOR);
    }
}