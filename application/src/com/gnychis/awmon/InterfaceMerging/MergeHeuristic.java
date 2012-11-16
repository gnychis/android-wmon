package com.gnychis.awmon.InterfaceMerging;

import java.util.ArrayList;

import android.os.AsyncTask;

import com.gnychis.awmon.DeviceAbstraction.Interface;

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
public abstract class MergeHeuristic extends AsyncTask<ArrayList<Interface>, Integer, ArrayList<Interface> > {

	public static final String NAME_RESOLVER_RESPONSE = "awmon.interfacemerging.name_resolver_response";

}
