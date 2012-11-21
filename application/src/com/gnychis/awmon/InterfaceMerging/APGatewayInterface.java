package com.gnychis.awmon.InterfaceMerging;

import java.util.Arrays;

import android.content.Context;

import com.gnychis.awmon.Core.UserSettings;
import com.gnychis.awmon.DeviceAbstraction.Interface;
import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.DeviceAbstraction.WiredInterface;
import com.gnychis.awmon.DeviceAbstraction.WirelessInterface;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;

/**
 * All 802.11 APs have a "wired" gateway interface, which uses a separate MAC address
 * than the wireless interface... however they belong to the same device. So, we want
 * to merge this wired Interface with the same device that contains the APs wireless
 * interfaces.
 * 
 * @author gnychis
 *
 */
public class APGatewayInterface extends MergeHeuristic {
	
	UserSettings _settings;
	
	@SuppressWarnings("unchecked")
	public APGatewayInterface(Context p) {
		super(p,Arrays.asList(Wifi.class, LAN.class));
		_settings = new UserSettings(p);
	}

	public MergeStrength classifyInterfacePair(InterfacePair pair) {
		
		Interface left = pair.getLeft();
		Interface right = pair.getRight();
		
		boolean leftIsGateway=false;
		boolean rightIsGateway=false;
		
		// See if either one of the Interfaces is the gateway interface
		if(left.getClass()==WiredInterface.class && ((WiredInterface)left).isGateway())
			leftIsGateway=true;
		if(right.getClass()==WiredInterface.class && ((WiredInterface)right).isGateway())
			rightIsGateway=true;
		
		// If neither is the gateway, then we have nothing to say about this pair.
		if(!leftIsGateway && !rightIsGateway)
			return MergeStrength.UNDETERMINED;
		
		// Let's see if the other one in the pair is the wireless interface to our AP
		if(right.getClass()==WirelessInterface.class && right._MAC.equals(_settings.getHomeWifiMAC()))
			return MergeStrength.LIKELY;
		if(left.getClass()==WirelessInterface.class && left._MAC.equals(_settings.getHomeWifiMAC()))
			return MergeStrength.LIKELY;

		return MergeStrength.UNDETERMINED;	
	}

}
