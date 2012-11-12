package com.gnychis.awmon.DeviceAbstraction;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Interface implements Parcelable {
	
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
	
	public boolean hasValidIP() {
		if(_IP==null)
			return false;
		return validateIPAddress(_IP);
	}
	
	public final static boolean validateIPAddress( String ipAddress )
	{
	    String[] parts = ipAddress.split( "\\." );

	    if ( parts.length != 4 )
	        return false;

	    for ( String s : parts )
	        if ( (Integer.parseInt( s ) < 0) || (Integer.parseInt( s ) > 255) )
	            return false;

	    return true;
	}
	
	public String getReverseIP() {
		if(_IP==null)
			return null;
		return reverseIPAddress(_IP);
	}
	
	public final static String reverseIPAddress( String ipAddress ) {
		if(!validateIPAddress(ipAddress))
			return null;
		String[] parts = ipAddress.split( "\\." );
		return parts[3] + "." + parts[2] + "." + parts[1] + "." + parts[0];
	}
	
	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public static final Parcelable.Creator<Interface> CREATOR = new Parcelable.Creator<Interface>() {
		public Interface createFromParcel(Parcel in) {
			return new Interface(in);
		}

		public Interface[] newArray(int size) {
			return new Interface[size];
		}
	};
	
	public void writeToParcel(Parcel dest, int parcelableFlags) { writeInterfaceToParcel(dest, parcelableFlags); }
	private Interface(Parcel source) { readInterfaceParcel(source); }
	
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
