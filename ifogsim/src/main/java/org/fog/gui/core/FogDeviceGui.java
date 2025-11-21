package org.fog.gui.core;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class FogDeviceGui extends Node {
	private static final long serialVersionUID = -8635044061126993668L;
	
	private int level;
	private String name;
	private long mips;
	private int ram;
	private long upBw;
	private long downBw;
	private double ratePerMips;
	
	public FogDeviceGui() {
	}

	public FogDeviceGui(String name, long mips, int ram, long upBw, long downBw, int level, double rate) {
		super(name, "FOG_DEVICE");
		this.name = name;
		this.mips = mips;
		this.ram = ram;
		this.upBw = upBw;
		this.downBw = downBw;
		this.level = level;
		this.ratePerMips = rate;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getMips() {
		return mips;
	}

	public void setMips(long mips) {
		this.mips = mips;
	}

	public int getRam() {
		return ram;
	}

	public void setRam(int ram) {
		this.ram = ram;
	}

	public long getUpBw() {
		return upBw;
	}

	public void setUpBw(long upBw) {
		this.upBw = upBw;
	}

	public long getDownBw() {
		return downBw;
	}

	public void setDownBw(long downBw) {
		this.downBw = downBw;
	}

	@Override
	public String toString() {
		return "FogDevice [mips=" + mips + " ram=" + ram + " upBw=" + upBw + " downBw=" + downBw + "]";
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getRatePerMips() {
		return ratePerMips;
	}

	public void setRatePerMips(double ratePerMips) {
		this.ratePerMips = ratePerMips;
	}

}
