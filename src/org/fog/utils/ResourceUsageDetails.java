package org.fog.utils;

public class ResourceUsageDetails {

	private double mips;
	private double uplinkBandwidth;
	private double cpuTrafficIntensity;
	private double nwTrafficIntensity;
	
	public ResourceUsageDetails(double mips, double uplinkBandwidth,
			double cpuTrafficIntensity, double nwTrafficIntensity) {
		super();
		this.mips = mips;
		this.uplinkBandwidth = uplinkBandwidth;
		this.cpuTrafficIntensity = cpuTrafficIntensity;
		this.nwTrafficIntensity = nwTrafficIntensity;
	}
	public double getMips() {
		return mips;
	}
	public void setMips(double mips) {
		this.mips = mips;
	}
	public double getUplinkBandwidth() {
		return uplinkBandwidth;
	}
	public void setUplinkBandwidth(double uplinkBandwidth) {
		this.uplinkBandwidth = uplinkBandwidth;
	}
	public double getCpuTrafficIntensity() {
		return cpuTrafficIntensity;
	}
	public void setCpuTrafficIntensity(double cpuTrafficIntensity) {
		this.cpuTrafficIntensity = cpuTrafficIntensity;
	}
	public double getNwTrafficIntensity() {
		return nwTrafficIntensity;
	}
	public void setNwTrafficIntensity(double nwTrafficIntensity) {
		this.nwTrafficIntensity = nwTrafficIntensity;
	}
	
}
