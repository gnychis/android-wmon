package com.gnychis.awmon.DeviceAbstraction;
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/** This is just a basic class to keep groups of interfaces together, that will
 * likely be used to create "devices"
 * @author George Nychis (gnychis)
 */
public class InterfaceGroup implements Parcelable {

	private List<Interface> _members;
	
	// The constructors
	public InterfaceGroup() { _members = new ArrayList<Interface>(); }
	public InterfaceGroup(List<Interface> initMembers) { _members = initMembers; }
	
	// Methods for accessing and setting members
	public void setMembers(List<Interface> members) { _members = members; }
	public void addMembers(List<Interface> members) { _members.addAll(members); }
	public List<Interface> getInterfaces() { return _members; }
	
	// For checking if this group contains an interface
	public boolean hasInterface(Interface iface) { return _members.contains(iface); }
	
    // ********************************************************************* //
    // This code is to make this class parcelable and needs to be updated if
    // any new members are added to the Device class
    // ********************************************************************* //
    public int describeContents() {
        return this.hashCode();
    }
    
    public static final Creator<InterfaceGroup> CREATOR = new Creator<InterfaceGroup>() {
        public InterfaceGroup createFromParcel(Parcel source) { return new InterfaceGroup(source); }
        public InterfaceGroup[] newArray(int size) { return new InterfaceGroup[size]; }
    };

    public void writeToParcel(Parcel dest, int parcelableFlags) {
    	dest.writeList(_members);
    }
    
    private InterfaceGroup(Parcel source) {
    	source.readList(_members, this.getClass().getClassLoader());
    }
}
