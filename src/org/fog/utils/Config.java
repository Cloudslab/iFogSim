package org.fog.utils;

public class Config {

	public static final double RESOURCE_MGMT_INTERVAL = 100;
	public static int MAX_SIMULATION_TIME = 20000;
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

	// For periodic placement
	public static final double PLACEMENT_INTERVAL = 50;

	// simulation modes - STATIC - 1(placement happens before simulation start)  DYNAMIC - 2(placement happens after simulation starts)
	public static String SIMULATION_MODE = "DYNAMIC";

	//Placement Request Processing Mode
	public static String PERIODIC = "Periodic";
	public static String SEQUENTIAL = "Sequential";
	public static String PR_PROCESSING_MODE = SEQUENTIAL;

	//Resource info sharing among cluster nodes
	public static Boolean ENABLE_RESOURCE_DATA_SHARING = true;
	public static double MODULE_DEPLYMENT_TIME = 200;
	public static final int TRANSMISSION_START_DELAY = 500;

	//Dynamic Clustering
	public static Boolean ENABLE_DYNAMIC_CLUSTERING = true;
	public static double Node_Communication_RANGE = 300.0; // In terms of meter
	public static double clusteringLatency = 2.0; //milisecond

	
}
