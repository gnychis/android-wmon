package com.gnychis.awmon.InterfaceScanners;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

// The purpose of this class is to make a type/class that is easily passable through
// intents around the threads.
public class RadioScanResult implements Parcelable {
	public WirelessInterface.Type hwType;
	public ArrayList<Interface> _interfaces;
	
    public RadioScanResult(WirelessInterface.Type hwt, ArrayList<Interface> interfaces) {
    	hwType = hwt;
    	_interfaces = interfaces;
    }

    // ********************************************************************* //
    // This code is to make this class parcelable and needs to be updated if
    // any new members are added to the Device class
    // ********************************************************************* //
    public int describeContents() {
        return this.hashCode();
    }

    // You cannot simply use a writeTypedList(devices), because there are different
    // child classes that interface can be using like WirelessInterface or WiredInterface
    public void writeToParcel(Parcel dest, int parcelableFlags) {
    	dest.writeInt(hwType.ordinal());
    	dest.writeList(_interfaces);
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
        hwType = WirelessInterface.Type.values()[source.readInt()];
        _interfaces = new ArrayList<Interface>();
        source.readList(_interfaces, this.getClass().getClassLoader());
    }
}