package org.fog.utils;

public class CanBeSentResult {

	private double cpuLoad;
	private double nwLoad;
	
	private boolean canBeSent;

	public CanBeSentResult(double cpuLoad, double nwLoad, boolean canBeSent){
		this.cpuLoad = cpuLoad;
		this.nwLoad = nwLoad;
		this.canBeSent = canBeSent;
	}
	
	public CanBeSentResult() {
		// TODO Auto-generated constructor stub
	}

	public double getCpuLoad() {
		return cpuLoad;
	}

	public void setCpuLoad(double cpuLoad) {
		this.cpuLoad = cpuLoad;
	}

	public double getNwLoad() {
		return nwLoad;
	}

	public void setNwLoad(double nwLoad) {
		this.nwLoad = nwLoad;
	}

	public boolean isCanBeSent() {
		return canBeSent;
	}

	public void setCanBeSent(boolean canBeSent) {
		this.canBeSent = canBeSent;
	}
	
	
	
}
