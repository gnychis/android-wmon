package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import com.gnychis.awmon.NetDevDefinitions.Device;

abstract public class ScanResultParser {

	abstract ArrayList<Device> returnDevices(ArrayList<Object> scanResult);
	
}
