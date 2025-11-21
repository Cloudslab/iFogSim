package org.cloudbus.cloudsim.sdn.graph.core;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class SwitchNode extends Node {
	private static final long serialVersionUID = 804858850147477656L;
	
	private long iops;
	private int upports;
	private int downports;
	private long bw;
	

	public SwitchNode() {
	}

	public SwitchNode(String name, String type, long iops, int upports, int downports, long bw) {
		super(name, type);
		this.iops = iops;
		this.upports = upports;
		this.downports = downports;
		this.bw = bw;
	}

	public void setIops(long iops) {
		this.iops = iops;
	}

	public long getIops() {
		return iops;
	}

	public void setUpports(int upports) {
		this.upports = upports;
	}

	public int getUpports() {
		return upports;
	}
	public void setDownports(int downports) {
		this.downports = downports;
	}

	public int getDownports() {
		return downports;
	}
	public void setBw(long bw) {
		this.bw = bw;
	}

	public long getBw() {
		return bw;
	}	

	@Override
	public String toString() {
		return "Node [iops=" + iops + " upports=" + upports + " downports=" + downports + " bw=" + bw + "]";
	}

}
