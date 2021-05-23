/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Routing table for hosts and switches. This has information about the next hop.
 * When physical topology is set up, RoutingTable is created with the information
 * about next hop
 *  
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class RoutingTable {
	
	Map<Node, List<Link>> table;

	public RoutingTable(){
		this.table = new HashMap<Node, List<Link>>();
	}
	
	public void clear(){
		table.clear();
	}
	
	public void addRoute(Node destHost, Link to){
		List<Link> links = table.get(destHost);
		if(links == null)
		{
			links = new ArrayList<Link>();
		}
		links.add(to);
		table.put(destHost, links);
	}
	
	public void removeRoute(Node destHost){
		table.remove(destHost);
	}

	public List<Link> getRoute(Node destHost) {
		List<Link> links = table.get(destHost);
		if(links == null)
			links = table.get(null);
		return links;
	}
	
	public Set<Node> getKnownDestination() {
		return table.keySet();
	}
	
	public void printRoutingTable() {
		for(Node key:table.keySet()) {
			for(Link l: table.get(key)) {
				System.out.println("dst:"+key+" : "+l);
			}
		}
	}
}
