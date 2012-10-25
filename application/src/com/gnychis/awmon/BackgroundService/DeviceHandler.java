package com.gnychis.awmon.BackgroundService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gnychis.awmon.AWMon;
import com.gnychis.awmon.AWMon.ThreadMessages;
import com.gnychis.awmon.Core.USBMon;
import com.gnychis.awmon.DeviceHandlers.Wifi;
import com.gnychis.awmon.DeviceHandlers.ZigBee;
import com.gnychis.awmon.Interfaces.AddNetwork;
import com.gnychis.awmon.ScanReceivers.NetworksScan;

// The handlers to the devices must reside in the background service, because there is
// not guarantee the main activity (AWMon) is actually active or in use.  But, it is
// guaranteed that the background service is always running.  Therefore, this class
// should be instantiated in the BackgroundService.
public class DeviceHandler {
	
	private static final String TAG = "AWMonDeviceHandler";
	
	Context _parent;
	
	public WifiManager _wifiManager;
	public Wifi _wifi;
	public ZigBee _zigbee;
	public BluetoothAdapter _bt;
	public NetworksScan _networks_scan;
	protected USBMon _usbmon;
	
	public DeviceHandler(Context parent) {
		_parent=parent;
		
		// Initialize the device handles
		_wifi = new Wifi(parent);
		
		// Setup internal wireless device handles
		_wifiManager = (WifiManager) _parent.getSystemService(Context.WIFI_SERVICE);
		_bt = BluetoothAdapter.getDefaultAdapter();
		
		// Create handles to our internal devices and mechanisms
		_zigbee = new ZigBee(parent);
		_usbmon = new USBMon(parent, _handler);
		
		// Register various receivers to receive scan updates.
    	_networks_scan = new NetworksScan(_handler, _usbmon, _wifi, _zigbee, _bt);
		_parent.registerReceiver(_networks_scan._rcvr_80211, new IntentFilter(Wifi.WIFI_SCAN_RESULT));
		_parent.registerReceiver(_networks_scan._rcvr_ZigBee, new IntentFilter(ZigBee.ZIGBEE_SCAN_RESULT));
		_parent.registerReceiver(_networks_scan._rcvr_BTooth, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		_parent.registerReceiver(_networks_scan._rcvr_BTooth, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

	}
	
	public Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			// Based on the thread message, a difference action will take place
			ThreadMessages tm = ThreadMessages.values()[msg.what];
			switch(tm) {
			
				//////////////////////////////////////////////////////
				case SHOW_TOAST:
					try {
						//String m = toastMessages.remove();  // FIXME
						//Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show();	 // FIXME
					} catch(Exception e) { }
					break;
					
				//////////////////////////////////////////////////////
				case WIFIDEV_CONNECTED:
					//_pd = ProgressDialog.show(AWMon.this, "", "Initializing Wifi device...", true, false);  // FIXME
					_usbmon.stopUSBMon();
					_wifi.connected();
					break;
					
				case WIFIDEV_INITIALIZED:
					//Toast.makeText(getApplicationContext(), "Successfully initialized Wifi device", Toast.LENGTH_LONG).show();	// FIXME
					//_pd.dismiss();
					_usbmon.startUSBMon();
					break;
					
				case WIFIDEV_FAILED:
					//Toast.makeText(getApplicationContext(), "Failed to initialize Wifi device", Toast.LENGTH_LONG).show();  // FIXME
					break;
					
				//////////////////////////////////////////////////////
				case ZIGBEE_CONNECTED:
					//_pd = ProgressDialog.show(AWMon.this, "", "Initializing ZigBee device...", true, false);  // FIXME
					_usbmon.stopUSBMon();
					_zigbee.connected();
					break;
					
				case ZIGBEE_WAIT_RESET:
					//_pd.dismiss();  // FIXME
					//_pd = ProgressDialog.show(AWMon.this, "", "Press ZigBee reset button...", true, false);  // FIXME
					break;
					
				case ZIGBEE_INITIALIZED:
					//_pd.dismiss();  // FIXME
					//Toast.makeText(getApplicationContext(), "Successfully initialized ZigBee device", Toast.LENGTH_LONG).show();	// FIXME
					_usbmon.startUSBMon();
					break;
					
				case ZIGBEE_FAILED:
					//Toast.makeText(getApplicationContext(), "Failed to initialize ZigBee device", Toast.LENGTH_LONG).show();	// FIXME
					break;
					
					
				//////////////////////////////////////////////////////
				case INCREMENT_SCAN_PROGRESS:
					//_pd.incrementProgressBy(1);  // FIXME
					break;
				
				case NETWORK_SCANS_COMPLETE:
					//_pd.dismiss();  // FIXME
					/*try {
						Log.d(TAG,"Trying to load add networks window");
						Intent i = new Intent(AWMon.this, AddNetwork.class);
						
						// Hopefully this is not broken, using it as a WifiScanReceiver rather
						// than BroadcastReceiver type.
						i.putExtra(_app_name + ".80211", _networks_scan._wifi_scan_result);
						i.putExtra(_app_name + ".ZigBee", _networks_scan._zigbee_scan_result);
						i.putExtra(_app_name + ".Bluetooth", _networks_scan._bluetooth_scan_result);
						i.putExtra(_app_name + ".WiSpy", _networks_scan._wispy_scan_result);
						
						startActivity(i);
					} catch (Exception e) {
						Log.e(TAG, "Exception trying to load network add window",e);
						return;
					}*/
					break;	
			}
		}
	};

}
