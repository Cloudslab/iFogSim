package org.cloudbus.cloudsim.sdn.graph.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A graph model. Normally a model should not have any logic, but in this case we implement logic to manipulate the
 * adjacencyList like reorganizing, adding nodes, removing nodes, e.g
 *
 */
public class Graph implements Serializable {
	private static final long serialVersionUID = 745864022429447529L;
	
	private Map<Node, List<Edge>> adjacencyList;


	public Graph() {
		// when creating a new graph ensure that a new adjacencyList is created
		adjacencyList = new HashMap<Node, List<Edge>>();
	}

	public Graph(Map<Node, List<Edge>> adjacencyList) {
		this.adjacencyList = adjacencyList;
	}

	public void setAdjacencyList(Map<Node, List<Edge>> adjacencyList) {
		this.adjacencyList = adjacencyList;
	}

	public Map<Node, List<Edge>> getAdjacencyList() {
		return adjacencyList;
	}

	/** Adds a given edge to the adjacency list. If the base node is not yet part of the adjacency list a new entry is added */
	public void addEdge(Node key, Edge value) {

		if (adjacencyList.containsKey(key)) {
			if (adjacencyList.get(key) == null) {
				adjacencyList.put(key, new ArrayList<Edge>());
			}
			// TODO: perhaps check if a value may not be added twice.
			// add edge if not null
			if (value != null) {
				adjacencyList.get(key).add(value);
			}
		} else {
			List<Edge> edges = new ArrayList<Edge>();
			// add edge if not null
			if (value != null) {
				edges.add(value);
			}

			adjacencyList.put(key, edges);
		}

		// do bidirectional adding. Ugly duplicated code.
		// only execute when there is an edge defined.
		if (value != null) {
			Edge reverseEdge = new Edge(key, value.getInfo());

			if (adjacencyList.containsKey(value.getNode())) {
				if (adjacencyList.get(value.getNode()) == null) {
					adjacencyList.put(value.getNode(), new ArrayList<Edge>());
				}
				// TODO: perhaps check if a value may not be added twice.
				// add edge if not null
				if (reverseEdge != null) {
					adjacencyList.get(value.getNode()).add(reverseEdge);
				}
			} else {
				List<Edge> edges = new ArrayList<Edge>();
				// add edge if not null
				if (reverseEdge != null) {
					edges.add(reverseEdge);
				}

				adjacencyList.put(value.getNode(), edges);
			}
		}
	}

	/** Simply adds a new node, without setting any edges */
	public void addNode(Node node) {
		addEdge(node, null);
	}

	public void removeEdge(Node key, Edge value) {

		if (!adjacencyList.containsKey(key)) {
			throw new IllegalArgumentException("The adjacency list does not contain a node for the given key: " + key);
		}
		List<Edge> edges = adjacencyList.get(key);

		if (!edges.contains(value)) {
			throw new IllegalArgumentException("The list of edges does not contain the given edge to remove: " + value);
		}

		edges.remove(value);
		// remove bidirectional
		List<Edge> reverseEdges = adjacencyList.get(value.getNode());
		List<Edge> toRemove = new ArrayList<Edge>();
		for (Edge edge : reverseEdges) {
			if (edge.getNode().equals(key)) {
				toRemove.add(edge);
			}
		}
		//normally only one element
		reverseEdges.removeAll(toRemove);
	}

	/** Deletes a node */
	public void removeNode(Node key) {

		if (!adjacencyList.containsKey(key)) {
			throw new IllegalArgumentException("The adjacency list does not contain a node for the given key: " + key);
		}

		adjacencyList.remove(key);

		// clean up all edges
		for (Entry<Node, List<Edge>> entry : adjacencyList.entrySet()) {

			List<Edge> toRemove = new ArrayList<Edge>();

			for (Edge edge : entry.getValue()) {
				if (edge.getNode().equals(key)) {
					toRemove.add(edge);
				}
			}
			entry.getValue().removeAll(toRemove);
		}
	}
	
	public void clearGraph(){
		adjacencyList.clear();
	}
	
	public String toJsonString(){
		String jsonText = Bridge.graphToJson(this);
		return jsonText;
	}
	

	@Override
	public String toString() {
		return "Graph [adjacencyList=" + adjacencyList + "]";
	}

}
