package com.gnychis.awmon.ScanResultParsers;

import java.util.ArrayList;

import com.gnychis.awmon.Core.Radio;

abstract public class ScanResultParser {

	abstract public <T extends Object> ArrayList<Radio> returnDevices(ArrayList<T> scanResult);
	
}
