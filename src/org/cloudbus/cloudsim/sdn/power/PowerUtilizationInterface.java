/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.power;

import java.util.List;


/**
 * Interface to manage utilization history.
 * 
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */
public interface PowerUtilizationInterface {
	public void addUtilizationEntryTermination(double terminatedTime);
	public List<PowerUtilizationHistoryEntry> getUtilizationHisotry();
	public double getUtilizationEnergyConsumption();
}

