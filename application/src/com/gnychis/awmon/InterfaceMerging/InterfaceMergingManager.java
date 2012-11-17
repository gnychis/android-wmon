package com.gnychis.awmon.InterfaceMerging;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.NameResolution.NameResolver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

@SuppressWarnings("unchecked")
public class InterfaceMergingManager {

	private static final String TAG = "InterfaceMergingManager";
	private static final boolean VERBOSE = true;
	
	public static final String INTERFACE_MERGING_REQUEST = "awmon.interfacemerging.interface_merging_request";
	public static final String INTERFACE_MERGING_RESPONSE = "awmon.interfacemerging.interface_merging_request";
	
	Context _parent;
	Queue<Class<?>> _pendingResults;
	Stack<Class<?>> _heuristicQueue;	// These should be kept in a heirarchy such that
										// it would be OK if the next resolver overwrote previous.
	State _state;
	public enum State {
		IDLE,
		MERGING,
	}
	
	public InterfaceMergingManager(Context parent) {
		_parent = parent;
		_state = State.IDLE;
        
        _parent.registerReceiver(incomingEvent, new IntentFilter(MergeHeuristic.MERGE_HEURISTIC_RESPONSE));
        _parent.registerReceiver(incomingEvent, new IntentFilter(INTERFACE_MERGING_REQUEST));
	}
	
	private void registerHeuristic(List<Class<? extends MergeHeuristic>> heuristics) {
		_heuristicQueue = new Stack<Class<?>>();
		_pendingResults = new LinkedList < Class<?> >();
		for(Class<?> heuristic : heuristics) {
			debugOut("Registering the following interface merging heuristic: " + heuristic.getName());
			_heuristicQueue.push(heuristic);
			_pendingResults.add(heuristic);
		}
	}
	
    private BroadcastReceiver incomingEvent = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	
        }
    };
    
	public boolean triggerNextHeuristic(InterfaceConnectivityGraph graph) {
		
		return true;
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
