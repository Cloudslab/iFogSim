/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * VmSchedulerSpaceShared is a VMM allocation policy that allocates one or more Pe to a VM, and
 * doesn't allow sharing of PEs. If there is no free PEs to the VM, allocation fails. Free PEs are
 * not allocated to VMs
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class VmSchedulerSpaceSharedEnergy extends VmScheduler {

	/** Map containing VM ID and a vector of PEs allocated to this VM. */
	private Map<String, List<Pe>> peAllocationMap;

	/** The free pes vector. */
	private List<Pe> freePes;

	/**
	 * Instantiates a new vm scheduler space shared.
	 * 
	 * @param pelist the pelist
	 */
	public VmSchedulerSpaceSharedEnergy(List<? extends Pe> pelist) {
		super(pelist);
		setPeAllocationMap(new HashMap<String, List<Pe>>());
		setFreePes(new ArrayList<Pe>());
		getFreePes().addAll(pelist);
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmScheduler#allocatePesForVm(org.cloudbus.cloudsim.Vm,
	 * java.util.List)
	 */
	@Override
	public boolean allocatePesForVm(Vm vm, List<Double> mipsShare) {
		// if there is no enough free PEs, fails
		if (getFreePes().size() < mipsShare.size()) {
			return false;
		}

		List<Pe> selectedPes = new ArrayList<Pe>();
		Iterator<Pe> peIterator = getFreePes().iterator();
		Pe pe = peIterator.next();
		double totalMips = 0;
		for (Double mips : mipsShare) {
			if (mips <= pe.getMips()) {
				selectedPes.add(pe);
				totalMips += mips;
				if (!peIterator.hasNext()) {
					break;
				}
				pe = peIterator.next();
			}
		}
		if (mipsShare.size() > selectedPes.size()) {
			return false;
		}

		getFreePes().removeAll(selectedPes);

		getPeAllocationMap().put(vm.getUid(), selectedPes);
		getMipsMap().put(vm.getUid(), mipsShare);
		setAvailableMips(getAvailableMips() - totalMips);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmScheduler#deallocatePesForVm(org.cloudbus.cloudsim.Vm)
	 */
	@Override
	public void deallocatePesForVm(Vm vm) {
		getFreePes().addAll(getPeAllocationMap().get(vm.getUid()));
		getPeAllocationMap().remove(vm.getUid());

		double totalMips = 0;
		for (double mips : getMipsMap().get(vm.getUid())) {
			totalMips += mips;
		}
		setAvailableMips(getAvailableMips() + totalMips);

		getMipsMap().remove(vm.getUid());
	}

	/**
	 * Sets the pe allocation map.
	 * 
	 * @param peAllocationMap the pe allocation map
	 */
	protected void setPeAllocationMap(Map<String, List<Pe>> peAllocationMap) {
		this.peAllocationMap = peAllocationMap;
	}

	/**
	 * Gets the pe allocation map.
	 * 
	 * @return the pe allocation map
	 */
	protected Map<String, List<Pe>> getPeAllocationMap() {
		return peAllocationMap;
	}

	/**
	 * Sets the free pes vector.
	 * 
	 * @param freePes the new free pes vector
	 */
	protected void setFreePes(List<Pe> freePes) {
		this.freePes = freePes;
	}

	/**
	 * Gets the free pes vector.
	 * 
	 * @return the free pes vector
	 */
	protected List<Pe> getFreePes() {
		return freePes;
	}


	/************************************************
	 *  Calculate Utilization history
	 ************************************************/
	public class HistoryEntry {
		public double startTime;
		public double usedMips;
		HistoryEntry(double t, double m) { startTime=t; usedMips=m;}
	}
	private List<HistoryEntry> utilizationHistories = null;
	public List<HistoryEntry> getUtilizationHisotry() {
		return utilizationHistories;
	}
	public double getUtilizationTotalMips() {
		double total=0;
		double lastTime=0;
		double lastMips=0;
		for(HistoryEntry h:this.utilizationHistories) {
			total += lastMips * (h.startTime - lastTime);
			lastTime = h.startTime;
			lastMips = h.usedMips;
		}
		return total;
	}
	private static double powerOffDuration = 1*3600; //if host is idle for 1 hours, it's turned off.
	
	public double getUtilizationEnergyConsumption() {
		
		double total=0;
		double lastTime=0;
		double lastMips=0;
		for(HistoryEntry h:this.utilizationHistories) {
			double duration = h.startTime - lastTime;
			double utilPercentage = lastMips/ getTotalMips();
			double power = calculatePower(utilPercentage);
			double energyConsumption = power * duration;
			
			// Assume that the host is turned off when duration is long enough
			if(duration > powerOffDuration && lastMips == 0)
				energyConsumption = 0;
			
			total += energyConsumption;
			lastTime = h.startTime;
			lastMips = h.usedMips;
		}
		return total/3600;	// transform to Whatt*hour from What*seconds
	}
	private void addUtilizationEntry() {
		double time = CloudSim.clock();
		double totalMips = getTotalMips();
		double usingMips = totalMips - this.getAvailableMips();
		if(usingMips < 0) {
			System.err.println("No way!");
		}
		if(utilizationHistories == null)
			utilizationHistories = new ArrayList<HistoryEntry>();
		this.utilizationHistories.add(new HistoryEntry(time, usingMips));
	}
	private double calculatePower(double u) {
		double power = 120 + 154 * u;
		return power;
	}
	private double getTotalMips() {
		return this.getPeList().size() * this.getPeCapacity();
	}

	@Override
	protected void setAvailableMips(double availableMips) {
		super.setAvailableMips(availableMips);
		addUtilizationEntry();		
	}
}
