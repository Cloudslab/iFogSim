package org.fog.gui.core;

import java.io.Serializable;

public class ActuatorGui extends Node implements Serializable{

	private static final long serialVersionUID = 4087896123649020073L;

	private String name;
	private String actuatorType;
	
	public ActuatorGui(String name, String actuatorType){
		super(name, "ACTUATOR");
		setName(name);
		setActuatorType(actuatorType);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Actuator []";
	}

	public String getActuatorType() {
		return actuatorType;
	}

	public void setActuatorType(String actuatorType) {
		this.actuatorType = actuatorType;
	}

	
}
