package org.fog.application;

/**
 * Class represents application edges which connect modules together and represent data dependency between them.
 * @author Harshit Gupta
 *
 */
public class AppEdge {
	
	public static final int SENSOR = 1; // App Edge originates from a sensor
	public static final int ACTUATOR = 2; // App Edge leads to an actuator
	public static final int MODULE = 3; // App Edge is between application modules
	
	/**
	 * Name of source application module
	 */
	private String source;
	/**
	 * Name of destination application module
	 */
	private String destination;
	/**
	 * CPU length (in MIPS) of tuples carried by the application edge
	 */
	private double tupleCpuLength;
	/**
	 * Network length (in bytes) of tuples carried by the application edge
	 */
	private double tupleNwLength;
	/**
	 * Type of tuples carried by the application edge
	 */
	private String tupleType;
	/**
	 * Direction of tuples carried by the application edge.
	 */
	private int direction;
	private int edgeType;
	
	/**
	 * Periodicity of application edge (in case it is periodic).
	 */
	private double periodicity;
	/**
	 * Denotes if the application edge is a periodic edge.
	 */
	private boolean isPeriodic;
	
	public AppEdge(){
		
	}
	
	public AppEdge(String source, String destination, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int direction, int edgeType){
		setSource(source);
		setDestination(destination);
		setTupleCpuLength(tupleCpuLength);
		setTupleNwLength(tupleNwLength);
		setTupleType(tupleType);
		setDirection(direction);
		setEdgeType(edgeType);
		setPeriodic(false);
	}
	
	public AppEdge(String source, String destination, double periodicity, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int direction, int edgeType){
		setSource(source);
		setDestination(destination);
		setTupleCpuLength(tupleCpuLength);
		setTupleNwLength(tupleNwLength);
		setTupleType(tupleType);
		setDirection(direction);
		setEdgeType(edgeType);
		setPeriodic(true);
		setPeriodicity(periodicity);
	}
	
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}
	public double getTupleCpuLength() {
		return tupleCpuLength;
	}
	public void setTupleCpuLength(double tupleCpuLength) {
		this.tupleCpuLength = tupleCpuLength;
	}
	public double getTupleNwLength() {
		return tupleNwLength;
	}
	public void setTupleNwLength(double tupleNwLength) {
		this.tupleNwLength = tupleNwLength;
	}
	public String getTupleType() {
		return tupleType;
	}
	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getEdgeType() {
		return edgeType;
	}

	public void setEdgeType(int edgeType) {
		this.edgeType = edgeType;
	}

	public double getPeriodicity() {
		return periodicity;
	}

	public void setPeriodicity(double periodicity) {
		this.periodicity = periodicity;
	}

	public boolean isPeriodic() {
		return isPeriodic;
	}

	public void setPeriodic(boolean isPeriodic) {
		this.isPeriodic = isPeriodic;
	}

	@Override
	public String toString() {
		return "AppEdge [source=" + source + ", destination=" + destination
				+ ", tupleType=" + tupleType + "]";
	}
	
	
}
