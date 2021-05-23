/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn;

import org.cloudbus.cloudsim.Cloudlet;

/**
 * Processing activity to compute in VM. Basically a wrapper of Cloudlet. 
 *  
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class Processing implements Activity {

	long requestId;
	Cloudlet cl;
	
	public Processing(Cloudlet cl){
		this.cl=cl;
	}
	
	public Cloudlet getCloudlet(){
		return cl;
	}
}