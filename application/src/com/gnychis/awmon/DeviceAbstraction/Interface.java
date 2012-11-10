package com.gnychis.awmon.DeviceAbstraction;

import android.os.Parcel;
import android.util.Log;

abstract public class Interface {
	
	public String _MAC;							// The MAC address of the interface, or some address.
	public String _IP;							// The IP address associated to the interface (null if none)
	public String _ouiName;						// The associated manufacturer OUI name (null if none)
	public String _ifaceName;					// A name associated with the specific interface
	public Class<?> _type;						// The interface type (should be a class in HardwareHandlers that extended InternalRadio)

	public Interface() {
		_MAC=null;
		_IP=null;
		_ouiName=null;
		_ifaceName=null;
		_type=null;	
	}
	
	public Interface(Class<?> type) {
		_MAC=null;
		_IP=null;
		_ouiName=null;
		_ifaceName=null;
		_type=type;
	}
	
	public Interface(Interface i) {
		_MAC=i._MAC;
		_IP=i._IP;
		_ouiName=i._ouiName;
		_ifaceName=i._ifaceName;
		_type=i._type;
	}
	
	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public void writeInterfaceToParcel(Parcel dest, int parcelableFlags) {
		dest.writeString(_MAC);
    	dest.writeString(_IP);
    	dest.writeString(_ouiName);
    	dest.writeString(_ifaceName);
    	dest.writeString(_type.getName());
	}

	public void readInterfaceParcel(Parcel source) {
		_MAC = source.readString();
        _IP = source.readString();
        _ouiName = source.readString();
        _ifaceName = source.readString();
        try {
        _type = Class.forName(source.readString());
        } catch(Exception e) { Log.e("Interface", "Error getting class in Interface parcel"); }
	}
}
