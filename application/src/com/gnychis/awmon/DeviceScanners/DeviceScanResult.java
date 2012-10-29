package com.gnychis.awmon.DeviceScanners;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.gnychis.awmon.Core.Device;
import com.gnychis.awmon.DeviceHandlers.HardwareDevice;

// The purpose of this class is to make a type/class that is easily passable through
// intents around the threads.
public class DeviceScanResult implements Parcelable {
	HardwareDevice.Type hwType;
	ArrayList<Device> devices;
	
    public DeviceScanResult(HardwareDevice.Type hwt, ArrayList<Device> devs) {
    	devices = new ArrayList<Device>();
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
        hwType = HardwareDevice.Type.values()[source.readInt()];
        devices = new ArrayList<Device>();
        source.readTypedList(devices, Device.CREATOR);
    }
}