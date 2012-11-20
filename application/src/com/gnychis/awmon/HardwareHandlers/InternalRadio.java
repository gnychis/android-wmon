package com.gnychis.awmon.HardwareHandlers;

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.util.Log;

import com.gnychis.awmon.InterfaceScanners.BluetoothScanner;
import com.gnychis.awmon.InterfaceScanners.LANScanner;
import com.gnychis.awmon.InterfaceScanners.InterfaceScanner;
import com.gnychis.awmon.InterfaceScanners.WifiScanner;
import com.gnychis.awmon.InterfaceScanners.ZigBeeScanner;

abstract public class InternalRadio {
	
	public Context _parent;
	public InterfaceScanner _interfaceScanner;
		
	State _state;
	Semaphore _state_lock;
	public enum State {
		IDLE,
		SCANNING,
	}

	abstract public boolean isConnected();
	
	public boolean startDeviceScan() {
		if(!stateChange(State.SCANNING))
			return false;
		
		if(this.getClass() == ZigBee.class)
			_interfaceScanner = new ZigBeeScanner(_parent);
		if(this.getClass() == Wifi.class)
			_interfaceScanner = new WifiScanner(_parent);
		if(this.getClass() == Bluetooth.class)
			_interfaceScanner = new BluetoothScanner(_parent);
		if(this.getClass() == LAN.class)
			_interfaceScanner = new LANScanner(_parent);
		
		if(_interfaceScanner==null)
			return false;
		
		_interfaceScanner.execute(this);
		return true;
	}
	
	public InternalRadio(Context c) {
		_parent = c;
		_state_lock = new Semaphore(1,true);
		_state = State.IDLE;
	}
	
	public InternalRadio.State getState() {
		return _state;
	}
	
	public Class<?> deviceType() { return this.getClass(); };
	public static Class<?> deviceType(String name) { 
        try {
        return Class.forName(name);
        } catch(Exception e) { Log.e("Interface", "Error getting class in Interface parcel"); }
        return null;
	}
	
	// Some methods the child class can override
	public void leavingIdleState() { }
	public void enteringIdleState() { }
	
	// Attempts to change the current state, will return
	// the state after the change if successful/failure
	public boolean stateChange(State s) {
		boolean res = false;
		if(_state_lock.tryAcquire()) {
			try {
				
				// Can add logic here to only allow certain state changes
				// Given a _state... then...
				switch(_state) {
			
				case IDLE:	// Given the IDLE state...
					if(s==State.IDLE) {
						res=false;  // Going from IDLE to IDLE should be res=false, it's not a real state change.
					} else {
						_state = s;	// anywhere else is acceptable though....
						res = true;
						leavingIdleState();
					}
				break;
				
				case SCANNING:	// Given the SCANNING state...
					if(s==State.IDLE) {
						_state = s;
						res = true;
					} else if(s==State.SCANNING) {  // ignore an attempt to switch in to same state
						res = false;
					} 
				break;
				
				default:
					res = false;
				}
				
				// If res is true, we had a real state change.  (false if going from same state to same state).
				if(res && _state==State.IDLE)
					enteringIdleState();
								
			} finally {
				_state_lock.release();
			}
		} 		
		
		return res;
	}
}
