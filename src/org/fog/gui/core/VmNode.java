package org.fog.gui.core;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class VmNode extends Node {
	private static final long serialVersionUID = 804858850147477656L;
	
	private long size;
	private int pes;
	private long mips;
	private int ram;
	

	public VmNode() {
	}

	public VmNode(String name, String type, long size, int pes, long mips, int ram) {
		super(name, type);
		this.size = size;
		this.pes = pes;
		this.mips = mips;
		this.ram = ram;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public long getSize() {
		return size;
	}

	public void setPes(int pes) {
		this.pes = pes;
	}

	public int getPes() {
		return pes;
	}
	public void setMips(long mips) {
		this.mips = mips;
	}

	public long getMips() {
		return mips;
	}
	public void setRam(int ram) {
		this.ram = ram;
	}

	public int getRam() {
		return ram;
	}	

	@Override
	public String toString() {
		return "Node [size=" + size + " pes=" + pes + " mips=" + mips + " ram=" + ram + "]";
	}

}
