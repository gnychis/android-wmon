package com.gnychis.awmon.DeviceScanners;

import java.util.ArrayList;

import org.jnetpcap.protocol.network.Arp.HardwareType;

import android.os.Parcel;
import android.os.Parcelable;

import com.gnychis.awmon.Core.Device;

// The purpose of this class is to make a type/class that is easily passable through
// intents around the threads.
public class DeviceScanResult implements Parcelable {
	HardwareType hwType;
	ArrayList<Device> devices;
	
    public DeviceScanResult(HardwareType hwt, ArrayList<Device> devs) {
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
    	dest.writeValue(hwType.ordinal());
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
        hwType = HardwareType.values()[source.readInt()];
        source.readTypedList(devices, Device.CREATOR);
    }
}