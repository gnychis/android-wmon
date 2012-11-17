package com.gnychis.awmon.InterfaceMerging;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;

/** This is an abstract class to help write heuristics which are run in background threads that try to resolve
 * whether or not pairs of interfaces belong to the same physical device.  What is passed between them is a
 * connectivity graph.  Each heuristic can modify this graph by adding edges between pairs of interfaces (signifying
 * that the heuristic believes they belong to the same device), or by removing edges (strong evidence against it).
 * 
 * It extends AsyncTask to force these heuristics to run in background threads.  The graph is passed between the
 * heuristics using broadcasts.
 * 
 * @author George Nychis (gnychis)
 */
public abstract class MergeHeuristic extends AsyncTask<InterfaceConnectivityGraph, Integer, InterfaceConnectivityGraph > {

	public static final String MERGE_HEURISTIC_RESPONSE = "awmon.interfacemerging.merge_heuristic_response";
	
	public enum MergeStrength {
		LIKELY,
		UNLIKELY,
		UNDETERMINED,
	}
	
	Context _parent;		// Need the parent to do things like send broadcasts.
	public List<Class<? extends InternalRadio>> _supportedInterfaceTypes;  // The types of interfaces the heuristic supports
	
	public MergeHeuristic(Context c, List<Class<? extends InternalRadio>> supportedInterfaces) {
		_supportedInterfaceTypes = supportedInterfaces;
		_parent = c;
	}
	
	@Override
	protected InterfaceConnectivityGraph doInBackground( InterfaceConnectivityGraph ... params )
	{
		InterfaceConnectivityGraph graph = params[0];
		
		// Get all interface pairs for the supported types, pass it to the classifier (heuristic)
		List<InterfacePair> supportedPairs = graph.getInterfacePairsOfTypes(_supportedInterfaceTypes);
		Map<InterfacePair,MergeStrength> classifications = classifyInterfacePairs(supportedPairs);
		
		// Now, apply the classification done by the heuristic to the graph
		graph.applyHeuristicClassification(classifications);
		
		return graph;
	}
	
    @Override
    protected void onPostExecute(InterfaceConnectivityGraph graph) {    		
		Intent i = new Intent();
		i.setAction(MERGE_HEURISTIC_RESPONSE);
		i.putExtra("heuristic", this.getClass());
		i.putExtra("result", graph);
		_parent.sendBroadcast(i);
    }
	
	/** This should be overriden by the child class extending MergeHeuristic. It should create a Map<InterfacePair,MergeStrength>,
	 * such that for each InterfacePair, it should add an entry in the Map with the appropriate
	 * MergeStrength value from the heuristic.  These will then be used to connect/disconnect edges in the graph.
	 * @param pairs the interface pairs that should be classified by the heuristic
	 * @return a map of each InterfacePair to the merge strength
	 */
	abstract public Map<InterfacePair,MergeStrength> classifyInterfacePairs(List<InterfacePair> pairs);
}
