package com.gnychis.coexisyst.NetDevDefinitions;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

// NOTE:  This is a very basic class, mainly just a wrapper around an already pretty rich class for Bluetooth
//        devices provided by the Android API. Some calls are just a little more simplified.

public class BluetoothDev implements Parcelable {

	private BluetoothDevice _raw_device;
	private short _rssi;
	
    public void writeToParcel(Parcel out, int flags) {
    	
    	out.writeParcelable(_raw_device, 0);
    	out.writeInt(_rssi);

    }
    
    private BluetoothDev(Parcel in) {
    	_raw_device = (BluetoothDevice) in.readParcelable(BluetoothDevice.class.getClassLoader());
    	_rssi = (short) in.readInt();
    }
	
	public int describeContents()
	{
		return this.hashCode();
	}
	
    public static final Parcelable.Creator<BluetoothDev> CREATOR = new Parcelable.Creator<BluetoothDev>() {
    	public BluetoothDev createFromParcel(Parcel in) {
    		return new BluetoothDev(in);
    	}

		public BluetoothDev[] newArray(int size) {
			return new BluetoothDev[size];
		}
    };
	
	public BluetoothDev(BluetoothDevice d) {
		_raw_device = d;
	}
	
	public String name() {
		return _raw_device.getName();
	}
	
	public String mac() {
		return _raw_device.getAddress();
	}
	
	public void set_rssi(short r) { _rssi = r; }
	
	public short rssi() {
		return _rssi;
	}
	
}
