package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import com.gnychis.awmon.DeviceAbstraction.WirelessRadio;

abstract public class ScanResultParser {

	abstract public <T extends Object> ArrayList<WirelessRadio> returnDevices(ArrayList<T> scanResult);
	
}
