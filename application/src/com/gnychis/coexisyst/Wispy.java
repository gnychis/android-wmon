package com.gnychis.coexisyst;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import android.os.Environment;
import android.util.Log;

public class Wispy {
	public static final int WISPY_CONNECT = 0;
	public static final int WISPY_DISCONNECT = 1;
	public static final int WISPY_POLL = 2;
	public static final int WISPY_POLL_FAIL = 3;
	public static final int WISPY_POLL_THREAD = 4;
	
	File _root;
	FileOutputStream _wispyOut;
	PrintStream _wispyPrint;
	
	boolean _wispy_connected;
	boolean _wispy_polling;
	boolean _wispy_reset_max;
	boolean _wispy_save_scans;
	int _wispy_poll_count;
	int _maxresults[];
	
	public Wispy(){
        _wispy_connected=false;
        _wispy_polling=false;
        _wispy_reset_max=false;
        _wispy_poll_count=0;
        _wispy_save_scans=false;
        _maxresults = new int[256];
        for(int i=0; i<256; i++)
        	_maxresults[i]=-200;
        
        // For writing to SD card
        try {
	        _root = Environment.getExternalStorageDirectory();
	        _wispyOut = new FileOutputStream(new File(_root, "wispy.dat"));
	        _wispyPrint = new PrintStream(_wispyOut);
        } catch(Exception e) {
        	//Log.e(TAG, "Error opening output file", e);
        }
	}

}
