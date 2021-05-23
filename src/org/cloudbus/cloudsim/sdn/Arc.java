/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

/**
 * Traffic requirements between two VMs
 * 
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class Arc {

	int srcId;
	int dstId;
	int flowId;
	long requiredBandwidth;
	double requiredLatency;
	
	public Arc(int srcId, int dstId, int flowId, long reqBW, double reqLatency) {
		super();
		this.srcId = srcId;
		this.dstId = dstId;
		this.flowId = flowId;
		this.requiredBandwidth = reqBW;
		this.requiredLatency = reqLatency;
	}

	public int getSrcId() {
		return srcId;
	}

	public int getDstId() {
		return dstId;
	}
	public int getFlowId() {
		return flowId;
	}

	public long getBw() {
		return requiredBandwidth;
	}

	public double getLatency() {
		return requiredLatency;
	}
}
