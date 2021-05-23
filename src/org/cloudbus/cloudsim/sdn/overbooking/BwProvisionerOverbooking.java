/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.overbooking;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;

/**
 * BwProvisionerSimple is a class that implements a simple best effort allocation policy: if there
 * is bw available to request, it allocates; otherwise, it fails.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class BwProvisionerOverbooking extends BwProvisioner {

	/** The bw table. */
	private Map<String, Long> bwTable;
	public static final double overbookingRatioBw = 1.0;	// 20% overbooking allowed for BW

	/**
	 * Instantiates a new bw provisioner simple.
	 * 
	 * @param bw the bw
	 */
	public BwProvisionerOverbooking(long bw) {
		super(bw);
		setAvailableBw((long) getOverbookedBw(bw));	//overwrite available BW to overbookable BW
		
		setBwTable(new HashMap<String, Long>());
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.provisioners.BwProvisioner#allocateBwForVm(cloudsim.Vm, long)
	 */
	@Override
	public boolean allocateBwForVm(Vm vm, long bw) {
		deallocateBwForVm(vm);

		if (getAvailableBw() >= bw) {
			setAvailableBw(getAvailableBw() - bw);
			getBwTable().put(vm.getUid(), bw);
			vm.setCurrentAllocatedBw(getAllocatedBwForVm(vm));
			return true;
		}

		vm.setCurrentAllocatedBw(getAllocatedBwForVm(vm));
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.provisioners.BwProvisioner#getAllocatedBwForVm(cloudsim.Vm)
	 */
	@Override
	public long getAllocatedBwForVm(Vm vm) {
		if (getBwTable().containsKey(vm.getUid())) {
			return getBwTable().get(vm.getUid());
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.provisioners.BwProvisioner#deallocateBwForVm(cloudsim.Vm)
	 */
	@Override
	public void deallocateBwForVm(Vm vm) {
		if (getBwTable().containsKey(vm.getUid())) {
			long amountFreed = getBwTable().remove(vm.getUid());
			setAvailableBw(getAvailableBw() + amountFreed);
			vm.setCurrentAllocatedBw(0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.provisioners.BwProvisioner#deallocateBwForVm(cloudsim.Vm)
	 */
	@Override
	public void deallocateBwForAllVms() {
		super.deallocateBwForAllVms();
		
		setAvailableBw((long) getOverbookedBw(getBw()));	//Overbooking
		getBwTable().clear();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * gridsim.virtualization.power.provisioners.BWProvisioner#isSuitableForVm(gridsim.virtualization
	 * .power.VM, long)
	 */
	@Override
	public boolean isSuitableForVm(Vm vm, long bw) {
		long allocatedBw = getAllocatedBwForVm(vm);
		boolean result = allocateBwForVm(vm, bw);
		deallocateBwForVm(vm);
		if (allocatedBw > 0) {
			allocateBwForVm(vm, allocatedBw);
		}
		return result;
	}

	/**
	 * Gets the bw table.
	 * 
	 * @return the bw table
	 */
	protected Map<String, Long> getBwTable() {
		return bwTable;
	}

	/**
	 * Sets the bw table.
	 * 
	 * @param bwTable the bw table
	 */
	protected void setBwTable(Map<String, Long> bwTable) {
		this.bwTable = bwTable;
	}

	public static double getOverbookedBw(long capacity) {
		double overbookedBw = capacity * BwProvisionerOverbooking.overbookingRatioBw;
		return overbookedBw;		
	}

}
