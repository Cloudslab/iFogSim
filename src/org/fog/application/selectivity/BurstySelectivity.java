package org.fog.application.selectivity;

import org.cloudbus.cloudsim.core.CloudSim;

/**
 * Generates an output tuple for every input tuple according to a bursty model.
 * During high burst period, all input tuples result in an output tuple.
 * During low burst period, no input tuples result in an output tuple.
 * @author Harshit Gupta
 *
 */
public class BurstySelectivity implements SelectivityModel{

	/**
	 * Duration of the low burst period
	 */
	double burstLowPeriod;
	
	/**
	 * Duration of the high burst period
	 */
	double burstHighPeriod;
	
	/**
	 * First instance of the start of high burst period, using which subsequent burst periods will be calculated.
	 */
	double firstHighTime;
	
	public BurstySelectivity(double burstLowPeriod, double burstHighPeriod, double firstHighTime){
		setBurstLowPeriod(burstLowPeriod);
		setBurstHighPeriod(burstHighPeriod);
		setFirstHighTime(firstHighTime);
	}
	
	/**
	 * If the current time falls in the high burst period of the specified burst model, an output tuple is generated for the incoming tuple.
	 */
	@Override
	public boolean canSelect() {
		double time = CloudSim.clock() + getFirstHighTime();
		double burstPeriod = getBurstHighPeriod()+getBurstLowPeriod();
		double burstStartTime = burstPeriod*((int)(time/burstPeriod));
		if(time <= burstStartTime + getBurstHighPeriod())
			return true;
		else
			return false;
	}

	/**
	 * The mean tuple generation rate is the fraction of high burst period in the total burst period.
	 */
	@Override
	public double getMeanRate() {
		return getBurstHighPeriod()/(getBurstHighPeriod()+getBurstLowPeriod());
	}

	/**
	 * Maximum tuple generation rate equals 1 (when the burst is high)
	 */
	@Override
	public double getMaxRate() {
		return 1;
	}

	public double getBurstLowPeriod() {
		return burstLowPeriod;
	}

	public void setBurstLowPeriod(double burstLowPeriod) {
		this.burstLowPeriod = burstLowPeriod;
	}

	public double getBurstHighPeriod() {
		return burstHighPeriod;
	}

	public void setBurstHighPeriod(double burstHighPeriod) {
		this.burstHighPeriod = burstHighPeriod;
	}

	public double getFirstHighTime() {
		return firstHighTime;
	}

	public void setFirstHighTime(double firstHighTime) {
		this.firstHighTime = firstHighTime;
	}

}
