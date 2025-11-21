/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

/**
 * Constant variables to use
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.core.CloudSimTags;

public enum Constants implements CloudSimTags {
	
	SDN_BASE,
	
	SDN_PACKAGE,
	SDN_INTERNAL_PACKAGE_PROCESS,
	REQUEST_SUBMIT,
	REQUEST_COMPLETED,
	APPLICATION_SUBMIT,	// Broker -> Datacenter.
	APPLICATION_SUBMIT_ACK
}
