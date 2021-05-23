/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.example.policies;

import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationHistoryEntry;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationInterface;

/**
 * VmSchedulerTimeSharedEnergy is a VMM allocation policy that allocates one or more Pe to a VM, and
 * allows sharing of PEs by time. If there is no free PEs to the VM, allocation fails. Free PEs are
 * not allocated to VMs
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Jungmin Son
 *  * @since CloudSim Toolkit 1.0
 */
public class VmSchedulerTimeSharedEnergy extends VmSchedulerTimeShared implements PowerUtilizationInterface{

	
	public VmSchedulerTimeSharedEnergy(List<? extends Pe> pelist) {
		super(pelist);
	}

	@Override
	protected void setAvailableMips(double availableMips) {
		super.setAvailableMips(availableMips);
		addUtilizationEntry();		
	}
	
	private List<PowerUtilizationHistoryEntry> utilizationHistories = null;
	private static double powerOffDuration = 0; //if host is idle for 1 hours, it's turned off.
	
	public void addUtilizationEntryTermination(double terminatedTime) {
		if(this.utilizationHistories != null)
			this.utilizationHistories.add(new PowerUtilizationHistoryEntry(terminatedTime, 0));
	}
	
	public List<PowerUtilizationHistoryEntry> getUtilizationHisotry() {
		return utilizationHistories;
	}

	public double getUtilizationEnergyConsumption() {
		
		double total=0;
		double lastTime=0;
		double lastMips=0;
		if(this.utilizationHistories == null)
			return 0;
		
		for(PowerUtilizationHistoryEntry h:this.utilizationHistories) {
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
	
	private double calculatePower(double u) {
		double power = 120 + 154 * u;
		return power;
	}

	private void addUtilizationEntry() {
		double time = CloudSim.clock();
		double totalMips = getTotalMips();
		double usingMips = totalMips - this.getAvailableMips();
		if(usingMips < 0) {
			System.err.println("addUtilizationEntry : using mips is negative, No way!");
		}
		if(utilizationHistories == null)
			utilizationHistories = new ArrayList<PowerUtilizationHistoryEntry>();
		this.utilizationHistories.add(new PowerUtilizationHistoryEntry(time, usingMips));
	}
	
	private double getTotalMips() {
		return this.getPeList().size() * this.getPeCapacity();
	}
}
