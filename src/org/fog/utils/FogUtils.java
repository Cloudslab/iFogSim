package org.fog.utils;

import java.util.HashMap;
import java.util.Map;

public class FogUtils {
	private static int TUPLE_ID = 1;
	private static int ENTITY_ID = 1;
	private static int ACTUAL_TUPLE_ID = 1;
	
	public static int generateTupleId(){
		return TUPLE_ID++;
	}
	
	public static String getSensorTypeFromSensorName(String sensorName){
		return sensorName.substring(sensorName.indexOf('-')+1, sensorName.lastIndexOf('-'));
	}
	
	public static int generateEntityId(){
		return ENTITY_ID++;
	}
	
	public static int generateActualTupleId(){
		return ACTUAL_TUPLE_ID++;
	}
	
	public static int USER_ID = 1;
	
	//public static int MAX = 10000000;
	public static int MAX = 10000000;
	
	public static Map<String, GeoCoverage> appIdToGeoCoverageMap = new HashMap<String, GeoCoverage>();
}
