package com.gnychis.awmon.Scanners;

import java.util.ArrayList;

import android.content.Intent;
import android.os.AsyncTask;

import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;
import com.gnychis.awmon.HardwareHandlers.Wifi;
import com.gnychis.awmon.HardwareHandlers.ZigBee;
import com.gnychis.awmon.ScanResultParsers.BluetoothResultParser;
import com.gnychis.awmon.ScanResultParsers.ScanResultParser;
import com.gnychis.awmon.ScanResultParsers.WifiResultParser;
import com.gnychis.awmon.ScanResultParsers.ZigBeeResultParser;

// The whole purpose of this abstract class is that for every network device (e.g., 802.11 radio), 
// you should have a device scanner scanner for it.  This is a thread which can be spawned which
// will scan all bands for devices.  
abstract public class Scanner extends AsyncTask<InternalRadio, Integer, ArrayList<Interface> > {

	InternalRadio _hw_device;
	ScanResultParser _result_parser;
	public static final String DEVICE_SCAN_RESULT = "awmon.devicescanner.device_scan_result";
	
	public Scanner(Class<?> hardwareClass) {
		if(hardwareClass == ZigBee.class)
			_result_parser = new ZigBeeResultParser();
		if(hardwareClass == Wifi.class)
			_result_parser = new WifiResultParser();
		if(hardwareClass == Bluetooth.class)
			_result_parser = new BluetoothResultParser();
	}
	
    @Override
    protected void onPostExecute(ArrayList<Interface> interfaces) {    		
		_hw_device.stateChange(InternalRadio.State.IDLE);		// change the state to IDLE
		
		/* while(_scan_thread.getStatus()!=AsyncTask.Status.FINISHED)
			trySleep(100); */
		
		// Now, construct a broadcast that includes the hardware that it came from and
		// the ArrayList of all the devices!
		Intent i = new Intent();
		i.setAction(DEVICE_SCAN_RESULT);
		i.putExtra("hwType", _hw_device.deviceType());
		i.putExtra("result", new ScanResult(_hw_device.deviceType(), interfaces));
		_hw_device._parent.sendBroadcast(i);
    }
}
