package com.gnychis.awmon.DeviceHandlers;

import java.util.concurrent.Semaphore;

import android.content.Context;

import com.gnychis.awmon.Core.Radio;
import com.gnychis.awmon.RadioScanners.BluetoothDeviceScanner;
import com.gnychis.awmon.RadioScanners.DeviceScanner;
import com.gnychis.awmon.RadioScanners.WifiDeviceScanner;
import com.gnychis.awmon.RadioScanners.ZigBeeDeviceScanner;

abstract public class InternalRadio {
	
	public Context _parent;
	public DeviceScanner _device_scanner;
	
	Radio.Type _type;
	
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
				_device_scanner = new ZigBeeDeviceScanner();
				break;
			case Wifi:
				_device_scanner = new WifiDeviceScanner();
				break;
			case Bluetooth:
				_device_scanner = new BluetoothDeviceScanner();
				break;
		}
		
		_device_scanner.execute(this);
		return true;
	}
	
	public InternalRadio(Radio.Type type) {
		_type = type;
		_state_lock = new Semaphore(1,true);
		_state = State.IDLE;
	}
	
	public Radio.Type deviceType() { return _type; }
	public InternalRadio.State getState() {
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
