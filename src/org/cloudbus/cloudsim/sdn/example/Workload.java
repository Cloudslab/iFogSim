/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn.example;

import org.cloudbus.cloudsim.sdn.Request;

/**
 * Class to keep workload information parsed from files.
 * This class is used in WorkloadParser
 * 
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */
public class Workload {
	public int appId;
	public double time;
	public int submitVmId;
	public int submitPktSize;
	public Request request;	
}
