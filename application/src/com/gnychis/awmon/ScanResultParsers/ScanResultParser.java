package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import com.gnychis.awmon.NetDevDefinitions.Device;

abstract public class ScanResultParser {

	abstract public ArrayList<Device> returnDevices(ArrayList<Object> scanResult);
	
}
