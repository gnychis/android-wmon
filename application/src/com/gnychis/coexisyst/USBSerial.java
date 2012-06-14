package com.gnychis.coexisyst;

import java.util.List;

import android.util.Log;

import com.stericson.RootTools.RootTools;

public class USBSerial {
	
	private int fd;
	
	public USBSerial() {
		fd = -1;
	}
	
	public boolean openPort(String port_name) {
		
		// First set the user permissions
		try {
			String cmd = "chown " + getAppUser() + " " + port_name;
			RootTools.sendShell(cmd,0);
		} catch(Exception e) {}
		
		fd = openCommPort(port_name);
		
		if(fd==-1)
			return false;
		
		return true;
	}
	
	public boolean closePort() {
		if(closeCommPort(fd)==-1)
			return false;
		
		fd = -1;
		return true;
		
	}
	
	public byte getByte() {
		try {
			char c = blockRead1(fd);
			return (byte)c;
			
		} catch(Exception e) {
			Log.e("USBSerial", "Error trying to read byte", e);
			return 'f';
		}
	}
	
	public byte[] getBytes(int nBytes) {
		return blockReadBytes(fd, nBytes);
	}
	
	public void writeBytes(byte[] data, int length) {
		writeBytes(fd, data, length);
	}
	
	public void writeByte(byte data) {
		byte[] tdata = new byte[1];
		tdata[0] = data;
		writeBytes(fd, tdata, 1);
	}
	
	
    public String getAppUser() {
    	try {
    		List<String> res = RootTools.sendShell("ls -l /data/data | grep com.gnychis.coexisyst",0);
    		return res.get(0).split(" ")[1];
    	} catch(Exception e) {
    		return "FAIL";
    	}
    }
    
    public int getInt32() {
    	return readInt32(fd);
    }
	
    private native int readInt32(int fd);
    private native void writeBytes(int fd, byte[] data, int length);
	private native int openCommPort(String port);
	private native int closeCommPort(int fd);
	private native char blockRead1(int fd);
    private native byte[] blockReadBytes(int fd, int nBytes);
}
