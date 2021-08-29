package org.fog.utils;

public class MicroservicePlacementConfig {

    // simulation modes - STATIC - 1(initial placement happens before simulation start and placement related delays are not simulated)
    // DYNAMIC - 2(placement happens after simulation starts and placement related delays are simulated)
    public static String SIMULATION_MODE = "STATIC";

    //Placement Request Processing Mode
    public static String PERIODIC = "Periodic";
    public static String SEQUENTIAL = "Sequential";
    public static String PR_PROCESSING_MODE = PERIODIC;

    // For periodic placement
    public static final double PLACEMENT_INTERVAL = 1;

    //Resource info sharing among cluster nodes
    public static Boolean ENABLE_RESOURCE_DATA_SHARING = false;
    public static double MODULE_DEPLOYMENT_TIME = 0.0;


}
