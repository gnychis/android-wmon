package com.gnychis.awmon.DeviceHandlers;

import java.util.concurrent.Semaphore;

import android.content.Context;

import com.gnychis.awmon.Scanners.DeviceScanner;
import com.gnychis.awmon.Scanners.ZigBeeScanner;

abstract public class HardwareDevice {
	
	public Context _parent;
	public DeviceScanner _device_scanner;
	
	HardwareDevice.Type _type;
	public enum Type {		// A List of possible scans to handle
		Wifi,
		ZigBee,
		Bluetooth,
	}
	
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
				_device_scanner = new ZigBeeScanner();
				break;
		}
		
		_device_scanner.execute(this);
		return true;
	}
	
	public HardwareDevice(HardwareDevice.Type type) {
		_type = type;
		_state_lock = new Semaphore(1,true);
		_state = State.IDLE;
	}
	
	public HardwareDevice.Type deviceType() { return _type; }
	public HardwareDevice.State getState() {
		return _state;
	}
	
	// Attempts to change the current state, will return
	// the state after the change if successful/failure
	public boolean stateChange(State s) {
		boolean res = false;
		if(_state_lock.tryAcquire()) {
			try {
				
				// Can add logic here to only allow certain state changes
				// Given a _state... then...
				switch(_state) {
				
				// From the IDLE state, we can go anywhere...
				case IDLE:
					_state = s;
					res = true;
				break;
				
				// We can go to idle, or ignore if we are in a
				// scan already.
				case SCANNING:
					if(s==State.IDLE) {  // cannot go directly to IDLE from SCANNING
						_state = s;
						res = true;
					} else if(s==State.SCANNING) {  // ignore an attempt to switch in to same state
						res = false;
					} 
				break;
				
				default:
					res = false;
				}
				
			} finally {
				_state_lock.release();
			}
		} 		
		
		return res;
	}
}
