package com.gnychis.coexisyst;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import android.util.Log;

public class SubSystem {
	DataOutputStream _out;
	Process _proc;
	CoexiSyst _coexisyst;
	PrintStream _cmdfh;

	public SubSystem(CoexiSyst c) {
		_coexisyst = c;
		try {
        	_proc = Runtime.getRuntime().exec("su");
        	_out = new DataOutputStream(_proc.getOutputStream()); 
    		_cmdfh = new PrintStream( new FileOutputStream("/sdcard/cmds.txt"));
		} catch(Exception e) {
			Log.e("SYSTEM", "exception trying to get data streams",e);
		}
	}
	
	public void cmd(String c) {
		try {
			String fullc = c + "\n";
			_out.writeBytes(fullc);
			_out.flush();
			Log.d("SYSTEM", "running command: " + fullc);
			_cmdfh.print(fullc);
			_cmdfh.flush();
		} catch (Exception e) {
			Log.e("SYSTEM", "failure trying to run: " + c);
			Log.e("SYSTEM", "exception trying to run command",e);
		}
	}
	
	public void local_cmd(String c) {
		try {
			String fullc = "/data/data/com.gnychis.coexisyst/bin/" + c + "\n";
			_out.writeBytes(fullc);
			_out.flush();
			_cmdfh.print(fullc);
			_cmdfh.flush();
			Log.d("SYSTEM", "running command: " + fullc);
		} catch (Exception e) {
			Log.e("SYSTEM", "exception trying to run command",e);
		}		
	}
	
	public void install_bin(String b, int resource) {
		Log.d("SYSTEM", "Working to install: " + b);
    	// Copy in iwconfig
    	File outFile = new File("/data/data/com.gnychis.coexisyst/" + b);
    	InputStream is = _coexisyst.getResources().openRawResource(resource);
    	byte buf[] = new byte[1024];
        int len;
        try {
        	OutputStream out = new FileOutputStream(outFile);
        	while((len = is.read(buf))>0) {
				out.write(buf,0,len);
			}
        	out.close();
        	is.close();
		} catch (IOException e) {
			Log.e("SYSTEM", "Unable to install bin " + b, e);
		}
		//cmd("busybox whoami > /data/data/com.gnychis.coexisyst/me");
		cmd("mv /data/data/com.gnychis.coexisyst/" + b + " /data/data/com.gnychis.coexisyst/bin/");
		cmd("chmod 0755 /data/data/com.gnychis.coexisyst/bin/" + b);		
	}
}
