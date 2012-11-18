package com.gnychis.awmon.InterfaceMerging;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Parcel;
import android.os.Parcelable;

import com.gnychis.awmon.DeviceAbstraction.Device;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.InterfaceGroup;
import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;
import com.gnychis.awmon.InterfaceMerging.MergeHeuristic.MergeStrength;


/** The purpose of this class is to provide an implementation of a graph where
 * nodes are connected via bidirectional edges, and the implementation provides the
 * ability to find groups of nodes connected together.  This allows us to build graphs
 * of interfaces that we believe are connected to the same physical device, and easily
 * retrieve them after a set of heuristics which add or break edges. 
 * 
 * @author George Nychis (gnychis)
 */
public class InterfaceConnectivityGraph implements Parcelable {

	private List<Interface> _nodes;					// Each interface is a node
	private Map<String,Boolean> _graph;				// If a pair of interfaces have an active link between them
	private Map<String,Integer> _negativeWeight;	// Negative weight for each pair of interfaces
	private Map<String,Integer> _positiveWeight;	// Positive weight for each pair of interfaces
	
	private List<Interface> _visitedNodes;
	
	public static final int INVALID_WEIGHT_VAL = -10000;
	
	/** Creates a new interface connectivity graph for use in merging interfaces
	 * in to devices.
	 * @param interfaces
	 */
	public InterfaceConnectivityGraph(List<Interface> interfaces) {
		_nodes = interfaces;
		_graph = new HashMap<String,Boolean>();
		_negativeWeight = new HashMap<String,Integer>();
		_positiveWeight = new HashMap<String,Integer>();
		initGraph();
	}
	
	
	/** Initializes the graph where all of the interfaces are unconnected.  Note:
	 * _interfaces MUST be populated, otherwise this will fail.
	 * @return
	 */
	private void initGraph() {
		for(Interface i1 : _nodes) {
			for(Interface i2 : _nodes) {
				_graph.put(hashKey(i1,i2), false);
				setPositiveWeight(i1,i2,0);
				setNegativeWeight(i1,i2,0);
			}
		}
	}
	
	/** The purpose of this function is to take a graph and a series of classifications that were output from one of the heuristics,
	 * and then modify the graph (connect/disconnect edges) based on it.
	 * @param graph
	 * @return
	 */
	public void applyHeuristicClassification(Map<InterfacePair,MergeStrength> classifications) {
		
		// For all hashKeys of all the interface pairs, update the links
		for(InterfacePair pair : classifications.keySet()) {
			MergeStrength strength = classifications.get(pair);
			switch(strength) {
			
				/******************************** LIKELY ********************************/
				case LIKELY:
					incrementPositiveWeight(pair);
					connect(pair);
				break;
				
				/******************************* UNLIKELY ********************************/
				case UNLIKELY:
					incrementNegativeWeight(pair);
				break;
				
				/***************************** UNDETERMINED ******************************/
				case UNDETERMINED:
				break;
			}
		}
	}
	
	
	/**  Gets the total weight on a link between two interfaces by summing the positive
	 * and negative weights
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return the total weight, INVALID_WEIGHT_VAL if link-pair is invalid
	 */
	public int getWeight(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return INVALID_WEIGHT_VAL;
		return positiveWeight(i1, i2) + negativeWeight(i1, i2);
	}
	
	/**  Gets the total weight on a link between two interfaces by summing the positive
	 * and negative weights
	 * @param ifacePair the interface pair
	 * @return the total weight, INVALID_WEIGHT_VAL if link-pair is invalid
	 */
	public int getWeight(InterfacePair ifacePair) {
		return getWeight(ifacePair.getLeft(), ifacePair.getRight());
	}
	
	/** Get the positive weight value between two interfaces on their link.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return positive weight, INVALID_WEIGHT_VAL if invalid
	 */
	public int positiveWeight(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return INVALID_WEIGHT_VAL;
		return _positiveWeight.get(hashKey(i1,i2));
	}
	
	/** Get the positive weight value between two interfaces on their link.
	 * @param ifacePair the interface pair
	 * @return positive weight, INVALID_WEIGHT_VAL if invalid
	 */
	public int positiveWeight(InterfacePair ifacePair) {
		return positiveWeight(ifacePair.getLeft(), ifacePair.getRight());
	}
	
	/** Get the negative weight value between two interfaces on their link.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return negative weight, INVALID_WEIGHT_VAL if invalid
	 */
	public int negativeWeight(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return INVALID_WEIGHT_VAL;
		return _negativeWeight.get(hashKey(i1,i2));
	}
	
	/** Get the negative weight value between two interfaces on their link.
	 * @param ifacePair the interface pair
	 * @return negative weight, INVALID_WEIGHT_VAL if invalid
	 */
	public int negativeWeight(InterfacePair ifacePair) {
		return negativeWeight(ifacePair.getLeft(), ifacePair.getRight());
	}
	
	/** This function allows us to increment the positive weight on a link in the graph
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return true if we were able to increment it, false otherwise
	 */
	public boolean incrementPositiveWeight(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return false;
		int curr_val = _positiveWeight.get(hashKey(i1,i2));
		_positiveWeight.put(hashKey(i1,i2), curr_val+1);
		return true;
	}
	
	/** This function allows us to increment the positive weight on a link in the graph
	 * @param ifacePair the interface pair
	 * @return true if we were able to increment it, false otherwise
	 */
	public boolean incrementPositiveWeight(InterfacePair ifacePair) {
		return incrementPositiveWeight(ifacePair.getLeft(), ifacePair.getRight());
	}
	
	/** This function allows us to increment the negative weight on a link in the graph
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return true if we were able to increment it, false otherwise
	 */
	public boolean incrementNegativeWeight(Interface i1, Interface i2) {
		if(!validPair(i1, i2))
			return false;
		int curr_val = _negativeWeight.get(hashKey(i1,i2));
		_negativeWeight.put(hashKey(i1,i2), curr_val+1);
		return true;
	}
	
	/** This function allows us to increment the negative weight on a link in the graph
	 * @param ifacePair the interface pair
	 * @return true if we were able to increment it, false otherwise
	 */
	public boolean incrementNegativeWeight(InterfacePair ifacePair) {
		return incrementNegativeWeight(ifacePair.getLeft(), ifacePair.getRight());
	}
	
	/** This function allows us to easily set the positive weight on a link in the graph
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return true if we were able to set the weight, false otherwise
	 */
	private boolean setPositiveWeight(Interface i1, Interface i2, int value) {
		if(!validPair(i1, i2))
			return false;
		_positiveWeight.put(hashKey(i1,i2), value);
		return true;
	}
	
	
	/** This function allows us to easily set the positive weight on a link in the graph
	 * @param ifacePair the interface pair
	 * @return true if we were able to set the weight, false otherwise
	 */
	private boolean setPositiveWeight(InterfacePair ifacePair, int value) {
		return setPositiveWeight(ifacePair.getLeft(), ifacePair.getRight(), value);
	}
	
	/** This function allows us to easily set the negative weight on a link in the graph
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return true if we were able to set the weight, false otherwise
	 */
	private boolean setNegativeWeight(Interface i1, Interface i2, int value) {
		if(!validPair(i1, i2))
			return false;
		_negativeWeight.put(hashKey(i1,i2), value);
		return true;
	}
	
	/** This function allows us to easily set the negative weight on a link in the graph
	 * @param ifacePair the interface pair
	 * @return true if we were able to set the weight, false otherwise
	 */
	private boolean setNegativeWeight(InterfacePair ifacePair, int value) {
		return setNegativeWeight(ifacePair.getLeft(), ifacePair.getRight(), value);
	}
	
	/** Returns the hash key for the two interfaces in our connectivity graph.  Since
	 * the graph is bidirectional, we make sure that the ordering does not matter.
	 * @param i1 Interface 1
	 * @param i2 Interface 2
	 * @return the key to _graph for the two interfaces.  Returns null if pair doesn't exist.
	 */
	public static String hashKey(Interface i1, Interface i2) {
		String key=null;
		if(i1.hashCode()<i2.hashCode())
			key = Integer.toString(i1.hashCode()) + "," + Integer.toString(i2.hashCode());
		else
			key = Integer.toString(i2.hashCode()) + "," + Integer.toString(i1.hashCode());
		return key;
	}
	
	/** Returns the hash key for the two interfaces using an InterfacePair in our connectivity graph.  Since
	 * the graph is bidirectional, we make sure that the ordering does not matter.
	 * @param ifacePair the interface pair
	 * @return the key to _graph for the two interfaces.  Returns null if pair doesn't exist.
	 */
	public static String hashKey(InterfacePair ifacePair) {
		String key=null;
		if(ifacePair.getLeft().hashCode()<ifacePair.getRight().hashCode())
			key = Integer.toString(ifacePair.getLeft().hashCode()) + "," + Integer.toString(ifacePair.getRight().hashCode());
		else
			key = Integer.toString(ifacePair.getRight().hashCode()) + "," + Integer.toString(ifacePair.getLeft().hashCode());
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
	
	/** This sets the value of the connection between two interfaces in the graph.
	 * @param ifacePair the interface pair
	 * @param value true for connected, false for unconnected.
	 * @return true of the connection succeeded, false otherwise
	 */
	public boolean setConnected(InterfacePair ifacePair, boolean value) { 
		return setConnected(ifacePair.getLeft(), ifacePair.getRight(), value); 
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
	
	/** This function connects two interfaces together in our graph.
	 * @param ifacePair the interface pair
	 * @return returns true if they were connected, false if the connection failed.
	 */
	public boolean connect(InterfacePair ifacePair) {
		return connect(ifacePair.getLeft(), ifacePair.getRight());
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
	
	/** This function disconnects two interfaces together in our graph.
	 * @param ifacePair the interface pair
	 * @return returns true if they were disconnected, false if the connection failed.
	 */
	public boolean disconnect(InterfacePair ifacePair) {
		return disconnect(ifacePair.getLeft(), ifacePair.getRight());
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
	
	/** This returns whether or not the pair of interfaces is valid in our graph.  I.e.,
	 * both must exist in our graph to be valid.
	 * @param ifacePair the interface pair
	 * @return true if valid, false if not.
	 */
	public boolean validPair(InterfacePair ifacePair) {
		return validPair(ifacePair.getLeft(), ifacePair.getRight());
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
	
	/** This function will return true if the two interfaces are connected in our
	 * graph, false if otherwise.  There is no ordering here, it is bidirectional.
	 * @param ifacePair the interface pair
	 * @return true if i1 and i2 are connected, false otherwise.
	 */
	public boolean areConnected(InterfacePair ifacePair) {
		return areConnected(ifacePair.getLeft(), ifacePair.getRight());
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
	
	/** This function will return true if the two interfaces are directly connected in our
	 * graph, false if otherwise.  There is no ordering here, it is bidirectional.
	 * @param ifacePair the interface pair
	 * @return true if i1 and i2 are connected, false otherwise.
	 */
	public boolean areDirectlyConnected(InterfacePair ifacePair) {
		return areDirectlyConnected(ifacePair.getLeft(), ifacePair.getRight());
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
	
	/** This function which will be used via recursion to try and get all of the connected nodes,
	 *  passing along a "visited" variable to avoid loops.
	 * @param ifacePair the interface pair
	 * @param visited A list of the visited nodes so far
	 * @return true if i1 and i2 are connected, false otherwise.
	 */
	public boolean checkIndirectConnection(InterfacePair ifacePair) {
		return checkIndirectConnection(ifacePair.getLeft(), ifacePair.getRight());
	}
	
	/** This returns all interfaces that are directly connected to the given interface.
	 * This does not include itself.
	 * @param iface The interface to find those directly connected to it.
	 * @return a list of interfaces directly connected to 'iface'
	 */
	public List<Interface> getDirectlyConnected(Interface iface) {
		List<Interface> interfaces = new ArrayList<Interface>();
		
		for(Interface i : _nodes)
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
		for(Interface iface : _nodes)
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
	
    
    /** This will return all interfaces in the graph that are of a certain type or types.
     * @param types The types of interfaces that should be returned.
     * @return a list of interfaces that belong to 'types'
     */
    public List<Interface> getInterfacesOfTypes(List<? extends Class<? extends InternalRadio>> types) {
    	List<Interface> interfaces = new ArrayList<Interface>();
    	
    	for(Interface iface : _nodes)
    		if(types.contains(iface._type))
    			interfaces.add(iface);
    	
    	return interfaces;
    }
    
    /** This will return all interfaces in the graph that are of a certain type.
     * @param type The type of interfaces that should be returned.
     * @return a list of interfaces that are of type 'type'
     */
    @SuppressWarnings("unchecked")
    public List<Interface> getInterfacesOfType(Class<? extends InternalRadio> type) {    	
    	return getInterfacesOfTypes(Arrays.asList(type));
    }
    
    /** Returns a list of all interface pairs with no redundancy.
     * @return a list of all interface pairs.
     */
    public List<InterfacePair> getAllInterfacePairs() {
    	Map<String,InterfacePair> pairMapping= new HashMap<String,InterfacePair>();
    	
    	for(Interface i1 : _nodes)		// For all nodes (interfaces)
    		for(Interface i2 : _nodes)	// ... and all other interfaces
    			if(i1 != i2 && !pairMapping.containsKey(hashKey(i1,i2)))  // Create a pair 
    				pairMapping.put(hashKey(i1,i2), new InterfacePair(i1,i2));
    	
    	return new ArrayList<InterfacePair>(pairMapping.values());
    }
    
    /** Returns a list of all interface pairs with no redundancy.
     * @return a list of all interface pairs.
     */
    public List<InterfacePair> getInterfacePairs(List<Interface> interfaces) {
    	Map<String,InterfacePair> pairMapping= new HashMap<String,InterfacePair>();
    	
    	for(Interface i1 : interfaces)		// For all nodes (interfaces)
    		for(Interface i2 : interfaces)	// ... and all other interfaces
    			if(i1 != i2 && !pairMapping.containsKey(hashKey(i1,i2)))  // Create a pair 
    				pairMapping.put(hashKey(i1,i2), new InterfacePair(i1,i2));
    	
    	return new ArrayList<InterfacePair>(pairMapping.values());
    }

    /** Get all interface pairs of certain types that belong to the graph.
     * @param types the type that the interfaces must match
     * @return a list of InterfacePair items
     */
    public List<InterfacePair> getInterfacePairsOfTypes(List<? extends Class<? extends InternalRadio>> types) {
    	List<Interface> interfaces = getInterfacesOfTypes(types);
    	return getInterfacePairs(interfaces);
    }
    
    
    /** Get all interface pairs of a certain type that belong to the graph.
     * @param type the type that the interfaces must match
     * @return a list of InterfacePair items
     */
    @SuppressWarnings("unchecked")
    public List<InterfacePair> getInterfacePairsOfType(Class<? extends InternalRadio> type) {
    	return getInterfacePairsOfTypes(Arrays.asList(type));
    }
	
	/** The purpose of this function is to take a connectivity graph of interfaces,
	 * and from it create a list of devices.
	 * 
	 * @return a list of devices from the connectivity graph.
	 */
	public ArrayList<Device> devicesFromConnectivityGraph() {
		ArrayList<Device> devices = new ArrayList<Device>();
		
		// Get a list of InterfaceGroup from the graph
		List<InterfaceGroup> interfaceGroups = getInterfaceGroups();
		
		// For each interface group, create a device
		for(InterfaceGroup group : interfaceGroups) {
			List<Interface> interfaces = group.getInterfaces();
			devices.add(new Device(interfaces));
		}
		
		return devices;
	}
	
    // ********************************************************************* //
    // This code is to make this class parcelable and needs to be updated if
    // any new members are added to the Device class
    // ********************************************************************* //
    public int describeContents() {
        return this.hashCode();
    }
    
    public static final Creator<InterfaceConnectivityGraph> CREATOR = new Creator<InterfaceConnectivityGraph>() {
        public InterfaceConnectivityGraph createFromParcel(Parcel source) { return new InterfaceConnectivityGraph(source); }
        public InterfaceConnectivityGraph[] newArray(int size) { return new InterfaceConnectivityGraph[size]; }
    };

    public void writeToParcel(Parcel dest, int parcelableFlags) {
    	dest.writeList(_nodes);
    	
    	// To make this parcelable, write the graph size and then one-by-one pump the
    	// keys and values through
    	dest.writeInt(_graph.size());
    	for (String key : _graph.keySet()) {
    		dest.writeString(key);
    		dest.writeInt( _graph.get(key) ? 1 : 0 );
    		dest.writeInt(_positiveWeight.get(key));
    		dest.writeInt(_negativeWeight.get(key));
    	}
    	
    	// NOTE: we do NOT write _visitedNodes.  It carries no persistent state.  It is a temp variable.
    }
    
    private InterfaceConnectivityGraph(Parcel source) {
    	_nodes = new ArrayList<Interface>();
    	source.readList(_nodes, this.getClass().getClassLoader());
    	
    	// Create a new HashMap and read the number of elements in the graph
    	int graph_size = source.readInt();
    	_graph = new HashMap<String,Boolean>();
    	_positiveWeight = new HashMap<String,Integer>();
    	_negativeWeight = new HashMap<String,Integer>();
    	while(graph_size>0) {
    		String key = source.readString();
    		boolean val = (source.readInt()==1) ? true : false;
    		_graph.put(key, val);
    		_positiveWeight.put(key,source.readInt());
    		_negativeWeight.put(key,source.readInt());
    	}
    	
    	_visitedNodes=null;
    }
}
