package com.gnychis.awmon.InterfaceMerging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.InterfaceGroup;


/** The purpose of this class is to provide an implementation of a graph where
 * nodes are connected via bidirectional edges, and the implementation provides the
 * ability to find groups of nodes connected together.  This allows us to build graphs
 * of interfaces that we believe are connected to the same physical device, and easily
 * retrieve them after a set of heuristics which add or break edges. 
 * 
 * @author George Nychis (gnychis)
 */
public class InterfaceConnectivityGraph {

	private List<Interface> _interfaces;
	private Map<String,Boolean> _graph;
	
	private List<Interface> _visitedNodes;
	
	/** Creates a new interface connectivity graph for use in merging interfaces
	 * in to devices.
	 * @param interfaces
	 */
	public InterfaceConnectivityGraph(List<Interface> interfaces) {
		_interfaces = interfaces;
		_graph = new HashMap<String,Boolean>();
		initGraph();
	}
	
	
	/** Initializes the graph where all of the interfaces are unconnected.  Note:
	 * _interfaces MUST be populated, otherwise this will fail.
	 * @return
	 */
	private void initGraph() {
		for(Interface i1 : _interfaces)
			for(Interface i2 : _interfaces)
				_graph.put(hashKey(i1,i2), false);		
	}
	
	/** Returns the hash key for the two interfaces in our connectivity graph.  Since
	 * the graph is bidirectional, we make sure that the ordering does not matter.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return the key to _graph for the two interfaces.  Returns null if pair doesn't exist.
	 */
	private String hashKey(Interface i1, Interface i2) {
		String key=null;
		if(i1.hashCode()<i2.hashCode())
			key = Integer.toString(i1.hashCode()) + "," + Integer.toString(i2.hashCode());
		else
			key = Integer.toString(i2.hashCode()) + "," + Integer.toString(i1.hashCode());
		return key;
	}
		
	/** This sets the value of the connection between two interfaces in the graph.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @param value true for connected, false for unconnected.
	 * @return
	 */
	public boolean setConnected(Interface i1, Interface i2, boolean value) {
		if(!validPair(i1, i2))
			return false;
		_graph.put(hashKey(i1,i2), value);
		return true;
	}
	
	/** This function connects two interfaces together in our graph.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return returns true if they were connected, false if the connection failed.
	 */
	public boolean connect(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return false;
		_graph.put(hashKey(i1,i2), true);
		return true;
	}
	
	/** This function disconnects two interfaces together in our graph.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return returns true if they were disconnected, false if the connection failed.
	 */
	public boolean disconnect(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return false;
		_graph.put(hashKey(i1,i2), false);
		return true;
	}
	
	/** This returns whether or not the pair of interfaces is valid in our graph.  I.e.,
	 * both must exist in our graph to be valid.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return true if valid, false if not.
	 */
	public boolean validPair(Interface i1, Interface i2) {
		String key = hashKey(i1,i2);
		if(!_graph.containsKey(key))
			return false;
		return true;
	}
	
	/** This function will return true if the two interfaces are connected in our
	 * graph, false if otherwise.  There is no ordering here, it is bidirectional.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return true if i1 and i2 are connected, false otherwise.
	 */
	public boolean areConnected(Interface i1, Interface i2) {
		
		if(!validPair(i1, i2))  // If one of the interfaces is not in the graph, return false
			return false;
		
		if(areDirectlyConnected(i1, i2))  // If they are directly connected, they are connected
			return true;
		
		_visitedNodes = new ArrayList<Interface>();
		return checkIndirectConnection(i1, i2);
	}
	
	/** This function will return true if the two interfaces are directly connected in our
	 * graph, false if otherwise.  There is no ordering here, it is bidirectional.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return true if i1 and i2 are connected, false otherwise.
	 */
	public boolean areDirectlyConnected(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return false;
		return _graph.get(hashKey(i1,i2));
	}
	
	/** This function which will be used via recursion to try and get all of the connected nodes,
	 *  passing along a "visited" variable to avoid loops.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @param visited A list of the visited nodes so far
	 * @return true if i1 and i2 are connected, false otherwise.
	 */
	public boolean checkIndirectConnection(Interface i1, Interface i2) {
		
		// If we have already been here before, let's escape...
		if(_visitedNodes.contains(i1))
			return false;
		_visitedNodes.add(i1);	// We have now visited i1
		
		List<Interface> directlyConnected = getDirectlyConnected(i1);
		for(Interface iface : directlyConnected) {
			if(iface == i2)		// Hey, we found a node directly connected to i2 (iface is)
				return true;
			
			if(checkIndirectConnection(iface, i2)==true)
				return true;
		}
		
		return false;
	}
	
	/** This returns all interfaces that are directly connected to the given interface.
	 * This does not include itself.
	 * @param iface The interface to find those directly connected to it.
	 * @return a list of interfaces directly connected to 'iface'
	 */
	public List<Interface> getDirectlyConnected(Interface iface) {
		List<Interface> interfaces = new ArrayList<Interface>();
		
		for(Interface i : _interfaces)
			if(areDirectlyConnected(iface, i))
				interfaces.add(i);
		
		return interfaces;
	}
	
	
	/** Given an interface 'iface', return all of the nodes that are connected to it.  This
	 * includes both direct and indirect connections.
	 * @param iface the interface whose connections are requested
	 * @return a list of interfaces connected to 'iface'
	 */
	public List<Interface> getConnectedInterfaces(Interface iface) {
		_visitedNodes = new ArrayList<Interface>();
		populateNodesConnectedTo(iface);
		return _visitedNodes;
	}
	
	/** Given an interface 'iface', return all of the nodes that are connected to it.  This
	 * includes both direct and indirect connections.
	 * @param iface the interface whose connections are requested
	 * @return a list of interfaces connected to 'iface'
	 */
	public InterfaceGroup getConnectedInterfaceGroup(Interface iface) {
		_visitedNodes = new ArrayList<Interface>();
		populateNodesConnectedTo(iface);
		return new InterfaceGroup(_visitedNodes);
	}
	
	/** This function just keeps going through a list of connected nodes recursively to find
	 * all nodes connected to a specific node.  The end result is that _visitedNodes is populated.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @param visited A list of the visited nodes so far
	 * @return true if i1 and i2 are connected, false otherwise.
	 */
	public void populateNodesConnectedTo(Interface iface) {
		
		// If we have already been here before, let's escape...
		if(_visitedNodes.contains(iface))
			return;
		_visitedNodes.add(iface);	// We have now visited i1
		
		List<Interface> directlyConnected = getDirectlyConnected(iface);
		for(Interface i : directlyConnected)
			populateNodesConnectedTo(i);
		
		return;
	}

	/** This will scan through the entire connectivity graph and return groups of interfaces.
	 * @return groups of interfaces that are connected.
	 */
	public List<InterfaceGroup> getInterfaceGroups() {
		List<InterfaceGroup> groups = new ArrayList<InterfaceGroup>();
		
		// For all interfaces, 
		for(Interface iface : _interfaces)
			if(!nodeContainedByAGroup(iface, groups))
				groups.add(getConnectedInterfaceGroup(iface));
		
		return groups;
	}
	
	/** Returns whether or not an interface belongs in any of the groups passed in the list of groups.
	 * @param iface  The interface in question.
	 * @param groups The groups in question.
	 * @return true if 'iface' belongs in a group in the list of groups.
	 */
	public boolean nodeContainedByAGroup(Interface iface, List<InterfaceGroup> groups) {
		for(InterfaceGroup grp : groups)
			if(grp.hasInterface(iface))
				return true;
		return false;
	}
	
	/** Returns a string representation of a set of list of interfaces by MAC address
	 * @param interfaces the interfaces to be printed
	 * @return the string representation
	 */
	public static String interfaceListToString(List<Interface> interfaces) {
		Collections.sort(interfaces,Interface.macsort);
		String ifaces = "{";
		for(Interface i : interfaces)
			ifaces += i._MAC + ",";
		return ifaces.substring(0, ifaces.length()-1) + "}";
	}
}
