package org.cloudbus.cloudsim.sdn.overbooking;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.Arc;
import org.cloudbus.cloudsim.sdn.Link;
import org.cloudbus.cloudsim.sdn.Middlebox;
import org.cloudbus.cloudsim.sdn.NetworkOperatingSystem;
import org.cloudbus.cloudsim.sdn.Node;
import org.cloudbus.cloudsim.sdn.SDNHost;
import org.cloudbus.cloudsim.sdn.TimedVm;

public class OverbookingNetworkOperatingSystem extends NetworkOperatingSystem {

	public OverbookingNetworkOperatingSystem(String fileName) {
		super(fileName);
	}

	@Override
	public boolean deployApplication(List<Vm> vms, List<Middlebox> middleboxes, List<Arc> links) {
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Starting deploying application..");
		
		for(Vm vm:vms)
		{
			TimedVm tvm = (TimedVm) vm;
			Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
					+ " in " + datacenter.getName() + ", (" + tvm.getStartTime() + "~" +tvm.getFinishTime() + ")");
			send(datacenter.getId(), tvm.getStartTime(), CloudSimTags.VM_CREATE_ACK, vm);
			
			if(tvm.getFinishTime() != Double.POSITIVE_INFINITY) {
				send(datacenter.getId(), tvm.getFinishTime(), CloudSimTags.VM_DESTROY, vm);
				send(this.getId(), tvm.getFinishTime(), CloudSimTags.VM_DESTROY, vm);
			}
		}
		return true;
	}
	
	public boolean deployFlow(List<Arc> links) {
		for(Arc link:links) {
			int srcVm = link.getSrcId();
			int dstVm = link.getDstId();
			int flowId = link.getFlowId();
			
			SDNHost srchost = findSDNHost(srcVm);
			SDNHost dsthost = findSDNHost(dstVm);
			if(srchost == null || dsthost == null) {
				continue;
			}
			
			if(srchost.equals(dsthost)) {
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Source SDN Host is same as Destination. Go loopback");
				srchost.addVMRoute(srcVm, dstVm, flowId, dsthost);
			}
			else {
				Log.printLine(CloudSim.clock() + ": " + getName() + ": VMs are in different hosts. Create entire routing table (hosts, switches)");
				boolean findRoute = buildRoutingTables(srchost, srcVm, dstVm, flowId, null);
				
				if(!findRoute) {
					System.err.println("SimpleNetworkOperatingSystem.deployFlow: Could not find route!!" + 
							NetworkOperatingSystem.debugVmIdName.get(srcVm) + "->"+NetworkOperatingSystem.debugVmIdName.get(dstVm));
				}
			}
			
		}
		
		// Print all routing tables.
		for(Node node:this.topology.getAllNodes()) {
			node.printVMRoute();
		}
		return true;
	}
	
	private boolean buildRoutingTables(Node node, int srcVm, int dstVm, int flowId, Node prevNode) {
		Collection<Link> links = this.topology.getAdjacentLinks(node);
		if(links.size() == 0) {
			// No link. Do nothing
		}
		else if(links.size() == 1) {
			// Only one way, no other choice (for Host)
			for(Link l:links) {
				Node nextHop= l.getHighOrder();
				if(nextHop.equals(node))
					nextHop= l.getLowOrder();
				
				node.addVMRoute(srcVm, dstVm, flowId, nextHop);
				buildRoutingTables(nextHop, srcVm, dstVm, flowId, node);
			}
			return true;
		}
		else {
			// There are many links. Determine which hop to go.
			SDNHost dsthost = findSDNHost(dstVm);
			
			for(Link l:links) {
				Node nextHop= l.getHighOrder();
				if(nextHop.equals(node))
					nextHop= l.getLowOrder();
				
				if(nextHop.equals(prevNode)) {
					// NextHop is going back to prev node
					continue;	
				}
				else if(nextHop.equals(dsthost)) {
					// NextHop is the destination. Just add. No further route finding.
					node.addVMRoute(srcVm, dstVm, flowId, nextHop);
					return true;
				} 
				else if(nextHop instanceof SDNHost) {
					// NextHop is host but no destination. Can't forward
					continue;
				}
				else {
					// Nexthop is switch
					if(buildRoutingTables(nextHop, srcVm, dstVm, flowId, node)) {
						// If the route is right.
						node.addVMRoute(srcVm, dstVm, flowId, nextHop);
						return true;
					}
					else
						continue;
				}
			}
		}
		return false;
	}

	@Override
	protected Middlebox deployMiddlebox(String type,Vm vm) {
		return null;
	}
	
	@Override
	public void processVmCreateAck(SimEvent ev) {
		// print the created VM info
		TimedVm vm = (TimedVm) ev.getData();
		Log.printLine(CloudSim.clock() + ": " + getName() + ": VM Created: " +  vm.getId() + " in " + this.findSDNHost(vm.getId()));
		deployFlow(this.arcList);
	}

	@Override
	protected Host createHost(int hostId, int ram, long bw, long storage, long pes, double mips) {
		LinkedList<Pe> peList = new LinkedList<Pe>();
		int peId=0;
		for(int i=0;i<pes;i++) peList.add(new Pe(peId++,new PeProvisionerOverbooking(mips)));
		
		RamProvisioner ramPro = new RamProvisionerSimple(ram);
		BwProvisioner bwPro = new BwProvisionerOverbooking(bw);
		VmScheduler vmScheduler = new VmSchedulerTimeSharedOverbookingEnergy(peList);		
		Host newHost = new Host(hostId, ramPro, bwPro, storage, peList, vmScheduler);
		
		return newHost;		
	}
}
