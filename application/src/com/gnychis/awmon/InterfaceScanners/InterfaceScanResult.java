package com.gnychis.awmon.InterfaceScanners;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.Interface;

// The purpose of this class is to make a type/class that is easily passable through
// intents around the threads.
public class InterfaceScanResult implements Parcelable {
	public Class<?> _interfaceType;
	public ArrayList<Interface> _interfaces;
	
    public InterfaceScanResult(Class<?> ifaceType, ArrayList<Interface> interfaces) {
    	_interfaceType = ifaceType;
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
    	dest.writeString(_interfaceType.getName());
    	dest.writeList(_interfaces);
    }
    

    public static final Creator<InterfaceScanResult> CREATOR = new Creator<InterfaceScanResult>() {
        public InterfaceScanResult createFromParcel(Parcel source) {
            return new InterfaceScanResult(source);
        }
        public InterfaceScanResult[] newArray(int size) {
            return new InterfaceScanResult[size];
        }
    };

    private InterfaceScanResult(Parcel source) {
        try {
        _interfaceType = Class.forName(source.readString());
        } catch(Exception e) { Log.e("ScanResult", "Error getting class in ScanResult parcel"); }
        _interfaces = new ArrayList<Interface>();
        source.readList(_interfaces, this.getClass().getClassLoader());
    }
}