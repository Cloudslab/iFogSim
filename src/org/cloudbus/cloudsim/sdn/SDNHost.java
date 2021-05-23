/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import java.util.Hashtable;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;


/**
 * Extended class of Host to support SDN.
 * Added function includes data transmission after completion of Cloudlet compute processing.
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class SDNHost extends SimEntity implements Node {
	private static final double PROCESSING_DELAY= 0.1;
		
	Host host;
	EdgeSwitch sw;
	//Hashtable<Integer,Vm> vms;
	Hashtable<Integer,Middlebox> middleboxes;
	Hashtable<Cloudlet,Request> requestsTable;
	ForwardingRule forwardingTable;
	RoutingTable routingTable;
	int rank = -1;
	NetworkOperatingSystem nos;

	SDNHost(Host host, NetworkOperatingSystem nos){
		super("Host"+host.getId());
		this.host=host;
		this.nos = nos;
			
		//this.vms = new Hashtable<Integer,Vm>();
		this.middleboxes = new Hashtable<Integer, Middlebox>();
		this.requestsTable = new Hashtable<Cloudlet, Request>();
		this.forwardingTable = new ForwardingRule();
		this.routingTable = new RoutingTable();
	}
	
	public Host getHost(){
		return host;
	}
	
	public void setEdgeSwitch(EdgeSwitch sw){
		this.sw=sw;
	}
/*	
	public void addVm(Vm vm){
		vms.put(vm.getId(), vm);
		host.vmCreate(vm);
	}
*/	
	public void addMiddlebox(Middlebox m){
		middleboxes.put(m.getId(), m);
		host.vmCreate(m.getVm());
	}

	@Override
	public void startEntity(){}
	
	@Override
	public void shutdownEntity(){}

	@Override
	public void processEvent(SimEvent ev) {
		int tag = ev.getTag();
		
		switch(tag){
			case Constants.SDN_PACKAGE: processPackage((Package) ev.getData()); break;
			case CloudSimTags.CLOUDLET_RETURN: processCloudletReturn((Cloudlet) ev.getData()); break;
			default: System.out.println("Unknown event received by "+super.getName()+". Tag:"+ev.getTag());
		}
	}
	
	private Vm findVm(int vmId) {
		List<Vm> vms = host.getVmList();
		for(Vm vm:vms) {
			if(vm.getId() == vmId) {
				return vm;
			}
		}
		return null;
	}
	
	private void processPackage(Package data) {
		int vmId = data.getDestination();
		Vm dstVm = findVm(vmId);
		
		if (dstVm != null){//Try to deliver package to a hosted VM
			//Log.printLine(CloudSim.clock() + ": " + getName() + ".processPackage(): Deliver the request to dest VM: "+ dstVm);
			
			data.setFinishTime(CloudSim.clock());
			
			Request req = data.getPayload();
			Activity ac = req.removeNextActivity();
			processActivity(ac, req, vmId);
		} else if (middleboxes.containsKey(vmId)){//Try to deliver package to a hosted middlebox
			Request req = data.getPayload();
			Middlebox m = middleboxes.get(vmId);
			m.submitRequest(req);
		} else {//Something wrong - package doesn't come from/goes to a VM from this Host
			System.out.println("Warning package sent to wrong host. Host ID="+host.getId()+" DST VM ID="+vmId+", SRC VM ID="+data.getDestination());
		}
	}
	
	private void processCloudletReturn(Cloudlet data) {
		Request req = requestsTable.remove(data);
		if (req.isFinished()){//return to user
			send(req.getUserId(),PROCESSING_DELAY,Constants.REQUEST_COMPLETED,req);
		} else {//consume next activity from request. It should be a transmission
			Activity ac = req.removeNextActivity();
			processActivity(ac, req, data.getVmId());
		}
	}
	
	private void processActivity(Activity ac, Request req, int vmId) {
		if(ac instanceof Transmission) {
			Transmission tr = (Transmission)ac;

			Package pkg = tr.getPackage();
			//send package to router via channel (NOS)
			nos.addPackageToChannel(this, pkg);
			
			pkg.setStartTime(CloudSim.clock());
		}
		else if(ac instanceof Processing) {
				Cloudlet cl = ((Processing) ac).getCloudlet();
				cl.setVmId(vmId);
				
				requestsTable.put(cl, req);
				sendNow(host.getDatacenter().getId(),CloudSimTags.CLOUDLET_SUBMIT,cl);
		} else {
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Activity is unknown..");
		}
		
		
	}

	/******* Routeable interface implementation methods ******/

	@Override
	public int getAddress() {
		return super.getId();
	}
	
	@Override
	public long getBandwidth() {
		return host.getBw();
	}

	@Override
	public void clearVMRoutingTable(){
		this.forwardingTable.clear();
	}

	@Override
	public void addVMRoute(int src, int dest, int flowId, Node to){
		forwardingTable.addRule(src, dest, flowId, to);
	}
	
	@Override
	public Node getVMRoute(int src, int dest, int flowId){
		Node route= this.forwardingTable.getRoute(src, dest, flowId);
		if(route == null) {
			this.printVMRoute();
			System.err.println("SDNHost: ERROR: Cannot find route:" + src + "->"+dest + ", flow ="+flowId);
		}
			
		return route;
	}
	
	@Override
	public void removeVMRoute(int src, int dest, int flowId){
		forwardingTable.removeRule(src, dest, flowId);
	}

	@Override
	public void setRank(int rank) {
		this.rank=rank;
	}

	@Override
	public int getRank() {
		return rank;
	}
	
	@Override
	public void printVMRoute() {
		forwardingTable.printForwardingTable(getName());
	}
	
	public String toString() {
		return "SDNHost: "+this.getName();
	}

	@Override
	public void addLink(Link l) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateNetworkUtilization() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addRoute(Node destHost, Link to) {
		this.routingTable.addRoute(destHost, to);
		
	}

	@Override
	public List<Link> getRoute(Node destHost) {
		return this.routingTable.getRoute(destHost);
	}
	
	@Override
	public RoutingTable getRoutingTable() {
		return this.routingTable;
	}
}
