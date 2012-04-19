package com.gnychis.coexisyst;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.jnetpcap.PcapHeader;
import org.jnetpcap.nio.JBuffer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import com.gnychis.coexisyst.CoexiSyst.ThreadMessages;
import com.stericson.RootTools.RootTools;

public class ZigBee {
	private static final String TAG = "ZigbeeDev";
	private static final boolean VERBOSE = true;

	public static final int ZIGBEE_CONNECT = 200;
	public static final int ZIGBEE_DISCONNECT = 201;
	public static final String ZIGBEE_SCAN_RESULT = "com.gnychis.coexisyst.ZIGBEE_SCAN_RESULT";
	public static final int MS_SLEEP_UNTIL_PCAPD = 5000;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	ZigBeeScan _monitor_thread;
	
	static int WTAP_ENCAP_802_15 = 127;
	
	ZigBeeState _state;
	private Semaphore _state_lock;
	public enum ZigBeeState {
		IDLE,
		SCANNING,
	}
	
	ArrayList<Packet> _scan_results;
	
	// http://en.wikipedia.org/wiki/List_of_WLAN_channels
	static int[] channels = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
	static int[] frequencies = {2405, 2410, 2415, 2420, 2425, 2430, 2435, 
			2440, 2445, 2450, 2455, 2460, 2465, 2470, 2475, 2480};
	
	static public int freqToChan(int freq) {
		int i=0;
		for(i=0; i<frequencies.length; i++)
			if(frequencies[i]==freq)
				break;
		if(!(i<frequencies.length))
			return -1;
		
		return channels[i];
	}
	
	static public int chanToFreq(int chan) {
		if(chan<0 || chan>channels.length-1)
			return -1;
		return frequencies[chan];
	}	
	
	public boolean isConnected() {
		return _device_connected;
	}
	
	// Set the state to scan and start to switch channels
	public boolean scanStart() {
		
		// Only allow to enter scanning state IF idle
		if(!ZigBeeStateChange(ZigBeeState.SCANNING))
			return false;
		
		_scan_results.clear();
		
		_monitor_thread = new ZigBeeScan();
		_monitor_thread.execute(coexisyst);
		
		return true;  // in scanning state, and channel hopping
	}
	
	public boolean scanStop() {
		// Need to return the state back to IDLE from scanning
		if(!ZigBeeStateChange(ZigBeeState.IDLE)) {
			Log.d(TAG, "Failed to change from scanning to IDLE");
			return false;
		}
		
		// Now, send out a broadcast with the results
		Intent i = new Intent();
		i.setAction(ZIGBEE_SCAN_RESULT);
		i.putExtra("packets", _scan_results);
		coexisyst.sendBroadcast(i);
		
		return true;
	}
	
	// Attempts to change the current state, will return
	// the state after the change if successful/failure
	public boolean ZigBeeStateChange(ZigBeeState s) {
		boolean res = false;
		if(_state_lock.tryAcquire()) {
			try {
				
				// Can add logic here to only allow certain state changes
				// Given a _state... then...
				switch(_state) {
				
				// From the IDLE state, we can go anywhere...
				case IDLE:
					Log.d(TAG, "Switching state from " + _state.toString() + " to " + s.toString());
					_state = s;
					res = true;
				break;
				
				// We can go to idle, or ignore if we are in a
				// scan already.
				case SCANNING:
					if(s==ZigBeeState.IDLE) {  // cannot go directly to IDLE from SCANNING
						Log.d(TAG, "Switching state from " + _state.toString() + " to " + s.toString());
						_state = s;
						res = true;
					} else if(s==ZigBeeState.SCANNING) {  // ignore an attempt to switch in to same state
						res = false;
					} 
				break;
				
				default:
					res = false;
				}
				
			} finally {
				_state_lock.release();
			}
		} 		
		
		return res;
	}
	
	public ZigBee(CoexiSyst c) {
		_state_lock = new Semaphore(1,true);
		_scan_results = new ArrayList<Packet>();
		coexisyst = c;
		_state = ZigBeeState.IDLE;
		Log.d(TAG, "Initializing ZigBee class...");

	}
	
	public void connected() {
		_device_connected=true;
		ZigBeeInit zbi = new ZigBeeInit();
		zbi.execute(coexisyst);
	}
	
	public void disconnected() {
		_device_connected=false;
	}
	
	public static byte[] parseMacAddress(String macAddress)
    {
        String[] bytes = macAddress.split(":");
        byte[] parsed = new byte[bytes.length];

        for (int x = 0; x < bytes.length; x++)
        {
            BigInteger temp = new BigInteger(bytes[x], 16);
            byte[] raw = temp.toByteArray();
            parsed[x] = raw[raw.length - 1];
        }
        return parsed;
    }
	
	public static BigInteger parseMacStringToBigInteger(String macAddress)
	{
		String newMac = macAddress.replaceAll(":", "");
		BigInteger ret = new BigInteger(newMac, 16);  // 16 specifies hex
		return ret;
	}

	protected class ZigBeeInit extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		USBSerial _dev;
		
		// The initialized sequence (hardware sends it when it is initialized)
		byte initialized_sequence[] = {0x67, 0x65, 0x6f, 0x72, 0x67, 0x65, 0x6e, 0x79, 0x63, 0x68, 0x69, 0x73};
		
		private void debugOut(String msg) {
			if(VERBOSE)
				Log.d("ZigBeeInit", msg);
		}
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(CoexiSyst.ThreadMessages t) {
			Message msg = new Message();
			msg.obj = t;
			coexisyst._handler.sendMessage(msg);
		}
		
		public boolean checkInitSeq(byte buf[]) {
			
			for(int i=0; i<initialized_sequence.length; i++)
				if(initialized_sequence[i]!=buf[i])
					return false;
						
			return true;
		}
		
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			
			// Create a serial device
			_dev = new USBSerial();
			
			// Get the name of the USB device, which will be the last thing in dmesg
			String ttyUSB_name;
			try {
				List<String> res = RootTools.sendShell("dmesg | grep ttyUSB | tail -n 1 | awk '{print $NF}'",0);
				ttyUSB_name = res.get(0);
			} catch (Exception e) { return ""; }	
			
			// Attempt to open the COM port which calls the native libraries
			if(!_dev.openPort("/dev/" + ttyUSB_name))
				return "FAIL";
			
			debugOut("opened device, now waiting for sequence");
			
			// Wait for the initialized sequence...
			byte[] readSeq = new byte[initialized_sequence.length];
			sendMainMessage(ThreadMessages.ZIGBEE_WAIT_RESET);
			while(!checkInitSeq(readSeq)) {
				for(int i=0; i<initialized_sequence.length-1; i++)
					readSeq[i] = readSeq[i+1];
				readSeq[initialized_sequence.length-1] = _dev.getByte();
			}
			
			debugOut("received the initialization sequence!");
			
			// Close the port
			if(!_dev.closePort())
				sendMainMessage(ThreadMessages.ZIGBEE_FAILED);

			sendMainMessage(ThreadMessages.ZIGBEE_INITIALIZED);

			return "OK";
		}
		
	}
	
	protected class ZigBeeScan extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
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
		
		// Used to send messages to the main Activity (UI) thread
		protected void sendMainMessage(CoexiSyst.ThreadMessages t) {
			Message msg = new Message();
			msg.obj = t;
			coexisyst._handler.sendMessage(msg);
		}
		
		@Override
		protected void onCancelled()
		{
			Log.d(TAG, "ZigBee monitor thread is canceled...");
		}
		
		// The entire meat of the thread, pulls packets off the interface and dissects them
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			_comm_lock = new Semaphore(1,true);
			
			// Create a serial device
			_dev = new USBSerial();
			
			// Get the name of the USB device, which will be the last thing in dmesg
			String ttyUSB_name;
			try {
				List<String> res = RootTools.sendShell("dmesg | grep ttyUSB | tail -n 1 | awk '{print $NF}'",0);
				ttyUSB_name = res.get(0);
			} catch (Exception e) { return ""; }	
			
			// Attempt to open the COM port which calls the native libraries
			if(!_dev.openPort("/dev/" + ttyUSB_name))
				return "FAIL";
			
			startScan(); 	// Initialize the scan
						
			// Loop and read headers and packets
			while(true) {
				byte cmd = getSocketData(1)[0];
				
				if(cmd==CHAN_IS) {
					_channel = (int)_dev.getByte() & 0xff;
					
					// Our way of tracking progress with the main UI
					sendMainMessage(ThreadMessages.INCREMENT_SCAN_PROGRESS);
				}
				
				if(cmd==SCAN_DONE)
					break;
				
				// Wait for a byte which signals a command
				if(cmd==RECEIVED_PACKET) {
				
					Packet rpkt = new Packet(WTAP_ENCAP_802_15);
					
					// The channel is read from the hardware
					rpkt._band = frequencies[(int)getSocketData(1)[0]];
					
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
						return "FAIL";
					}
					rpkt._dataLen = rpkt._rawData.length;

					// To identify a beacon from ZigBee, check for the field zbee.beacon.protocol.
					// If it exists, save the packet as part of our scan.
					if(rpkt.getField("zbee.beacon.protocol")!=null)
						_scan_results.add(rpkt);
				}
			}
			
			if(!_dev.closePort())
				sendMainMessage(ThreadMessages.ZIGBEE_FAILED);
			
			scanStop();
			return "PASS";
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
		
		// Transmit a command to start a scan on the hardware (channel hop)
		public boolean startScan() {
			try {
				_comm_lock.acquire();
				_dev.writeByte(START_SCAN);
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
}
