package org.fog.utils;

public class NetworkUsageMonitor {

	private static double networkUsage = 0.0;
	
	public static void sendingTuple(double latency, double tupleNwSize){
		networkUsage += latency*tupleNwSize;
	}
	
	public static void sendingModule(double latency, long moduleSize){
		networkUsage += latency*moduleSize;
	}
	
	public static double getNetworkUsage(){
		return networkUsage;
	}
}
