package org.fog.utils;

public class OperatorEdge {
	
	private String src;
	private String dst;
	private double selectivity;

	public OperatorEdge(String src, String dst, double selectivity){
		this.src = src;
		this.dst = dst;
		this.selectivity = selectivity;
	}
	
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getDst() {
		return dst;
	}
	public void setDst(String dst) {
		this.dst = dst;
	}
	public double getSelectivity() {
		return selectivity;
	}
	public void setSelectivity(double selectivity) {
		this.selectivity = selectivity;
	}
	
}
