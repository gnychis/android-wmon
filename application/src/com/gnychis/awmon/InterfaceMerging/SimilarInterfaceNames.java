package com.gnychis.awmon.InterfaceMerging;

import java.util.Arrays;

import android.content.Context;
import android.util.Log;

import com.gnychis.awmon.DeviceAbstraction.InterfacePair;
import com.gnychis.awmon.HardwareHandlers.Bluetooth;
import com.gnychis.awmon.HardwareHandlers.LAN;
import com.gnychis.awmon.HardwareHandlers.Wifi;
import com.gnychis.awmon.HardwareHandlers.ZigBee;

/** 
 * The purpose of this heuristic is to merge interfaces with similar names, this is common with
 * devices that have multiple interfaces that are named things like: "George's MacBook"
 * 
 * @author George Nychis (gnychis)
 */
public class SimilarInterfaceNames extends MergeHeuristic {
	
	private static final String TAG = "SimilarInterfaceNames";
	private static final boolean VERBOSE = false;
	
	private static final int TOLERABLE_STRING_DIFFERENCE = 2;

	@SuppressWarnings("unchecked")
	public SimilarInterfaceNames(Context p) {
		super(p,Arrays.asList(Wifi.class, Bluetooth.class, LAN.class, ZigBee.class));
	}

	public MergeStrength classifyInterfacePair(InterfacePair pair) {
		
		// Use LevenshteinDistance to calculate the difference in a pair of strings, and if
		// that distance is less than our tolerable distance, we consider their names to
		// be highly similar.
		if(pair.getLeft()._ifaceName!=null && pair.getRight()._ifaceName!=null) {
			debugOut("Comparing -" + pair.getLeft()._ifaceName + "-  to  -" + pair.getRight()._ifaceName + "-");
			if(computeLevenshteinDistance(pair.getLeft()._ifaceName, pair.getRight()._ifaceName)
					<= TOLERABLE_STRING_DIFFERENCE) {
				debugOut("... likely");
				return MergeStrength.LIKELY;
			}
			debugOut("...unlikely");
		}
		
		return MergeStrength.UNDETERMINED;	
	}

	// This code is compliments of:
	//  http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}
	
	// This code is compliments of:
	//  http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
	public static int computeLevenshteinDistance(CharSequence str1,
			CharSequence str2) {
		
		if(str1==null || str2==null)
			return 1000000;		// invalid
		
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for (int j = 1; j <= str2.length(); j++)
			distance[0][j] = j;

		for (int i = 1; i <= str1.length(); i++)
			for (int j = 1; j <= str2.length(); j++)
				distance[i][j] = minimum(
						distance[i - 1][j] + 1,
						distance[i][j - 1] + 1,
						distance[i - 1][j - 1]
								+ ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
										: 1));

		return distance[str1.length()][str2.length()];
	}
	
	private void debugOut(String msg) {
		if(VERBOSE)
			Log.d(TAG, msg);
	}
}
