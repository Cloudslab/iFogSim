/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.example.policies;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

/**
 * VM Allocation Policy - BW and Compute combined, LFF.
 * When select a host to create a new VM, this policy chooses 
 * the least full host in terms of both compute power and network bandwidth.   
 *  
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */
public class VmAllocationPolicyCombinedLeastFullFirst extends VmAllocationPolicyCombinedMostFullFirst{

	public VmAllocationPolicyCombinedLeastFullFirst(List<? extends Host> list) {
		super(list);
	}

	/**
	 * Allocates a host for a given VM.
	 * 
	 * @param vm VM specification
	 * @return $true if the host could be allocated; $false otherwise
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean allocateHostForVm(Vm vm) {
		if (getVmTable().containsKey(vm.getUid())) { // if this vm was not created
			return false;
		}
		
		int numHosts = getHostList().size();

		// 1. Find/Order the best host for this VM by comparing a metric
		int requiredPes = vm.getNumberOfPes();
		double requiredMips = vm.getCurrentRequestedTotalMips();
		long requiredBw = vm.getCurrentRequestedBw();

		boolean result = false;
		
		double[] freeResources = new double[numHosts];
		for (int i = 0; i < numHosts; i++) {
			double mipsFreePercent = (double)getFreeMips().get(i) / hostTotalMips; 
			double bwFreePercent = (double)getFreeBw().get(i) / hostTotalBw;
			
			freeResources[i] = convertWeightedMetric(mipsFreePercent, bwFreePercent);
		}

		for(int tries = 0; tries < numHosts; tries++) {// we still trying until we find a host or until we try all of them
			double moreFree = Double.NEGATIVE_INFINITY;
			int idx = -1;

			// we want the host with less pes in use
			for (int i = 0; i < numHosts; i++) {
				if (freeResources[i] > moreFree) {
					moreFree = freeResources[i];
					idx = i;
				}
			}
			
			if(idx==-1) {
				System.err.println("Cannot assign the VM to any host:"+tries+"/"+numHosts);
				return false;
			}
			
			freeResources[idx] = Double.NEGATIVE_INFINITY;
			
			Host host = getHostList().get(idx);

			// Check whether the host can hold this VM or not.
			if( getFreeMips().get(idx) < requiredMips) {
				//System.err.println("not enough MIPS");
				//Cannot host the VM
				continue;
			}
			if( getFreeBw().get(idx) < requiredBw) {
				//System.err.println("not enough BW");
				//Cannot host the VM
				continue;
			}
			
			result = host.vmCreate(vm);

			if (result) { // if vm were succesfully created in the host
				getVmTable().put(vm.getUid(), host);
				getUsedPes().put(vm.getUid(), requiredPes);
				getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
				
				getUsedMips().put(vm.getUid(), (long) requiredMips);
				getFreeMips().set(idx,  (long) (getFreeMips().get(idx) - requiredMips));

				getUsedBw().put(vm.getUid(), (long) requiredBw);
				getFreeBw().set(idx,  (long) (getFreeBw().get(idx) - requiredBw));
				break;
			} 
		}
		if(!result) {
			System.err.println("Cannot assign the VM to any host:"+"/"+numHosts);
		}
		logMaxNumHostsUsed();
		return result;
	}

}

