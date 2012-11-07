package com.gnychis.awmon.HardwareHandlers;

import java.util.concurrent.Semaphore;

import android.content.Context;

import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.RadioScanners.BluetoothRadioScanner;
import com.gnychis.awmon.RadioScanners.RadioScanner;
import com.gnychis.awmon.RadioScanners.WifiRadioScanner;
import com.gnychis.awmon.RadioScanners.ZigBeeRadioScanner;

abstract public class InternalRadio {
	
	public Context _parent;
	public RadioScanner _device_scanner;
	
	WirelessInterface.Type _type;
	
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
		
		switch(deviceType()) {
			case ZigBee:
				_device_scanner = new ZigBeeRadioScanner();
				break;
			case Wifi:
				_device_scanner = new WifiRadioScanner();
				break;
			case Bluetooth:
				_device_scanner = new BluetoothRadioScanner();
				break;
		}
		
		_device_scanner.execute(this);
		return true;
	}
	
	public InternalRadio(WirelessInterface.Type type) {
		_type = type;
		_state_lock = new Semaphore(1,true);
		_state = State.IDLE;
	}
	
	public WirelessInterface.Type deviceType() { return _type; }
	public InternalRadio.State getState() {
		return _state;
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
