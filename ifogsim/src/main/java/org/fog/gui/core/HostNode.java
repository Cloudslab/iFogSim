package org.fog.gui.core;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class HostNode extends Node {
	private static final long serialVersionUID = -8635044061126993668L;
	
	private long pes;
	private long mips;
	private int ram;
	private long storage;
	private long bw;


	public HostNode() {
	}

	public HostNode(String name, String type, long pes, long mips, int ram, long storage, long bw) {
		super(name, type);
		this.pes = pes;
		this.mips = mips;
		this.ram = ram;
		this.storage = storage;
		this.bw = bw;
	}

	public void setPes(long pes) {
		this.pes = pes;
	}

	public long getPes() {
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
	
	public void setStorage(long storage) {
		this.storage = storage;
	}

	public long getStorage() {
		return storage;
	}
	
	public void setBw(long bw) {
		this.bw = bw;
	}

	public long getBw() {
		return bw;
	}	

	@Override
	public String toString() {
		return "Node [pes=" + pes + " mips=" + mips + " ram=" + ram + " storage=" + storage + " bw=" + bw + "]";
	}

}
