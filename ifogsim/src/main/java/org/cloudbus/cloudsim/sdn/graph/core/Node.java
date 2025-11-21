package org.cloudbus.cloudsim.sdn.graph.core;

import java.io.Serializable;

import org.cloudbus.cloudsim.sdn.graph.core.Coordinates;

/**
 * The model that represents node (host or vm) for the graph.
 * 
 */
public class Node implements Serializable {
	private static final long serialVersionUID = 823544330517091616L;

	private Coordinates coord;
	private String name;
	private String type;

	public Node() {
	}

	public Node(String name, String type) {
		this.name = name;
		this.type = type;
		coord = new Coordinates();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}
	
	public void setCoordinate(Coordinates coord) {
		this.coord.setX(coord.getX());
		this.coord.setY(coord.getY());
	}

	public Coordinates getCoordinate() {
		return coord;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Node [name=" + name + " type=" + type + "]";
	}

}
