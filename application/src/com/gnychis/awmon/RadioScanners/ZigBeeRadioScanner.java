package com.gnychis.awmon.RadioScanners;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

import android.util.Log;

import com.gnychis.awmon.Core.Packet;
import com.gnychis.awmon.Core.USBSerial;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.GUI.MainInterface;
import com.gnychis.awmon.HardwareHandlers.InternalRadio;
import com.gnychis.awmon.HardwareHandlers.ZigBee;
import com.stericson.RootTools.RootTools;

public class ZigBeeRadioScanner extends RadioScanner {
	final String TAG = "ZigBeeScanner";
	static int WTAP_ENCAP_802_15 = 127;
	private int PCAP_HDR_SIZE = 16;
	int _channel;
	private Semaphore _comm_lock;
	USBSerial _dev;
	
	// Incoming commands
	byte CHANGE_CHAN=0x0000;
	byte TRANSMIT_PACKET=0x0001;
	byte RECEIVED_PACKET=0x0002;
	byte INITIALIZED=0x0003;
	byte TRANSMIT_BEACON=0x0004;
	byte START_SCAN=0x0005;
	byte SCAN_DONE=0x0006;
	byte CHAN_IS=0x0007;
	
	public ZigBeeRadioScanner() {
		super(WirelessInterface.Type.ZigBee);
	}
	
	// Transmit a command to start a scan on the hardware (channel hop)
	public boolean initializeScan() {					
		try {
			
			// First, we need to get the name of the USB device from dmesg
			List<String> res = RootTools.sendShell("dmesg | grep ttyUSB | tail -n 1 | awk '{print $NF}'",0);
			String ttyUSB_name = res.get(0);	
			
			// We also setup the serial port and acquire a communication lock on it
			_dev = new USBSerial();
			_comm_lock.acquire();	
			if(!_dev.openPort("/dev/" + ttyUSB_name))
				return false;
			
			// Finally we trigger the scan
			_dev.writeByte(START_SCAN);
		} catch (Exception e) {  
			_comm_lock.release();
			return false;
		}	
		
		_comm_lock.release();	// Release the lock.
		return true;
	}
	
	// The entire meat of the thread, pulls packets off the interface and dissects them
	@Override
	protected ArrayList<WirelessInterface> doInBackground( InternalRadio ... params )
	{
		_hw_device = params[0];
		_comm_lock = new Semaphore(1,true);
		ArrayList<Packet> scanResult = new ArrayList<Packet>();
		
		initializeScan(); 	// Initialize the scan
					
		// Loop and read headers and packets
		while(true) {
			byte cmd = getSocketData(1)[0];
			
			if(cmd==CHAN_IS) {
				_channel = (int)_dev.getByte() & 0xff;
				
				// Our way of tracking progress with the main UI
				MainInterface.sendThreadMessage(_hw_device._parent, MainInterface.ThreadMessages.INCREMENT_SCAN_PROGRESS);
			}
			
			if(cmd==SCAN_DONE)
				break;
			
			// Wait for a byte which signals a command
			if(cmd==RECEIVED_PACKET) {
			
				Packet rpkt = new Packet(WTAP_ENCAP_802_15);
				
				// The channel is read from the hardware
				rpkt._band = ZigBee.frequencies[(int)getSocketData(1)[0]];
				
				// Get the LQI
				rpkt._lqi = (int)getSocketData(1)[0] & 0xff;

				// Get the rx time
				getSocketData(4);
				
				// Get the data length
				rpkt._dataLen = (int)_dev.getByte();
				
				// Create a raw header (the serial device does not send one)
				rpkt._rawHeader = new byte[PCAP_HDR_SIZE];
				rpkt._headerLen = PCAP_HDR_SIZE;
				for(int k=0; k<8; k++)
					rpkt._rawHeader[k]=0;
				rpkt._rawHeader[8]=Integer.valueOf(rpkt._dataLen).byteValue(); rpkt._rawHeader[9]=0; rpkt._rawHeader[10]=0; rpkt._rawHeader[11]=0;
				rpkt._rawHeader[12]=Integer.valueOf(rpkt._dataLen).byteValue(); rpkt._rawHeader[13]=0; rpkt._rawHeader[14]=0; rpkt._rawHeader[15]=0;
								
				// Get the raw data now from the wirelen in the pcap header
				if((rpkt._rawData = getPcapPacket(rpkt._rawHeader))==null) {
					return null;
				}
				rpkt._dataLen = rpkt._rawData.length;

				// To identify a beacon from ZigBee, check for the field zbee.beacon.protocol.
				// If it exists, save the packet as part of our scan.
				if(rpkt.getField("zbee.beacon.protocol")!=null)
					scanResult.add(rpkt);
			}
		}
		
		if(!_dev.closePort())
			MainInterface.sendToastRequest(_hw_device._parent, "ZigBee device failed while scanning");
		
		return _result_parser.returnDevices(scanResult);
	}
	
	// First, acquire the lock to communicate with the ZigBee device,
	// then send the command to change the channel and the channel number.
	public boolean setChannel(int channel) {
		try {
			_comm_lock.acquire();
			_dev.writeByte(CHANGE_CHAN);		// first send the command
			_dev.writeByte((byte)channel);	// then send the channel
		} catch(Exception e) { 
			_comm_lock.release();
			return false;
		}
		_channel = channel;
		_comm_lock.release();
		return true;
	}
	
	// Acquire the lock to communicate with the ZigBee device, then write
	// the command to transmit a beacon on the current channel.
	public boolean transmitBeacon() {
		try {
			_comm_lock.acquire();
			_dev.writeByte(TRANSMIT_BEACON);
		} catch(Exception e) {
			_comm_lock.release();
			return false;
		}
		_comm_lock.release();
		return true;
	}

	// Read the pcap packet from the socket, based on the number of bytes
	// specified in the header (needed).
	public byte[] getPcapPacket(byte[] rawHeader) {
		byte[] rawdata;
		PcapHeader header = null;

		try {
			header = new PcapHeader();
			JBuffer headerBuffer = new JBuffer(rawHeader);  
			header.peer(headerBuffer, 0);				
		} catch(Exception e) {
			Log.e("WifiMon", "exception trying to read pcap header",e);
		}
					
		rawdata = getSocketData(header.wirelen());
		return rawdata;
	}
	
	// Returns any length of socket data specified, blocking until complete.
	public byte[] getSocketData(int length) {
		return _dev.getBytes(length);
	}
}
