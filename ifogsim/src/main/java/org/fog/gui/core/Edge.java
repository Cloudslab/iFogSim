package org.fog.gui.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The model that represents an edge with two vertexes, for physical link and virtual edge.
 * 
 */
public class Edge implements Serializable {
	private static final long serialVersionUID = -356975278987708987L;

	private Node dest = null;
	
	private double latency = 0.0;
	private String name = "";
	private long bandwidth = 0;

	/**
	 * Constructor.
	 * 
	 * @param node the node that belongs to the edge.
	 */
	public Edge(Node to) {
		this.dest = to;
	}
	
	/** physical topology link */
	public Edge(Node to, double latency) {
		this.dest = to;
		this.latency = latency;
	}

	/** virtual virtual edge */
	public Edge(Node to, String name, long bw) {
		this.dest = to;
		this.name = name;
		this.bandwidth = bw;
	}
	
	/** copy edge */
	public Edge(Node to, Map<String, Object> info){
		this.dest = to;
		if(info.get("name")!=null){
			this.name = (String) info.get("name");
		}
		if(info.get("bandwidth")!=null){
			this.bandwidth = (long) info.get("bandwidth");
		}
		if(info.get("latency")!=null){
			this.latency = (double) info.get("latency");
		}
	}

	public Node getNode() {
		return dest;
	}

	public long getBandwidth() {
		return bandwidth;
	}
	
	public double getLatency() {
		return latency;
	}
	
	public Map<String, Object> getInfo() {
		Map<String, Object> info = new HashMap<String, Object>();
		info.put("name", this.name);
		info.put("bandwidth",this.bandwidth);
		info.put("latency", this.latency);
		return info;
	}
	
	public void setInfo(Map<String, Object> info){
		if(info.get("name")!=null){
			this.name = (String) info.get("name");
		}
		if(info.get("bandwidth")!=null){
			this.bandwidth = (long) info.get("bandwidth");
		}
		if(info.get("latency")!=null){
			this.latency = (double) info.get("latency");
		}
	}

	@Override
	public String toString() {
		return "Edge [dest=" + dest + "]";
	}

}
