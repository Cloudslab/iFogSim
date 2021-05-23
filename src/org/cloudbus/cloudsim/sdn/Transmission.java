/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn;

/**
 * This class represents transmission of a package. It controls
 * amount of data transmitted in a shared data medium. Relation between
 * Transmission and Channel is the same as Cloudlet and CloudletScheduler,
 * but here we consider only the time shared case, representing a shared
 * channel among different simultaneous package transmissions.
 * Note that estimated transmission time is calculated in NOS.
 *
 * @author Jungmin Son
 * @author Rodrigo N. Calheiros
 * @since CloudSimSDN 1.0
 */
public class Transmission implements Activity {
	Package pkg;
	long amountToBeProcessed;
	
	public Transmission(int origin, int destination, long size, int flowId, Request payload) {
		this.pkg = new Package(origin, destination, size, flowId, payload);
		this.amountToBeProcessed=pkg.getSize();
	}
	
	public Transmission(Package pkg){
		this.pkg = pkg;
		this.amountToBeProcessed=pkg.getSize();
	}
	
	public long getSize(){
		return amountToBeProcessed;
	}
	
	public Package getPackage(){
		return pkg;
	}
	
	/**
	 * Sums some amount of data to the already transmitted data
	 * @param completed amount of data completed since last update
	 */
	public void addCompletedLength(long completed){
		amountToBeProcessed-=completed;
		if (amountToBeProcessed<=0) amountToBeProcessed = 0;
	}
	
	/**
	 * Say if the Package transmission finished or not.
	 * @return true if transmission finished; false otherwise
	 */
	public boolean isCompleted(){
		return amountToBeProcessed==0;
	}
	
	public String toString() {
		return "Transmission:"+this.pkg.toString();
	}
}
