package org.fog.application.selectivity;

/**
 * 以固定的概率为传入的输入元组生成一个输出元组
 */

/**
 * Generates an output tuple for an incoming input tuple with a fixed probability
 * @author Harshit Gupta
 *
 */
public class FractionalSelectivity implements SelectivityModel{

	/**
	 * The fixed probability of output tuple creation per incoming input tuple
	 */
	double selectivity;
	
	public FractionalSelectivity(double selectivity){
		setSelectivity(selectivity);
	}
	public double getSelectivity() {
		return selectivity;
	}
	public void setSelectivity(double selectivity) {
		this.selectivity = selectivity;
	}
	
	@Override
	public boolean canSelect() {
		if(Math.random() < getSelectivity()) // if the probability condition is satisfied
			return true;
		return false;
	}
	
	@Override
	public double getMeanRate() {
		return getSelectivity(); // the average rate of tuple generation is the fixed probability value
	}
	
	@Override
	public double getMaxRate() {
		return getSelectivity(); // the maximum rate of tuple generation is the fixed probability value
	}
	
}
