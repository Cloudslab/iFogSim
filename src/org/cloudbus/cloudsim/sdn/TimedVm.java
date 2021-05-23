/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.Vm;

/**
 * Extension of VM that supports to set start and terminate time of VM in VM creation request.
 * If start time and finish time is set up, specific CloudSim Event is triggered
 * in datacenter to create and terminate the VM. 
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class TimedVm extends Vm {

	private double startTime;
	private double finishTime;
	
	public TimedVm(int id, int userId, double mips, int numberOfPes, int ram,
			long bw, long size, String vmm, CloudletScheduler cloudletScheduler) {
		super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
	}
	
	public TimedVm(int id, int userId, double mips, int numberOfPes, int ram,
			long bw, long size, String vmm, CloudletScheduler cloudletScheduler, double startTime, double finishTime) {
		super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
	
		this.startTime = startTime;
		this.finishTime = finishTime;
	}
	
	public double getStartTime() {
		return startTime;
	}
	
	public double getFinishTime() {
		return finishTime;
	}

}
