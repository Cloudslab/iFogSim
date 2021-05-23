/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.example;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.sdn.Activity;
import org.cloudbus.cloudsim.sdn.Processing;
import org.cloudbus.cloudsim.sdn.Request;
import org.cloudbus.cloudsim.sdn.Switch;
import org.cloudbus.cloudsim.sdn.Transmission;
import org.cloudbus.cloudsim.sdn.Switch.HistoryEntry;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationHistoryEntry;
import org.cloudbus.cloudsim.sdn.power.PowerUtilizationInterface;

/**
 * This class is to print out logs into console.
 * 
 * @author Jungmin Son
 * @since CloudSimSDN 1.0
 */
public class LogPrinter {
	public static void printEnergyConsumption(List<Host> hostList, List<Switch> switchList, double finishTime) {
		double hostEnergyConsumption = 0, switchEnergyConsumption = 0;
		
		Log.printLine("========== HOST POWER CONSUMPTION AND DETAILED UTILIZATION ===========");
		for(Host host:hostList) {
			PowerUtilizationInterface scheduler =  (PowerUtilizationInterface) host.getVmScheduler();
			scheduler.addUtilizationEntryTermination(finishTime);
			
			double energy = scheduler.getUtilizationEnergyConsumption();
			Log.printLine("Host #"+host.getId()+": "+energy);
			hostEnergyConsumption+= energy;

			printHostUtilizationHistory(scheduler.getUtilizationHisotry());

		}

		Log.printLine("========== SWITCH POWER CONSUMPTION AND DETAILED UTILIZATION ===========");
		for(Switch sw:switchList) {
			sw.addUtilizationEntryTermination(finishTime);
			double energy = sw.getUtilizationEnergyConsumption();
			Log.printLine("Switch #"+sw.getId()+": "+energy);
			switchEnergyConsumption+= energy;

			printSwitchUtilizationHistory(sw.getUtilizationHisotry());

		}
		Log.printLine("========== TOTAL POWER CONSUMPTION ===========");
		Log.printLine("Host energy consumed: "+hostEnergyConsumption);
		Log.printLine("Switch energy consumed: "+switchEnergyConsumption);
		Log.printLine("Total energy consumed: "+(hostEnergyConsumption+switchEnergyConsumption));
		
	}

	private static void printHostUtilizationHistory(
			List<PowerUtilizationHistoryEntry> utilizationHisotry) {
		if(utilizationHisotry != null)
			for(PowerUtilizationHistoryEntry h:utilizationHisotry) {
				Log.printLine(h.startTime+", "+h.usedMips);
			}
	}
	private static void printSwitchUtilizationHistory(List<HistoryEntry> utilizationHisotry) {
		if(utilizationHisotry != null)
			for(HistoryEntry h:utilizationHisotry) {
				Log.printLine(h.startTime+", "+h.numActivePorts);
			}
	}
	
	static public String indent = ",";
	static public String tabSize = "10";
	static public String fString = 	"%"+tabSize+"s"+indent;
	static public String fInt = 	"%"+tabSize+"d"+indent;
	static public String fFloat = 	"%"+tabSize+".3f"+indent;
	
	public static void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		Cloudlet cloudlet;

		Log.printLine();
		Log.printLine("========== OUTPUT ==========");
		
		Log.print(String.format(fString, "Cloudlet_ID"));
		Log.print(String.format(fString, "STATUS" ));
		Log.print(String.format(fString, "DataCenter_ID"));
		Log.print(String.format(fString, "VM_ID"));
		Log.print(String.format(fString, "Length"));
		Log.print(String.format(fString, "Time"));
		Log.print(String.format(fString, "Start Time"));
		Log.print(String.format(fString, "Finish Time"));
		Log.print("\n");

		//DecimalFormat dft = new DecimalFormat("######.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			printCloudlet(cloudlet);
		}
	}
	
	private static void printCloudlet(Cloudlet cloudlet) {
		Log.print(String.format(fInt, cloudlet.getCloudletId()));

		if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
			Log.print(String.format(fString, "SUCCESS"));
			Log.print(String.format(fInt, cloudlet.getResourceId()));
			Log.print(String.format(fInt, cloudlet.getVmId()));
			Log.print(String.format(fInt, cloudlet.getCloudletLength()));
			Log.print(String.format(fFloat, cloudlet.getActualCPUTime()));
			Log.print(String.format(fFloat, cloudlet.getExecStartTime()));
			Log.print(String.format(fFloat, cloudlet.getFinishTime()));
			Log.print("\n");
		}
		else {
			Log.printLine("FAILED");
		}
	}
	
	private static double startTime, finishTime;
	public static void printWorkloadList(List<Workload> wls) {
		int[] appIdNum = new int[SDNBroker.appId];
		double[] appIdTime = new double[SDNBroker.appId];
		double[] appIdStartTime = new double[SDNBroker.appId];
		double[] appIdFinishTime = new double[SDNBroker.appId];
		
		double serveTime, totalTime = 0;

		Log.printLine();
		Log.printLine("========== DETAILED RESPONSE TIME OF WORKLOADS ===========");

		if(wls.size() == 0) return;
		
		Log.print(String.format(fString, "App_ID"));
		printRequestTitle(wls.get(0).request);
		Log.print(String.format(fString, "ResponseTime"));
		Log.printLine();

		for(Workload wl:wls) {
			Log.print(String.format(fInt, wl.appId));
			
			startTime = finishTime = -1;
			printRequest(wl.request);
			
			serveTime= (finishTime - startTime);
			Log.print(String.format(fFloat, serveTime));
			totalTime += serveTime;
			
			appIdNum[wl.appId] ++;	//How many workloads in this app.
			appIdTime[wl.appId] += serveTime;
			if(appIdStartTime[wl.appId] <=0) {
				appIdStartTime[wl.appId] = wl.time;
			}
			appIdFinishTime[wl.appId] = wl.time;
			Log.printLine();
		}

		Log.printLine("========== AVERAGE RESULT OF WORKLOADS ===========");
		for(int i=0; i<SDNBroker.appId; i++) {
			Log.printLine("App Id ("+i+"): "+appIdNum[i]+" requests, Start=" + appIdStartTime[i]+
					", Finish="+appIdFinishTime[i]+", Rate="+(double)appIdNum[i]/(appIdFinishTime[i] - appIdStartTime[i])+
					" req/sec, Response time=" + appIdTime[i]/appIdNum[i]);
		}
		
		//printGroupStatistics(WORKLOAD_GROUP_PRIORITY, appIdNum, appIdTime);
		
		Log.printLine("Average Response Time:"+(totalTime / wls.size()));
		
	}

	private static void printRequestTitle(Request req) {
		//Log.print(String.format(fString, "Req_ID"));
		//Log.print(String.format(fFloat, req.getStartTime()));
		//Log.print(String.format(fFloat, req.getFinishTime()));
		
		List<Activity> acts = req.getRemovedActivities();
		for(Activity act:acts) {
			if(act instanceof Transmission) {
				Transmission tr=(Transmission)act;
				Log.print(String.format(fString, "Tr:Size"));
				Log.print(String.format(fString, "Tr:Channel"));
				
				Log.print(String.format(fString, "Tr:time"));
				Log.print(String.format(fString, "Tr:Start"));
				Log.print(String.format(fString, "Tr:End"));
				
				printRequestTitle(tr.getPackage().getPayload());
			}
			else {
				Log.print(String.format(fString, "Pr:Size"));
				
				Log.print(String.format(fString, "Pr:time"));
				Log.print(String.format(fString, "Pr:Start"));
				Log.print(String.format(fString, "Pr:End"));
			}
		}
	}
	
	private static void printRequest(Request req) {
		//Log.print(String.format(fInt, req.getRequestId()));
		//Log.print(String.format(fFloat, req.getStartTime()));
		//Log.print(String.format(fFloat, req.getFinishTime()));
		
		List<Activity> acts = req.getRemovedActivities();
		for(Activity act:acts) {
			if(act instanceof Transmission) {
				Transmission tr=(Transmission)act;
				Log.print(String.format(fInt, tr.getPackage().getSize()));
				Log.print(String.format(fInt, tr.getPackage().getFlowId()));
				
				Log.print(String.format(fFloat, tr.getPackage().getFinishTime() - tr.getPackage().getStartTime()));
				Log.print(String.format(fFloat, tr.getPackage().getStartTime()));
				Log.print(String.format(fFloat, tr.getPackage().getFinishTime()));
				
				printRequest(tr.getPackage().getPayload());
			}
			else {
				Processing pr=(Processing)act;
				Log.print(String.format(fInt, pr.getCloudlet().getCloudletLength()));

				Log.print(String.format(fFloat, pr.getCloudlet().getActualCPUTime()));
				Log.print(String.format(fFloat, pr.getCloudlet().getExecStartTime()));
				Log.print(String.format(fFloat, pr.getCloudlet().getFinishTime()));

				if(startTime == -1) startTime = pr.getCloudlet().getExecStartTime();
				finishTime=pr.getCloudlet().getFinishTime();
			}
		}
	}
	
	public static void printGroupStatistics(int groupSeperateNum, int[] appIdNum, double[] appIdTime) {

		double prioritySum = 0, standardSum = 0;
		int priorityReqNum = 0, standardReqNum =0;
		
		for(int i=0; i<SDNBroker.appId; i++) {
			double avgResponseTime = appIdTime[i]/appIdNum[i];
			if(i<groupSeperateNum) {
				prioritySum += avgResponseTime;
				priorityReqNum += appIdNum[i];
			}
			else {
				standardSum += avgResponseTime;
				standardReqNum += appIdNum[i];
			}
		}

		Log.printLine("Average Response Time(Priority):"+(prioritySum / priorityReqNum));
		Log.printLine("Average Response Time(Standard):"+(standardSum / standardReqNum));
	}
	
	
}
