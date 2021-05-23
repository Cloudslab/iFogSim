package org.fog.utils;

public class TupleFinishDetails {

	private String queryId;
	private int actualTupleId;
	private double emitTime;
	private double finishTime;
	private String sensorType;
	public TupleFinishDetails(String queryId, int actualTupleId, double emitTime, double finishTime, String sensorType){
		this.queryId = queryId;
		this.actualTupleId = actualTupleId;
		this.emitTime = emitTime;
		this.finishTime = finishTime;
		this.sensorType = sensorType;
	}
	
	public String getQueryId() {
		return queryId;
	}
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}
	public int getActualTupleId() {
		return actualTupleId;
	}
	public void setActualTupleId(int actualTupleId) {
		this.actualTupleId = actualTupleId;
	}
	public double getEmitTime() {
		return emitTime;
	}
	public void setEmitTime(double emitTime) {
		this.emitTime = emitTime;
	}
	public double getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(double finishTime) {
		this.finishTime = finishTime;
	}

	public String getSensorType() {
		return sensorType;
	}

	public void setSensorType(String sensorType) {
		this.sensorType = sensorType;
	}
	
}
