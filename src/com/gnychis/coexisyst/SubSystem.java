package com.gnychis.coexisyst;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class SubSystem {
	DataOutputStream _out;
	DataInputStream _in;
	Process _proc;
	CoexiSyst _coexisyst;

	public SubSystem(CoexiSyst c) {
		_coexisyst = c;
		try {
        	_proc = Runtime.getRuntime().exec("su");
        	_out = new DataOutputStream(_proc.getOutputStream()); 
        	_in = new DataInputStream(_proc.getInputStream());
		} catch(Exception e) {
			Log.e("SYSTEM", "exception trying to get data streams",e);
		}
		
	}
	
	public void cmd(String c) {
		String in_data;
		try {
			_out.writeBytes(c);
		} catch (Exception e) {
			Log.e("SYSTEM", "exception trying to run command",e);
		}
		
		/*try {
			do {
				in_data = _in.readLine();
			} while(in_data != null);
		} catch (Exception e) {
			Log.e("SYSTEM", "error trying to read after command", e);
		}*/
	}
	
	public void install_bin(String b, int resource) {
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
			Log.e("SYSTEM", "Unable to install iwconfig", e);
		}
		cmd("mv /data/data/com.gnychis.coexisyst/" + b + " /data/data/com.gnychis.coexisyst/bin/\n");
		cmd("chmod 0755 /data/data/com.gnychis.coexisyst/bin/" + b + "\n");		
	}
}
