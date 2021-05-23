/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.example.policies.VmSchedulerTimeSharedEnergy;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * NOS calculates and estimates network behaviour. It also mimics SDN Controller functions.  
 * It manages channels between switches, and assigns packages to channels and control their completion
 * Once the transmission is completed, forward the packet to the destination.
 *  
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public abstract class NetworkOperatingSystem extends SimEntity {

	String physicalTopologyFileName; 
	protected PhysicalTopology topology;
	//Hashtable<Integer,SDNHost> vmHostTable;
	Hashtable<Package,Node> pkgTable;
	
	Hashtable<String, Channel> channelTable;

	List<Host> hosts;
	protected List<SDNHost> sdnhosts;
	protected List<Switch> switches= new ArrayList<Switch>();
	int vmId=0;
	protected SDNDatacenter datacenter;
	protected LinkedList<Vm> vmList;
	protected LinkedList<Arc> arcList;
	Map<Integer, Arc> flowIdArcTable;
	Map<String, Integer> vmNameIdTable;
	Map<String, Integer> flowNameIdTable;
	
	public static Map<Integer, String> debugVmIdName = new HashMap<Integer, String>();
	public static Map<Integer, String> debugFlowIdName = new HashMap<Integer, String>();
	
	boolean isApplicationDeployed = false;
	
	// Resolution of the result.
	public static double minTimeBetweenEvents = 0.001;	// in sec
	public static int resolutionPlaces = 5;
	public static int timeUnit = 1;	// 1: sec, 1000: msec
	



	/**
	 * 1. map VMs and middleboxes to hosts, add the new vm/mb to the vmHostTable, advise host, advise dc
	 * 2. set channels and bws
	 * 3. set routing tables to restrict hops to meet latency
	 */
	protected abstract boolean deployApplication(List<Vm> vms, List<Middlebox> middleboxes, List<Arc> links);
	protected abstract Middlebox deployMiddlebox(String type, Vm vm);

	public NetworkOperatingSystem(String fileName) {
		super("NOS");
		
		this.physicalTopologyFileName = fileName;
		
		this.pkgTable = new Hashtable<Package, Node>();
		this.channelTable = new Hashtable<String, Channel>();
		
		initPhysicalTopology();
	}

	public static double getMinTimeBetweenNetworkEvents() {
	    return minTimeBetweenEvents* timeUnit;
	}
	public static double round(double value) {
		int places = resolutionPlaces;
	    if (places < 0) throw new IllegalArgumentException();

		if(timeUnit >= 1000) value = Math.floor(value*timeUnit);
		
	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.CEILING);
	    return bd.doubleValue();
	    //return value;
	}
	

	
	@Override
	public void startEntity() {}

	@Override
	public void shutdownEntity() {}
	
	@Override
	public void processEvent(SimEvent ev) {
		int tag = ev.getTag();
		
		switch(tag){
			case Constants.SDN_INTERNAL_PACKAGE_PROCESS: 
				internalPackageProcess(); 
				break;
			case CloudSimTags.VM_CREATE_ACK:
				processVmCreateAck(ev);
				break;
			case CloudSimTags.VM_DESTROY:
				processVmDestroyAck(ev);
				break;
			default: System.out.println("Unknown event received by "+super.getName()+". Tag:"+ev.getTag());
		}
	}

	public void processVmCreateAck(SimEvent ev) {
	}
	protected void processVmDestroyAck(SimEvent ev) {
		Vm destroyedVm = (Vm) ev.getData();
		// remove all channels transferring data from or to this vm.
		for(Vm vm:this.vmList) {
			Channel ch = this.findChannel(vm.getId(), destroyedVm.getId(), -1);
			if(ch != null) {
				this.removeChannel(getKey(vm.getId(), destroyedVm.getId(), -1));
			}

			ch = this.findChannel(destroyedVm.getId(), vm.getId(), -1);
			if(ch != null) {
				this.removeChannel(getKey(destroyedVm.getId(), vm.getId(), -1));
			}

		}
		
		sendInternalEvent();
		
	}

	public void addPackageToChannel(Node sender, Package pkg) {
		int src = pkg.getOrigin();
		int dst = pkg.getDestination();
		int flowId = pkg.getFlowId();
					
		if(sender.equals(sender.getVMRoute(src, dst, flowId))) {
			// For loopback packet (when src and dst is on the same host)
			//Log.printLine(CloudSim.clock() + ": " + getName() + ".addPackageToChannel: Loopback package: "+pkg +". Send to destination:"+dst);
			sendNow(sender.getAddress(),Constants.SDN_PACKAGE,pkg);
			return;
		}
		
		updatePackageProcessing();
		
		pkgTable.put(pkg,sender);
		
		Channel channel=findChannel(src, dst, flowId);
		if(channel == null) {
			//No channel establisihed. Add a channel.
			channel = createChannel(src, dst, flowId, sender);
			
			if(channel == null) {
				// failed to create channel
				return;
			}
			addChannel(src, dst, flowId, channel);
		}
		
		double eft = channel.addTransmission(new Transmission(pkg));
		Log.printLine(CloudSim.clock() + ": " + getName() + ".addPackageToChannel ("+channel
				+"): Transmission added:" + 
				NetworkOperatingSystem.debugVmIdName.get(src) + "->"+
				NetworkOperatingSystem.debugVmIdName.get(dst) + ", flow ="+flowId + " / eft="+eft);

		sendInternalEvent();
	}
	

	private void internalPackageProcess() {
		if(updatePackageProcessing()) {
			sendInternalEvent();
		}
	}
	
	private void sendInternalEvent() {
		CloudSim.cancelAll(getId(), new PredicateType(Constants.SDN_INTERNAL_PACKAGE_PROCESS));
		
		if(channelTable.size() != 0) {
			// More to process. Send event again
			double delay = this.nextFinishTime();
			Log.printLine(CloudSim.clock() + ": " + getName() + ".sendInternalEvent(): next finish time: "+ delay);
			
			send(this.getId(), delay, Constants.SDN_INTERNAL_PACKAGE_PROCESS);
		}
	}
	
	private double nextFinishTime() {
		double earliestEft = Double.POSITIVE_INFINITY;
		for(Channel ch:channelTable.values()){
			
			double eft = ch.nextFinishTime();
			if (eft<earliestEft){
				earliestEft=eft;
			}
		}
		
		if(earliestEft == Double.POSITIVE_INFINITY) {
			throw new IllegalArgumentException("NOS.nextFinishTime(): next finish time is infinite!");
		}
		return earliestEft;
		
	}
	
	private boolean updatePackageProcessing() {
		boolean needSendEvent = false;
		
		LinkedList<Channel> completeChannels = new LinkedList<Channel>();
		
		for(Channel ch:channelTable.values()){
			boolean isCompleted = ch.updatePackageProcessing();
			needSendEvent = needSendEvent || isCompleted;
			//completeChannels.add(ch.getArrivedPackages());
			completeChannels.add(ch);
		}
		
		if(completeChannels.size() != 0) {
			processCompletePackages(completeChannels);
			updateChannel();
		}

		return needSendEvent;
	}
	
	private void processCompletePackages(List<Channel> channels){
		for(Channel ch:channels) {
			
			Node dest = ch.getLastNode();
			
			for (Transmission tr:ch.getArrivedPackages()){
				Package pkg = tr.getPackage();
				//Node sender = pkgTable.remove(pkg);
				//Node nextHop = sender.getRoute(pkg.getOrigin(),pkg.getDestination(),pkg.getFlowId());
				
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Package completed: "+pkg +". Send to destination:"+dest);
				sendNow(dest.getAddress(),Constants.SDN_PACKAGE,pkg);
			}
		}
	}
	
	public Map<String, Integer> getVmNameIdTable() {
		return this.vmNameIdTable;
	}
	public Map<String, Integer> getFlowNameIdTable() {
		return this.flowNameIdTable;
	}
	
	private Channel findChannel(int from, int to, int channelId) {
		// check if there is a pre-configured channel for this application
		Channel channel=channelTable.get(getKey(from,to, channelId));

		if (channel == null) {
			//there is no channel for specific flow, find the default channel for this link
			channel=channelTable.get(getKey(from,to));
		}
		return channel;
	}
	
	private void addChannel(int src, int dst, int chId, Channel ch) {
		//System.err.println("NOS.addChannel:"+getKey(src, dst, chId));
		this.channelTable.put(getKey(src, dst, chId), ch);
		ch.initialize();
		adjustAllChannels();
	}
	
	private Channel removeChannel(String key) {
		//System.err.println("NOS.removeChannel:"+key);
		Channel ch = this.channelTable.remove(key);
		ch.terminate();
		adjustAllChannels();
		return ch;
	}
	
	private void adjustAllChannels() {
		for(Channel ch:this.channelTable.values()) {
			if(ch.adjustDedicatedBandwidthAlongLink()) {
				// Channel BW is changed. send event.
			}
		}
		
		for(Channel ch:this.channelTable.values()) {
			if(ch.adjustSharedBandwidthAlongLink()) {
				// Channel BW is changed. send event.
			}
		}
	}

	private Channel createChannel(int src, int dst, int flowId, Node srcNode) {
		List<Node> nodes = new ArrayList<Node>();
		List<Link> links = new ArrayList<Link>();
		
		Node origin = srcNode;
		Node dest = origin.getVMRoute(src, dst, flowId);
		
		if(dest==null)
			return null;
		
		Link link;
		double lowestBw = Double.POSITIVE_INFINITY;
		double reqBw = 0;
		if(flowId != -1) {
			Arc flow = this.flowIdArcTable.get(flowId);
			reqBw = flow.getBw();			
		}
		
		nodes.add(origin);
		
		while(true) {
			link = this.topology.getLink(origin.getAddress(), dest.getAddress());
			links.add(link);
			nodes.add(dest);
			
			if(lowestBw > link.getFreeBandwidth(origin)) {
				lowestBw = link.getFreeBandwidth(origin);
			}
		
			if(dest instanceof SDNHost)
				break;
			
			origin = dest;
			dest = origin.getVMRoute(src, dst, flowId);
		} 
		
		if(flowId != -1 && lowestBw < reqBw) {
			// free bandwidth is less than required one.
			// Cannot make channel.
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Free bandwidth is less than required.("+getKey(src,dst,flowId)+"): ReqBW="+ reqBw + "/ Free="+lowestBw);
			//return null;
		}
		
		Channel channel=new Channel(flowId, src, dst, nodes, links, reqBw);

		return channel;
	}
	
	private void updateChannel() {
		List<String> removeCh = new ArrayList<String>();  
		for(String key:this.channelTable.keySet()) {
			Channel ch = this.channelTable.get(key);
			if(ch.getActiveTransmissionNum() == 0) {
				// No more job in channel. Delete
				removeCh.add(key);
			}
		}
		
		for(String key:removeCh) {
			removeChannel(key);
		}
	}
	
	private String getKey(int origin, int destination) {
		return origin+"-"+destination;
	}
	
	private String getKey(int origin, int destination, int appId) {
		return getKey(origin,destination)+"-"+appId;
	}


	public void setDatacenter(SDNDatacenter dc) {
		this.datacenter = dc;
	}

	public List<Host> getHostList() {
		return this.hosts;		
	}

	public List<Switch> getSwitchList() {
		return this.switches;
	}

	public boolean isApplicationDeployed() {
		return isApplicationDeployed;
	}

	protected Vm findVm(int vmId) {
		for(Vm vm:vmList) {
			if(vm.getId() == vmId)
				return vm;
		}
		return null;
	}
	protected SDNHost findSDNHost(Host host) {
		for(SDNHost sdnhost:sdnhosts) {
			if(sdnhost.getHost().equals(host)) {
				return sdnhost;
			}
		}
		return null;
	}
	protected SDNHost findSDNHost(int vmId) {
		Vm vm = findVm(vmId);
		if(vm == null)
			return null;
		
		for(SDNHost sdnhost:sdnhosts) {
			if(sdnhost.getHost().equals(vm.getHost())) {
				return sdnhost;
			}
		}
		//System.err.println("NOS.findSDNHost: Host is not found for VM:"+ vmId);
		return null;
	}
	
	public int getHostAddressByVmId(int vmId) {
		Vm vm = findVm(vmId);
		if(vm == null) {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Cannot find VM with vmId = "+ vmId);
			return -1;
		}
		
		Host host = vm.getHost();
		SDNHost sdnhost = findSDNHost(host);
		if(sdnhost == null) {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Cannot find SDN Host with vmId = "+ vmId);
			return -1;
		}
		
		return sdnhost.getAddress();
	}
	
	protected Host createHost(int hostId, int ram, long bw, long storage, long pes, double mips) {
		LinkedList<Pe> peList = new LinkedList<Pe>();
		int peId=0;
		for(int i=0;i<pes;i++) peList.add(new Pe(peId++,new PeProvisionerSimple(mips)));
		
		RamProvisioner ramPro = new RamProvisionerSimple(ram);
		BwProvisioner bwPro = new BwProvisionerSimple(bw);
		VmScheduler vmScheduler = new VmSchedulerTimeSharedEnergy(peList);		
		Host newHost = new Host(hostId, ramPro, bwPro, storage, peList, vmScheduler);
		
		return newHost;		
	}
	
	protected void initPhysicalTopology() {
		this.topology = new PhysicalTopology();
		this.hosts = new ArrayList<Host>();
		this.sdnhosts = new ArrayList<SDNHost>();
		
		int hostId=0;
		Hashtable<String,Integer> nameIdTable = new Hashtable<String, Integer>();
		try {
    		JSONObject doc = (JSONObject) JSONValue.parse(new FileReader(this.physicalTopologyFileName));
    		
    		JSONArray nodes = (JSONArray) doc.get("nodes");
    		@SuppressWarnings("unchecked")
			Iterator<JSONObject> iter =nodes.iterator(); 
			while(iter.hasNext()){
				JSONObject node = iter.next();
				String nodeType = (String) node.get("type");
				String nodeName = (String) node.get("name");
				
				if(nodeType.equalsIgnoreCase("host")){
					long pes = (Long) node.get("pes");
					long mips = (Long) node.get("mips");
					int ram = new BigDecimal((Long)node.get("ram")).intValueExact();
					long storage = (Long) node.get("storage");
					long bw = new BigDecimal((Long)node.get("bw")).intValueExact();
					
					int num = 1;
					if (node.get("nums")!= null)
						num = new BigDecimal((Long)node.get("nums")).intValueExact();

					for(int n = 0; n< num; n++) {
						String nodeName2 = nodeName;
						if(num >1) nodeName2 = nodeName + n;
						
						Host host = createHost(hostId, ram, bw, storage, pes, mips);
						SDNHost sdnHost = new SDNHost(host, this);
						nameIdTable.put(nodeName2, sdnHost.getAddress());
						hostId++;
						
						topology.addNode(sdnHost);
						this.hosts.add(host);
						this.sdnhosts.add(sdnHost);
					}
					
				} else {
					int MAX_PORTS = 256;
							
					int bw = new BigDecimal((Long)node.get("bw")).intValueExact();
					long iops = (Long) node.get("iops");
					int upports = MAX_PORTS;
					int downports = MAX_PORTS;
					if (node.get("upports")!= null)
						upports = new BigDecimal((Long)node.get("upports")).intValueExact();
					if (node.get("downports")!= null)
						downports = new BigDecimal((Long)node.get("downports")).intValueExact();
					Switch sw = null;
					
					if(nodeType.equalsIgnoreCase("core")) {
						sw = new CoreSwitch(nodeName, bw, iops, upports, downports, this);
					} else if (nodeType.equalsIgnoreCase("aggregate")){
						sw = new AggregationSwitch(nodeName, bw, iops, upports, downports, this);
					} else if (nodeType.equalsIgnoreCase("edge")){
						sw = new EdgeSwitch(nodeName, bw, iops, upports, downports, this);
					} else {
						throw new IllegalArgumentException("No switch found!");
					}
					
					if(sw != null) {
						nameIdTable.put(nodeName, sw.getAddress());
						topology.addNode(sw);
						this.switches.add(sw);
					}
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
				
				int srcAddress = nameIdTable.get(src);
				int dstAddress = nameIdTable.get(dst);
				topology.addLink(srcAddress, dstAddress, lat);
			}
    		
    		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		topology.buildDefaultRouting();
	}
	
	private static int flowNumbers=0;
	public boolean deployApplication(int userId, String vmsFileName){

		vmNameIdTable = new HashMap<String, Integer>();
		vmList = new LinkedList<Vm>();
		LinkedList<Middlebox> mbList = new LinkedList<Middlebox>();
		arcList = new LinkedList<Arc>();
		flowIdArcTable = new HashMap<Integer, Arc>();
		flowNameIdTable = new HashMap<String, Integer>();
		flowNameIdTable.put("default", -1);
		
		try {
    		JSONObject doc = (JSONObject) JSONValue.parse(new FileReader(vmsFileName));
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
				long bw = 1000;
				if(node.get("bw") != null)
					bw = (Long) node.get("bw");
				
				double starttime = 0;
				double endtime = Double.POSITIVE_INFINITY;
				if(node.get("starttime") != null)
					starttime = (Double) node.get("starttime");
				if(node.get("endtime") != null)
					endtime = (Double) node.get("endtime");

				long nums =1;
				if(node.get("nums") != null)
					nums = (Long) node.get("nums");
				
				for(int n=0; n<nums; n++) {
					String nodeName2 = nodeName;
					if(nums > 1) {
						// Nodename should be numbered.
						nodeName2 = nodeName + n;
					}
					if(nodeType.equalsIgnoreCase("vm")){
						// VM
						Vm vm = new TimedVm(vmId,userId,mips,pes,ram,bw,size,"VMM",new CloudletSchedulerTimeShared(), starttime, endtime);
						vmNameIdTable.put(nodeName2, vmId);
						NetworkOperatingSystem.debugVmIdName.put(vmId, nodeName2);
						
						vmList.add(vm);
						vmId++;
					} else {
						// Middle box
						Vm vm = new Vm(vmId,userId,mips,pes,ram,bw,size,"VMM",new CloudletSchedulerTimeShared());
						Middlebox m = deployMiddlebox(nodeType,vm);
						vmNameIdTable.put(nodeName2, vmId);
						mbList.add(m);
						vmId++;
					}
				}
			}
			
			JSONArray links = (JSONArray) doc.get("links");
			
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> linksIter = links.iterator(); 
			while(linksIter.hasNext()){
				JSONObject link = linksIter.next();
				String name = (String) link.get("name");
				String src = (String) link.get("source");  
				String dst = (String) link.get("destination");
				
				Object reqLat = link.get("latency");
				Object reqBw = link.get("bandwidth");
				
				double lat = 0.0;
				long bw = 0;
				
				if(reqLat != null)
					lat = (Double) reqLat;
				if(reqBw != null)
					bw = (Long) reqBw;
				
				int srcId = vmNameIdTable.get(src);
				int dstId = vmNameIdTable.get(dst);
				
				int flowId = -1;
				
				if(name == null || "default".equalsIgnoreCase(name)) {
					// default flow.
					flowId = -1;
				}
				else {
					flowId = flowNumbers++;
					flowNameIdTable.put(name, flowId);
				}
				
				Arc arc = new Arc(srcId, dstId, flowId, bw, lat);
				arcList.add(arc);
				if(flowId != -1) {
					flowIdArcTable.put(flowId, arc);

				}
			}
    	
			boolean result = deployApplication(vmList, mbList, arcList);
			if (result){
				isApplicationDeployed = true;
				return true;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	
}
