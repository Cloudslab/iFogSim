/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.example.topogenerators;

import java.util.Random;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.random.Well19937c;

/**
 * Generate VM requests, for example:
{
  "nodes" : [
    {
      "name" : "vm01",
      "type" : "vm",
      "size" : 1000,
      "pes": 1,
      "mips" : 30000000,
      "ram"  : 512,
      "bw"  : 100000,
      "starttime": 1.3,
      "endtime"  : 20.5,
    },
   ],
  "links" : [
    { 
    	"name": "l32", 
    	"source" : "vm03" , 
    	"destination" : "vm02" , 
    	"bandwidth" : 66000000
    },
   ],
}
 * 
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */

public class VMRequestRandomGenerator {
	
	public static void main(String [] argv) {
		int numVms = 5;
		String jsonFileName = "very_simple_virtual.json";

		VirtualTopologyGeneratorVmTypes vmGenerator = new VirtualTopologyGeneratorVmTypes();
		VMRequestRandomGenerator reqg = new VMRequestRandomGenerator(vmGenerator, numVms, jsonFileName);
		reqg.start();
	}
	
	private static long seed = 10;
	int numVms = 0;
	String jsonFileName = null;
	VirtualTopologyGeneratorVmTypes vmGenerator = null;
	
	public VMRequestRandomGenerator(VirtualTopologyGeneratorVmTypes vmGenerator, int numVms, String jsonFileName) {
		this.vmGenerator = vmGenerator;
		this.numVms = numVms;
		this.jsonFileName = jsonFileName;
		
	}
	public void start() {
		generateVMsRandom(numVms);
		vmGenerator.wrtieJSON(jsonFileName);
	}
	
	public void generateVMsRandom(int totalVmNum) {
		int vmCount = 0;
		double lastStartTime = 0;
		
		double startMean = 1800; // sec = 30min
		double durScale=14400; // sec = 4 hours
		double durShape=1.2;
		
		Random rVmNum = new Random(seed);
		ExponentialDistribution rStartTime = new ExponentialDistribution(new Well19937c(seed), startMean, ExponentialDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);	
		ParetoDistribution rDuration = new ParetoDistribution(new Well19937c(seed), durScale, durShape);
		
		while(vmCount < totalVmNum) {
			int vmsInGroup = rVmNum.nextInt(4)+2;
			double duration = Math.floor(rDuration.sample());
			
			vmGenerator.generateVMGroup(vmsInGroup, lastStartTime, lastStartTime+duration, null);
			lastStartTime += Math.floor(rStartTime.sample());
			
			vmCount += vmsInGroup;
			
		}
	}
}
