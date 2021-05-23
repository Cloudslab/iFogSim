/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

/**
 * Represent aggregation switch
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class AggregationSwitch extends Switch {

	public AggregationSwitch(String name,int bw, long iops, int upports, int downports, NetworkOperatingSystem nos) {
		super(name, bw, iops, upports, downports, nos);
	}

}
