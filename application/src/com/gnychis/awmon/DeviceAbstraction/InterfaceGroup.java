package com.gnychis.awmon.DeviceAbstraction;
import java.util.ArrayList;
import java.util.List;



public class InterfaceGroup {

	private List<Interface> _members;
	
	public InterfaceGroup() {
		_members = new ArrayList<Interface>();
	}
	
	public InterfaceGroup(List<Interface> initMembers) {
		_members = initMembers;
	}
	
	public void setMembers(List<Interface> members) {
		_members = members;
	}
	
	public void addMembers(List<Interface> members) {
		_members.addAll(members);
	}
	
	
	public boolean hasInterface(Interface iface) {
		return _members.contains(iface);
	}
	
	public List<Interface> getMembers() {
		return _members;
	}

}
