package org.cloudbus.cloudsim.sdn.graph.core;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Bridge {
	
	private static Node getNode(Graph graph, String name){
		for(Node node : graph.getAdjacencyList().keySet()){
			if(node!=null){
				if(node.getName().equals(name)){
					return node;
				}
			}
		}
		return null;
	}

	// convert from JSON object to Graph object
	public static Graph jsonToGraph(String fileName, int type){
		
		Graph graph = new Graph();
		
		// type 0->physical topology 1->virtual topology
		if(0 == type){
			try {
				JSONObject doc = (JSONObject) JSONValue.parse(new FileReader(fileName));
	    		JSONArray nodes = (JSONArray) doc.get("nodes");
	    		@SuppressWarnings("unchecked")
				Iterator<JSONObject> iter =nodes.iterator(); 
				while(iter.hasNext()){
					JSONObject node = iter.next();
					String nodeType = (String) node.get("type");
					String nodeName = (String) node.get("name");
					
					if(nodeType.equalsIgnoreCase("host")){  //host
						long pes = (Long) node.get("pes");
						long mips = (Long) node.get("mips");
						int ram = new BigDecimal((Long)node.get("ram")).intValueExact();
						long storage = (Long) node.get("storage");
						long bw = new BigDecimal((Long)node.get("bw")).intValueExact();
						
						int num = 1;
						if (node.get("nums")!= null)
							num = new BigDecimal((Long)node.get("nums")).intValueExact();

						for(int n = 0; n< num; n++) {
							Node hNode = new HostNode(nodeName, nodeType, pes, mips, ram, storage, bw);
							graph.addNode(hNode);
						}
						
					} else {   //switch
						int bw = new BigDecimal((Long)node.get("bw")).intValueExact();
						long iops = (Long) node.get("iops");
						int upports =  new BigDecimal((Long)node.get("upports")).intValueExact();
						int downports = new BigDecimal((Long)node.get("downports")).intValueExact();
						
						Node sNode = new SwitchNode(nodeName, nodeType, iops, upports, downports, bw);
						graph.addNode(sNode);
					}
				}
					
				JSONArray links = (JSONArray) doc.get("links");
				@SuppressWarnings("unchecked")
				Iterator<JSONObject> linksIter =links.iterator(); 
				while(linksIter.hasNext()){
					JSONObject link = linksIter.next();
					String src = (String) link.get("source");  
					String dst = (String) link.get("destination");
					double lat = (Double) link.get("latency");
					
					Node source = (Node) getNode(graph, src);
					Node target = (Node) getNode(graph, dst);

					
					if(source!=null && target!=null){
						Edge edge = new Edge(target, lat);
						graph.addEdge(source, edge);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
		}else if(1 == type){
			try {
				JSONObject doc = (JSONObject) JSONValue.parse(new FileReader(fileName));
	    		JSONArray nodes = (JSONArray) doc.get("nodes");
	    		@SuppressWarnings("unchecked")
				Iterator<JSONObject> iter = nodes.iterator(); 
				while(iter.hasNext()){
					JSONObject node = iter.next();
					
					String nodeType = (String) node.get("type");
					String nodeName = (String) node.get("name");
					int pes = new BigDecimal((Long)node.get("pes")).intValueExact();
					long mips = (Long) node.get("mips");
					int ram = new BigDecimal((Long)node.get("ram")).intValueExact();
					long size = (Long) node.get("size");
					
					Node vmNode = new VmNode(nodeName, nodeType, size, pes, mips, ram);
					graph.addNode(vmNode);
				}
				
				JSONArray links = (JSONArray) doc.get("links");
				
				@SuppressWarnings("unchecked")
				Iterator<JSONObject> linksIter = links.iterator(); 
				while(linksIter.hasNext()){
					JSONObject link = linksIter.next();
					String name = (String) link.get("name");
					String src = (String) link.get("source");  
					String dst = (String) link.get("destination");

					Object reqBw = link.get("bandwidth");
				
					long bw = 0;
					if(reqBw != null)
						bw = (Long) reqBw;
					
					Node source = getNode(graph, src);
					Node target = getNode(graph, dst);

					Edge edge = new Edge(target, name, bw);
					graph.addEdge(source, edge);
					
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		return graph;
	}
	
	// convert from Graph object to JSON object
	@SuppressWarnings("unchecked")
	public static String graphToJson(Graph graph){
		if(graph.getAdjacencyList().size() < 1){
			return "Graph is Empty";
		}
		Map<Node, List<Node>> edgeList = new HashMap<Node, List<Node>>();
		
		JSONObject topo = new JSONObject();
		JSONArray nodes = new JSONArray();
		JSONArray links = new JSONArray();
		
		for (Entry<Node, List<Edge>> entry : graph.getAdjacencyList().entrySet()) {
			Node srcNode = entry.getKey();
			
			// add node
			JSONObject jobj = new JSONObject();
			switch(srcNode.getType()){
				case "host":
					HostNode hNode = (HostNode)srcNode;
					jobj.put("name", hNode.getName());
					jobj.put("type", hNode.getType());
					jobj.put("pes", hNode.getPes());
					jobj.put("mips", hNode.getMips());
					jobj.put("ram", hNode.getRam());
					jobj.put("storage", hNode.getStorage());
					jobj.put("bw", hNode.getBw());
					break;
				case "core":
				case "edge":
				    SwitchNode sNode = (SwitchNode)srcNode;
					jobj.put("name", sNode.getName());
					jobj.put("type", sNode.getType());
					jobj.put("iops", sNode.getIops());
					jobj.put("upports", sNode.getDownports());
					jobj.put("downports", sNode.getDownports());
					jobj.put("bw", sNode.getBw());
					break;
				case "vm":
					VmNode vNode = (VmNode)srcNode;
					jobj.put("name", vNode.getName());
					jobj.put("type", vNode.getType());
					jobj.put("size", vNode.getSize());
					jobj.put("pes", vNode.getPes());
					jobj.put("mips", vNode.getMips());
					jobj.put("ram", vNode.getRam());
					break;
			}
			nodes.add(jobj);
			
			// add edge
			for (Edge edge : entry.getValue()) {
				Node destNode = edge.getNode();
				
				// check if edge exist (dest->src)
				if (edgeList.containsKey(destNode) && edgeList.get(destNode).contains(srcNode)) {
					continue;
				}
				
				JSONObject jobj2 = new JSONObject();
				jobj2.put("source", srcNode.getName());
				jobj2.put("destination", destNode.getName());
				if("host"==destNode.getType() || "core"==destNode.getType() || "edge"==destNode.getType()){
					jobj2.put("latency", edge.getLatency());
				}else if("vm"==destNode.getName()){
					if(edge.getBandwidth()>0){
						jobj2.put("bandwidth", edge.getBandwidth());
					}
				}
				links.add(jobj2);
				
				// add exist edge to the edgeList
				if (edgeList.containsKey(entry.getKey())) {
					edgeList.get(entry.getKey()).add(edge.getNode());
				} else {
					List<Node> ns = new ArrayList<Node>();
					ns.add(edge.getNode());
					edgeList.put(entry.getKey(), ns);
				}
				
			}
		}
		topo.put("nodes", nodes);
		topo.put("links", links);
		
		StringWriter out = new StringWriter();
		String jsonText = "";
		try {
			topo.writeJSONString(out);
			jsonText = out.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//System.out.println(jsonText);
		return jsonText;
	}
	
}
