package com.gnychis.coexisyst;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Semaphore;

import android.os.Environment;
import android.util.Log;

public class Wispy {
	public static final int WISPY_CONNECT = 0;
	public static final int WISPY_DISCONNECT = 1;
	public static final int WISPY_POLL = 2;
	public static final int WISPY_POLL_FAIL = 3;
	public static final int WISPY_POLL_THREAD = 4;
	
	public static final int PASSES = 20;
	
	File _root;
	FileOutputStream _wispyOut;
	PrintStream _wispyPrint;
	
	boolean _device_connected;
	boolean _is_polling;
	boolean _reset_max;
	boolean _save_scans;
	int _poll_count;
	int _maxresults[];
	
	Semaphore _lock;
	
	public Wispy() {
		_lock = new Semaphore(1,true);
        _device_connected=false;
        _is_polling=false;
        _reset_max=false;
        _poll_count=0;
        _save_scans=false;
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
	
	public void getResultsBlock(int count_length) {
		
		Log.d("wispy", "attempting to get results");
		// Reset everything here, making sure not to conflict with the polling thread
		try {
			_lock.acquire();

			_poll_count=0;
			_reset_max=false;
			for(int i=0; i<256; i++)
	        	_maxresults[i]=-200;
			_save_scans=true;
			_lock.release();
		} catch (Exception e) {
			Log.d("wispy", "error acquiring lock to reset results");
		}

		// Take a second lock which 
		try {
			while(true) {
				_lock.acquire();
				if(_poll_count==count_length) {
					_save_scans=false;
					break;
				}
				_lock.release();
			}
			_lock.release();
		} catch (Exception e) {
			Log.d("wispy", "error acquiring lock to not save any more scans");
		}
		Log.d("wispy", "finished getting results!");
	}

}
