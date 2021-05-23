/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSimTags;

/**
 * Middlebox represent specific VM that acts as a middle box
 *  
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public abstract class Middlebox {
	
	Vm vm;
	int mipsPerOp;
	SDNHost host;
	static int id=0;
	
	public Middlebox(Vm vm, int misPerOperation){
		this.vm=vm;
		this.mipsPerOp=misPerOperation;
	}
	
	public abstract void editRequest(Request req);
	
	public int getId(){
		return vm.getId();
	}
	
	public Vm getVm(){
		return vm;
	}
	
	public void setHost(SDNHost host){
		this.host=host;
	}
	
	public void submitRequest(Request req){
		Cloudlet cl = new Cloudlet(id++,mipsPerOp,1,0,0,new UtilizationModelFull(),new UtilizationModelFull(),new UtilizationModelFull());
		cl.setVmId(vm.getId());
		
		host.schedule(host.getHost().getDatacenter().getId(), 0.0, CloudSimTags.CLOUDLET_SUBMIT, cl);
	}

}
