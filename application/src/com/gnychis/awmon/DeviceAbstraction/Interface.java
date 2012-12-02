package com.gnychis.awmon.DeviceAbstraction;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.text.WordUtils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;
import com.gnychis.awmon.HardwareHandlers.ZigBee;

public class Interface implements Parcelable {
	
	public String _MAC;							// The MAC address of the interface, or some address.
	public String _IP;							// The IP address associated to the interface (null if none)
	public String _ouiName;						// The associated manufacturer OUI name (null if none)
	public String _ifaceName;					// A name associated with the specific interface
	public Class<?> _type;						// The interface type (should be a class in HardwareHandlers that extended InternalRadio)
	private int _ifaceKey;						// This is a random long to denote a unique Interface that we can track, ask George for importance

	public Interface() {
		_MAC=null;
		_IP=null;
		_ouiName=null;
		_ifaceName=null;
		_type=null;	
		_ifaceKey = generateKey();
	}
	
	public Interface(Class<?> type) {
		_MAC=null;
		_IP=null;
		_ouiName=null;
		_ifaceName=null;
		_type=type;
		_ifaceKey = generateKey();
	}
	
	public Interface(Interface i) {
		_MAC=i._MAC;
		_IP=i._IP;
		_ouiName=i._ouiName;
		_ifaceName=i._ifaceName;
		_type=i._type;
		_ifaceKey=i._ifaceKey;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj.getClass() != getClass())
            return false;
        
        Interface iface = (Interface) obj;
        
        return new EqualsBuilder().
                //appendSuper(super.equals(obj)).
                append(_MAC, iface._MAC).
                append(_IP, iface._IP).
                append(_ouiName, iface._ouiName).
                append(_ifaceName, iface._ifaceName).
                append(_type, iface._type).
                isEquals();
	}
	
	public String toFormattedString() {
		return simplifiedClassName(_type) + " Interface: <br />" 
				+ "&nbsp;&nbsp;&nbsp;&nbsp;* <u>MAC</u>: " + _MAC + "<br />" 
				+ ((_type==Wifi.class || _type==LAN.class) ? "&nbsp;&nbsp;&nbsp;&nbsp;* <u>IP</u>: " + _IP + "<br />" : "")
				+ ((_ifaceName!=null) ? "&nbsp;&nbsp;&nbsp;&nbsp;* <u>Name</u>: " + _ifaceName + "<br />": "") 
				+ "<br />";
	}
	
	@Override
	public String toString()  {
		return simplifiedClassName(getClass()) + " - " + simplifiedClassName(_type) + ": " 
				+ "  MAC: " + _MAC 
				+ "  IP: " + _IP
				+ "  Name:" + _ifaceName
				+ "  OUI:" + _ouiName;
	}
	
	/** This let's us know if the device belongs on the home LAN.  We make the assumption
	 * for now that, if it has an IP address, it must be associated to the LAN.  We assign
	 * IPs from the active ARP scan which only occurs on the local LAN, so for now this should hold.
	 * @return true if the interface belongs to the home LAN, false otherwise
	 */
	public boolean interfaceOnHomeLAN() {
		if(_IP!=null)
			return true;
		return false;
	}
	
	
	/** This just strips some of the junk out of OUI names like "corp." and "LTD"
	 * @return returns the clean OUI name.
	 */
	public String cleanOUIname() {
		List<String> kill = Arrays.asList(" INC.", " CORP.", " LTD.", ",", " Ltd.", " Inc.", " CO.");
		String cleanName = _ouiName;
		for(String k : kill)
			cleanName = cleanName.replace(k, "");
		String[] split = cleanName.split(" ");
		if(split.length>0)
			return WordUtils.capitalize(cleanName.split(" ")[0].toLowerCase());
		else
			return WordUtils.capitalize(cleanName.toLowerCase());
	}
	
	/** This merges the information from Interface 'i' in to the current interface,
	 * if it is any information that the current interface is lacking.
	 * @param i the Interface to merge in
	 */
	public void merge(Interface i) {
		if(_MAC==null) 			_MAC = i._MAC;
		if(_IP==null) 			_IP = i._IP;
		if(_ouiName==null)  	_ouiName = i._ouiName;
		if(_ifaceName==null)	_ifaceName = i._ifaceName;
		if(_type==null)			_type = i._type;
	}
	
	/** Returns the unique key for the interface which is persistent as interfaces are copied
	 * with broadcasts, merged in to devices, etc.
	 * @return
	 */
	public int getKey() { return _ifaceKey; }
	
	/** Set the key for the interface.  This should really only be used when reading/creating
	 * from the database.  Otherwise it should be set using generateKey();
	 * @param value the value to force the key to.
	 */
	public void setKey(int value) { _ifaceKey=value; }
	
	/** This method generates a random long value which can be used for Interface
	 * keys to track them as they get "copied" but we need unique values for them
	 * that are persistent.
	 * @return returns a random int for use as a key
	 */
	public static int generateKey() {
		Random r = new Random();
		return r.nextInt();
	}
	
	/** Checks whether the instance of the interface has a valid IP address
	 * @return false if the IP address is invalid, true otherwise.
	 */
	public boolean hasValidIP() {
		if(_IP==null)
			return false;
		return validateIPAddress(_IP);
	}
	
	/** Checks that the string representation of the ip address is valid.
	 * @param ipAddress the IP address in question.
	 * @return true if the IP address is valid, false otherwise.
	 */
	public final static boolean validateIPAddress( String ipAddress )
	{
	    String[] parts = ipAddress.split( "\\." );

	    if ( parts.length != 4 )
	        return false;

	    for ( String s : parts )
	        if ( (Integer.parseInt( s ) < 0) || (Integer.parseInt( s ) > 255) )
	            return false;

	    return true;
	}
	
	public static String intIPtoString(int IP) {
		byte[] bytes = BigInteger.valueOf(IP).toByteArray();
		try {
			InetAddress address = InetAddress.getByAddress(bytes);
			return address.toString();
		} catch(Exception e) { return null; }
	}
	
	/** Gets the reverse of this specific interfaces IP address;
	 * @return the reverse of the interface's IP.
	 */
	public String getReverseIP() {
		if(_IP==null)
			return null;
		return reverseIPAddress(_IP);
	}
	
	
	/** Checks if the interface's current MAC address is a valid IEEE address.
	 * @return true if the MAC is valid, false otherwise.
	 */
	public boolean hasValidIEEEmac() {
		return validIEEEmac(_MAC);
	}
	
	
	/** Checks the given MAC address for a valid IEEE MAC.
	 * @param mac the MAC address to check
	 * @return true if 'mac' is a valid MAC
	 */
	public static boolean validIEEEmac(String mac) {
		if(mac==null)
			return false;
		return mac.toUpperCase().matches("^([0-9A-F]{2}[:-]){5}([0-9A-F]{2})$");
	}
	
	/** Takes a string representation of an IP addresses and reverts it (e.g., from
	 * big endian to little endian.  Any address can be passed to this.
	 * @param ipAddress the IP to reverse
	 * @return the string representation of the reversed IP, null if IP was invalid.
	 */
	public final static String reverseIPAddress( String ipAddress ) {
		if(!validateIPAddress(ipAddress))
			return null;
		String[] parts = ipAddress.split( "\\." );
		return parts[3] + "." + parts[2] + "." + parts[1] + "." + parts[0];
	}
	
    /** A sorter for interfaces by the string representation of the MAC address. */
    public static Comparator<Object> macsort = new Comparator<Object>() {
    	public int compare(Object arg0, Object arg1) {
    		if(((Interface)arg0)._MAC.compareTo(((Interface)arg1)._MAC)>0)
    			return 1;
    		else if(((Interface)arg0)._MAC.compareTo(((Interface)arg1)._MAC)<0)
    			return -1;
    		else
    			return 0;
    	}
      };
      
      /** A sorter for interfaces by a rank of how we want names to be prioritized.  E.g., give Bluetooth names the highest
       * rank when considering how to name an interface
      */
      static List<? extends Class<?>> interfaceNamingRanks = Arrays.asList(Bluetooth.class, Wifi.class, LAN.class, ZigBee.class);
      public static Comparator<Object> byNameRank = new Comparator<Object>() {
    	  public int compare(Object arg0, Object arg1) {
    		  int arg0Rank = (interfaceNamingRanks.contains(((Interface)arg0)._type)) ? interfaceNamingRanks.indexOf(((Interface)arg0)._type) : -1;
    		  int arg1Rank = (interfaceNamingRanks.contains(((Interface)arg1)._type)) ? interfaceNamingRanks.indexOf(((Interface)arg1)._type) : -1;
    		  
    		  if(arg1Rank < arg0Rank)
    			  return 1;
    		  else if(arg1Rank > arg0Rank)
    			  return -1;
    		  else
    			  return 0;
    	  }
      };

  	
  	/** Converts a string representation of an IEEE MAC address to a byte array
  	 * @param macString the string representation of the MAC
  	 * @return the MAC as a byte array
  	 */
  	public static byte[] macStringToBytes(String macString) {
  	    String[] mac = macString.split(":");
  	    byte[] macAddress = new byte[6];        // mac.length == 6 bytes
  	    for(int i = 0; i < mac.length; i++) {
  	        macAddress[i] = Integer.decode("0x" + mac[i]).byteValue();
  	    }
  	    return macAddress;
  	}
  	
  	/** Helps convert the string representation of an IEEE MAC to long
  	 * @param macString the string representation of the MAC
  	 * @return the long representation of the MAC
  	 */
  	public static long macStringToLong(String macString) {
  		byte[] addr = macStringToBytes(macString);
  		final long address = ((long)addr[5] & 0xff) 
  			    + (((long)addr[4] & 0xff) << 8) 
  			    + (((long)addr[3] & 0xff) << 16) 
  			    + (((long)addr[2] & 0xff) << 24) 
  			    + (((long)addr[1] & 0xff) << 32) 
  			    + (((long)addr[0] & 0xff) << 40);
  		return address;
  	}
  	
  	/** Helps convert a byte representation of an IEEE MAC address to long
  	 * @param macBytes The byte representation of the MAC address.
  	 * @return the long representation of the MAC address.
  	 */
  	public static long macBytesToLong(byte[] macBytes) {
  		final long address = ((long)macBytes[5] & 0xff) 
  			    + (((long)macBytes[4] & 0xff) << 8) 
  			    + (((long)macBytes[3] & 0xff) << 16) 
  			    + (((long)macBytes[2] & 0xff) << 24) 
  			    + (((long)macBytes[1] & 0xff) << 32) 
  			    + (((long)macBytes[0] & 0xff) << 40);
  		return address;
  	}
  	
  	/** This function helps convert a long representation of an IEEE MAC address to a byte array.
  	 * @param addr The MAC address, 'long' format.
  	 * @return the byte array representation of the MAC.
  	 */
  	public static byte[] macLongToBytes(long addr) {
  		byte[] macBytes = new byte[6]; 
  		macBytes[0] = (byte) (addr >> 40);
  		macBytes[1] = (byte) (addr >> 32);
  		macBytes[2] = (byte) (addr >> 24);
  		macBytes[3] = (byte) (addr >> 16);
  		macBytes[4] = (byte) (addr >> 8);
  		macBytes[5] = (byte) addr;
  		return macBytes;
  	}

	/**
	 * Mainly returns the name of the child class
	 * @param c the class to pass
	 * @return a simplified class name as a string
	 */
	public static String simplifiedClassName(Class<?> c) {
		String fullName = c.getName();
		String[] topName = fullName.split("\\.");
		if(topName.length==0)
			return fullName;
		return topName[topName.length-1];
	}
  	
  	/** This function helps convert a byte representation of an IEEE MAC address to a string.
  	 * @param macBytes the byte representation of the MAC
  	 * @return returns the string representation of a MAC (e.g., "aa:bb:cc:dd:ee:ff")
  	 */
  	public static String macBytesToString(byte[] macBytes) {
  	    StringBuilder sb = new StringBuilder(18);
  	    for (byte b : macBytes) {
  	        if (sb.length() > 0)
  	            sb.append(':');
  	        sb.append(String.format("%02x", b));
  	    }
  	    return sb.toString();
  	}

	// ********************************************************************* //
	// This code is to make this class parcelable and needs to be updated if
	// any new members are added to the Device class
	// ********************************************************************* //
	public int describeContents() {
		return this.hashCode();
	}

	public static final Parcelable.Creator<Interface> CREATOR = new Parcelable.Creator<Interface>() {
		public Interface createFromParcel(Parcel in) {
			return new Interface(in);
		}

		public Interface[] newArray(int size) {
			return new Interface[size];
		}
	};
	
	public void writeToParcel(Parcel dest, int parcelableFlags) { writeInterfaceToParcel(dest, parcelableFlags); }
	private Interface(Parcel source) { readInterfaceParcel(source); }
	
	public void writeInterfaceToParcel(Parcel dest, int parcelableFlags) {
		dest.writeString(_MAC);
    	dest.writeString(_IP);
    	dest.writeString(_ouiName);
    	dest.writeString(_ifaceName);
    	dest.writeString(_type.getName());
    	dest.writeInt(_ifaceKey);
	}

	public void readInterfaceParcel(Parcel source) {
		_MAC = source.readString();
        _IP = source.readString();
        _ouiName = source.readString();
        _ifaceName = source.readString();
        try {
        _type = Class.forName(source.readString());
        } catch(Exception e) { Log.e("Interface", "Error getting class in Interface parcel"); }
        _ifaceKey = source.readInt();
	}	
}
