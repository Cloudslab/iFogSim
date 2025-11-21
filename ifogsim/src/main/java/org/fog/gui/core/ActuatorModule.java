package org.fog.gui.core;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class ActuatorModule extends Node {
	private static final long serialVersionUID = 804858850147477656L;
	
	String actuatorType;

	public ActuatorModule() {
	}

	public ActuatorModule(String actuatorType) {
		super(actuatorType, "ACTUATOR_MODULE");
		setActuatorType(actuatorType);
	}

	
	public String getActuatorType() {
		return actuatorType;
	}

	public void setActuatorType(String actuatorType) {
		this.actuatorType = actuatorType;
	}

	@Override
	public String toString() {
		return "Node []";
	}

}
