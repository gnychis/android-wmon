package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import android.content.Context;

import com.gnychis.awmon.DeviceAbstraction.Interface;

// The goal of this base class is to provide a means for results (e.g., packets) to be parsed to detect
// wireless or wired interfaces in the environment.
abstract public class ScanResultParser {
	
	Context _parent;
	
	public ScanResultParser(Context c) {
		_parent=c;
	}

	abstract public <T extends Object> ArrayList<Interface> returnInterfaces(ArrayList<T> scanResult);
	
}
