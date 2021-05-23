package org.fog.entities;

import java.util.List;

public class PhysicalTopology {
	
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}
	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}
	public List<Sensor> getSensors() {
		return sensors;
	}
	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}
	public List<Actuator> getActuators() {
		return actuators;
	}
	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

}
