package org.fog.scheduler;

import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.ResCloudlet;

public class TupleScheduler extends CloudletSchedulerTimeShared{

	public TupleScheduler(double mips, int numberOfPes) {
		//super(mips, numberOfPes);
		super();
	}

	/**
	 * Get estimated cloudlet completion time.
	 * 
	 * @param rcl the rcl
	 * @param time the time
	 * @return the estimated finish time
	 */
	public double getEstimatedFinishTime(ResCloudlet rcl, double time) {
		//System.out.println("REMAINING CLOUDLET LENGTH : "+rcl.getRemainingCloudletLength()+"\tCLOUDLET LENGTH"+rcl.getCloudletLength());
		//System.out.println("CURRENT ALLOC MIPS FOR CLOUDLET : "+getTotalCurrentAllocatedMipsForCloudlet(rcl, time));
		
		/*>>>>>>>>>>>>>>>>>>>>*/
		/* edit made by HARSHIT GUPTA */
		
		System.out.println("ALLOCATED MIPS FOR CLOUDLET = "+getTotalCurrentAllocatedMipsForCloudlet(rcl, time));
		return time
				+ ((rcl.getRemainingCloudletLength()) / getTotalCurrentAllocatedMipsForCloudlet(rcl, time));
		
		
				
		//return ((rcl.getRemainingCloudletLength()) / getTotalCurrentAllocatedMipsForCloudlet(rcl, time));
		/*end of edit*/
		/*<<<<<<<<<<<<<<<<<<<<<*/
	}
	
//	public void cloudletFinish(ResCloudlet rcl) {
//		rcl.setCloudletStatus(Cloudlet.SUCCESS);
//		rcl.finalizeCloudlet();
//		getCloudletFinishedList().add(rcl);
//	}
	
}
