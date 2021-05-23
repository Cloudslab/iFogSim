/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn.power;


/**
 * Interface to manage host history.
 * 
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */
public interface PowerUtilizationMaxHostInterface {
	void logMaxNumHostsUsed();
	int getMaxNumHostsUsed();
}
