/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

/**
 * Represent edge switch
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class EdgeSwitch extends Switch {

	public EdgeSwitch(String name,int bw, long iops, int upports, int downports, NetworkOperatingSystem nos) {
		super(name, bw, iops, upports, downports, nos);
	}
	
}
