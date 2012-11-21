package com.gnychis.awmon.NameResolution;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;

abstract public class NameResolver extends AsyncTask<ArrayList<Interface>, Integer, ArrayList<Interface> > {
	
	public static final String NAME_RESOLVER_RESPONSE = "awmon.nameresolver.name_resolver_response";
	
	NameResolutionManager _nr_manager;
	Context _parent;
	
	// This is an array which will keep track of the support hardware types for each name resolver
	public List<Class<? extends InternalRadio>> _supportedInterfaceTypes;
	
	public NameResolver(NameResolutionManager nrm, List<Class<? extends InternalRadio>> supportedInterfaces) {
		_supportedInterfaceTypes = supportedInterfaces;
		_nr_manager = nrm;
		_parent = _nr_manager._parent;
	}
	
	@Override
	protected ArrayList<Interface> doInBackground( ArrayList<Interface> ... params )
	{
		ArrayList<Interface> interfaces = params[0];
		ArrayList<Interface> supported = new ArrayList<Interface>();
		ArrayList<Interface> unsupported = new ArrayList<Interface>();
		ArrayList<Interface> merged = new ArrayList<Interface>();
		
		for(Interface iface : interfaces) {
			if(_supportedInterfaceTypes.contains(iface._type))
				supported.add(iface);
			else
				unsupported.add(iface);
		}
		
		supported = resolveSupportedInterfaces(supported);	// This is what each name resolver implements
		
		// Merge the newly resolved supported devices with the unsupported and return.
		merged.addAll(supported);
		merged.addAll(unsupported);
		return merged;
	}
	
    @Override
    protected void onPostExecute(ArrayList<Interface> interfaces) {    		
		Intent i = new Intent();
		i.setAction(NAME_RESOLVER_RESPONSE);
		i.putExtra("resolver", this.getClass());
		i.putExtra("result", interfaces);
		_nr_manager._parent.sendBroadcast(i);
    }
	
	abstract public ArrayList<Interface> resolveSupportedInterfaces(ArrayList<Interface> supportedRadios);
}
