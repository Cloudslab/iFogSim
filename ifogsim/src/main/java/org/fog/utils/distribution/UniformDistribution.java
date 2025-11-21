package org.fog.utils.distribution;

public class UniformDistribution extends Distribution{

	private double min;
	private double max;
	
	public UniformDistribution(double min, double max){
		super();
		setMin(min);
		setMax(max);
	}
	
	@Override
	public double getNextValue() {
		return getRandom().nextDouble()*(getMax()-getMin())+getMin();
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}
	
	@Override
	public int getDistributionType() {
		return Distribution.UNIFORM;
	}

	@Override
	public double getMeanInterTransmitTime() {
		return (min+max)/2;
	}

}
