package com.gnychis.coexisyst;

public class AtherosDev {
	public static final int ATHEROS_CONNECT = 100;
	public static final int ATHEROS_DISCONNECT = 101;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	
	
	public AtherosDev(CoexiSyst c) {
		coexisyst = c;
    	//coexisyst.system.cmd("cd /system/lib/modules\n");
    	coexisyst.system.cmd("insmod /system/lib/modules/cfg80211.ko\n");
    	coexisyst.system.cmd("insmod /system/lib/modules/crc7.ko\n");
    	coexisyst.system.cmd("insmod /system/lib/modules/mac80211.ko\n");
    	coexisyst.system.cmd("insmod /system/lib/modules/zd1211rw.ko\n");
    	coexisyst.system.cmd("cd /system/etc/firmware");
    	coexisyst.system.cmd("busybox unzip /data/data/com.gnychis.coexisyst/bin/zd_firmware.zip");
	}
	
	public void connected() {
		_device_connected=true;
		coexisyst.system.cmd("netcfg wlan0 down");
		coexisyst.system.local_cmd("iwconfig wlan0 mode monitor");
		coexisyst.system.cmd("netcfg wlan0 up");
	}
	
	public void disconnected() {
		_device_connected=false;
	}
	
}
