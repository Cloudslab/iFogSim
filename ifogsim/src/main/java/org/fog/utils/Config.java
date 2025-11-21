package org.fog.utils;

public class Config {

	public static final double RESOURCE_MGMT_INTERVAL = 100;
	public static int MAX_SIMULATION_TIME = 2000;
	public static int RESOURCE_MANAGE_INTERVAL = 100;
	public static String FOG_DEVICE_ARCH = "x86";
	public static String FOG_DEVICE_OS = "Linux";
	public static String FOG_DEVICE_VMM = "Xen";
	public static double FOG_DEVICE_TIMEZONE = 10.0;
	public static double FOG_DEVICE_COST = 3.0;
	public static double FOG_DEVICE_COST_PER_MEMORY = 0.05;
	public static double FOG_DEVICE_COST_PER_STORAGE = 0.001;
	public static double FOG_DEVICE_COST_PER_BW = 0.0;
	public static double MAX_VALUE = 1000000.0;

	// Create cluster among devices of same level with common parent irrespective of location. Only one of the two clustering modes should be used for clustering
	public static boolean ENABLE_STATIC_CLUSTERING = false;
	//Dynamic Clustering
	public static boolean ENABLE_DYNAMIC_CLUSTERING = true;
	public static double Node_Communication_RANGE = 300.0; // In terms of meter
	public static double clusteringLatency = 2.0; //milisecond

	public static final int TRANSMISSION_START_DELAY = 50;
}
