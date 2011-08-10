package com.gnychis.coexisyst;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
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

	public static final int ZIGBEE_CONNECT = 200;
	public static final int ZIGBEE_DISCONNECT = 201;
	public static final String ZIGBEE_SCAN_RESULT = "com.gnychis.coexisyst.ZIGBEE_SCAN_RESULT";
	public static final int MS_SLEEP_UNTIL_PCAPD = 5000;
	
	CoexiSyst coexisyst;
	
	boolean _device_connected;
	ZigBeeMon _monitor_thread;
	
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
		_monitor_thread.startScan();
		
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
		coexisyst.zigbee._monitor_thread = new ZigBeeMon();
		coexisyst.zigbee._monitor_thread.execute(coexisyst);
	}
	
	public void disconnected() {
		_device_connected=false;
		coexisyst.zigbee._monitor_thread.cancel(true);
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

	
	protected class ZigBeeMon extends AsyncTask<Context, Integer, String>
	{
		Context parent;
		CoexiSyst coexisyst;
		Socket skt;
		InputStream skt_in;
		OutputStream skt_out;
		private static final String ZIGMON_TAG = "ZigBeeMonitor";
		private int PCAP_HDR_SIZE = 16;
		Zigcapd zigcapd_thread;
		int _channel;
		private Semaphore _comm_lock;
		
		// Incoming commands
		byte CHANGE_CHAN=0x0000;
		byte TRANSMIT_PACKET=0x0001;
		byte RECEIVED_PACKET=0x0002;
		byte INITIALIZED=0x0003;
		byte TRANSMIT_BEACON=0x0004;
		byte START_SCAN=0x0005;
		byte SCAN_DONE=0x0006;
		
		// On pre-execute, we make sure that we initialize the card properly and set the state to IDLE
		@Override 
		protected void onPreExecute( )
		{
			_state = ZigBeeState.IDLE;			
		}
		
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
			try {
				RootTools.sendShell("busybox killall zigcapd");
			} catch(Exception e) {
				
			}
		}
		
		// The entire meat of the thread, pulls packets off the interface and dissects them
		@Override
		protected String doInBackground( Context ... params )
		{
			parent = params[0];
			coexisyst = (CoexiSyst) params[0];
			_comm_lock = new Semaphore(1,true);

			// Connect to the pcap daemon to pull packets from the hardware
			if(connectToZigcapd() == false) {
				Log.d(TAG, "failed to connect to the pcapd daemon, doh");
				sendMainMessage(ThreadMessages.ZIGBEE_FAILED);
				return "FAIL";
			}
			sendMainMessage(ThreadMessages.ZIGBEE_WAIT_RESET);
			
			// Wait for the initialized byte
			if(getSocketData(1)[0]!=INITIALIZED) {
				sendMainMessage(ThreadMessages.ZIGBEE_FAILED);
				return "FAIL";
			}
			
			sendMainMessage(ThreadMessages.ZIGBEE_INITIALIZED);
			
			// Send a command to set the channel to 1
			setChannel(0);
						
			// Loop and read headers and packets
			while(true) {
				byte cmd = getSocketData(1)[0];
				
				if(cmd==SCAN_DONE) {
					scanStop();
				}
				
				// Wait for a byte which signals a command
				if(cmd==RECEIVED_PACKET) {
				
					Packet rpkt = new Packet(WTAP_ENCAP_802_15);
	
					if((rpkt._rawHeader = getPcapHeader())==null) {
						zigcapd_thread.cancel(true);
						return "error reading pcap header";
					}
					rpkt._headerLen = rpkt._rawHeader.length;
									
					// Get the raw data now from the wirelen in the pcap header
					if((rpkt._rawData = getPcapPacket(rpkt._rawHeader))==null) {
						zigcapd_thread.cancel(true);
						return "error reading data";
					}
					rpkt._dataLen = rpkt._rawData.length;
					
					// Get the rx time
					getSocketData(4);
					
					// Get the LQI
					rpkt._lqi = (int)getSocketData(1)[0];
					
					// The channel is read from the hardware
					rpkt._band = frequencies[(int)getSocketData(1)[0]]; ;
					
					// Based on the state of our wifi thread, we determine what to do with the packet
					switch(_state) {
					
					case IDLE:
						break;
					
					// In the scanning state, we save all beacon frames as we hop channels (handled by a
					// separate thread).
					case SCANNING:
						// To identify a beacon from ZigBee, check for the field zbee.beacon.protocol.
						// If it exists, save the packet as part of our scan.
						if(rpkt.getField("zbee.beacon.protocol")!=null)
							_scan_results.add(rpkt);
						
						break;
					}
				}
			}
		}
		
		// First, acquire the lock to communicate with the ZigBee device,
		// then send the command to change the channel and the channel number.
		public boolean setChannel(int channel) {
			try {
				_comm_lock.acquire();
				skt_out.write(CHANGE_CHAN);		// first send the command
				skt_out.write(channel);	// then send the channel
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
				skt_out.write(TRANSMIT_BEACON);
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
				skt_out.write(START_SCAN);
			} catch(Exception e) {
				_comm_lock.release();
				return false;
			}
			_comm_lock.release();
			return true;
		}
		
		// Connect to the daemon (written in native C code) for which we communicate
		// with the ZigBee device.  This is done so that the daemon can run as root.
		public boolean connectToZigcapd() {
			
			// Generate a random port for Pcapd
			Random generator = new Random();
			int pcapd_port = 2000 + generator.nextInt(500);
			
			Log.d(ZIGMON_TAG, "a new Wifi monitor thread was started");
			
			// Attempt to create capture process spawned in the background
			// which we will connect to for pcap information.
			zigcapd_thread = new Zigcapd(pcapd_port);
			zigcapd_thread.execute(coexisyst);
			
			// Send a message to block the main dialog after the card is done initializing
			try { Thread.sleep(MS_SLEEP_UNTIL_PCAPD); } catch (Exception e) {} // give some time for the process
			
			// Attempt to connect to the socket via TCP for the PCAP info
			try {
				skt = new Socket("localhost", pcapd_port);
			} catch(Exception e) {
				Log.e(ZIGMON_TAG, "exception trying to connect to wifi socket for pcap on " + Integer.toString(pcapd_port), e);
				return false;
			}
			
			try {
				skt_in = skt.getInputStream();
				skt_out = skt.getOutputStream();
			} catch(Exception e) {
				Log.e(ZIGMON_TAG, "exception trying to get inputbuffer from socket stream");
				return false;
			}
			Log.d(ZIGMON_TAG, "successfully connected to pcapd on port " + Integer.toString(pcapd_port));
			return true;
		}
		
		// Read the pcap header from the socket
		public byte[] getPcapHeader() {
			byte[] rawdata = getSocketData(PCAP_HDR_SIZE);
			return rawdata;
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
			byte[] data = new byte[length];
			int v=0;
			try {
				int total=0;
				while(total < length) {
					v = skt_in.read(data, total, length-total);
					//Log.d("WifiMon", "Read in " + Integer.toString(v) + " - " + Integer.toString(total+v) + " / " + Integer.toString(length));
					if(v==-1)
						cancel(true);  // cancel the thread if we have errors reading socket
					total+=v;
				}
			} catch(Exception e) { 
				Log.e("WifiMon", "unable to read from pcapd buffer",e);
				return null;
			}
			
			return data;
		}
	}
}
