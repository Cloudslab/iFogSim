package org.fog.gui.core;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class SensorModule extends Node {
	private static final long serialVersionUID = 804858850147477656L;
	
	String sensorType;

	public SensorModule() {
	}

	public SensorModule(String sensorType) {
		super(sensorType, "SENSOR_MODULE");
		setSensorType(sensorType);
	}

	
	public String getSensorType() {
		return sensorType;
	}

	public void setSensorType(String sensorType) {
		this.sensorType = sensorType;
	}

	@Override
	public String toString() {
		return "Node []";
	}

}
