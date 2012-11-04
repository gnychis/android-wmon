package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;

abstract public class ScanResultParser {

	abstract public <T extends Object> ArrayList<WirelessInterface> returnDevices(ArrayList<T> scanResult);
	
}
