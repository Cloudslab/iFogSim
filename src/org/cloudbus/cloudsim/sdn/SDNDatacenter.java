/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 * Extended class of Datacenter that supports processing SDN-specific events.
 * In addtion to the default Datacenter, it processes Request submission to VM,
 * and application deployment request. 
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class SDNDatacenter extends Datacenter {

	NetworkOperatingSystem nos;
	
	public SDNDatacenter(String name, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval, NetworkOperatingSystem nos) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		
		this.nos=nos;
		//nos.init();
	}
	
	public void addVm(Vm vm){
		getVmList().add(vm);
		if (vm.isBeingInstantiated()) vm.setBeingInstantiated(false);
		vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));
	}
		
	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		super.processVmCreate(ev, ack);
		if(ack) {
			send(nos.getId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, ev.getData());
		}
			
	}
	
	@Override
	public void processOtherEvent(SimEvent ev){
		switch(ev.getTag()){
			case Constants.REQUEST_SUBMIT: processRequest((Request) ev.getData()); break;
			case Constants.APPLICATION_SUBMIT: processApplication(ev.getSource(),(String) ev.getData()); break;
			default: System.out.println("Unknown event recevied by SdnDatacenter. Tag:"+ev.getTag());
		}
	}

	@Override
	protected void checkCloudletCompletion() {
		if(!nos.isApplicationDeployed())
		{
			super.checkCloudletCompletion();
			return;
		}
		
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			for (Vm vm : host.getVmList()) {
				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					if (cl != null) {
						int hostAddress = nos.getHostAddressByVmId(cl.getVmId());
						sendNow(hostAddress, CloudSimTags.CLOUDLET_RETURN, cl);
					}
				}
			}
		}
	}
	
	private void processRequest(Request req) {//Request received from user. Send to SdnHost
		Activity ac = req.getNextActivity();
		if(ac instanceof Processing) {
			Cloudlet cl = ((Processing) ac).getCloudlet();
			int hostAddress = nos.getHostAddressByVmId(cl.getVmId());
			
			//for this first package, size doesn't matter
			Package pkg = new Package(super.getId(), cl.getVmId(), -1, -1, req);
			sendNow(hostAddress, Constants.SDN_PACKAGE, pkg);
		}
		else {
			System.err.println("Request should start with Processing!!");
		}
	}
	
	private void processApplication(int userId, String filename) {
		nos.deployApplication(userId,filename);
		send(userId, CloudSim.getMinTimeBetweenEvents(), Constants.APPLICATION_SUBMIT_ACK, filename);
	}
	
	public Map<String, Integer> getVmNameIdTable() {
		return this.nos.getVmNameIdTable();
	}
	public Map<String, Integer> getFlowNameIdTable() {
		return this.nos.getFlowNameIdTable();
	}
}
