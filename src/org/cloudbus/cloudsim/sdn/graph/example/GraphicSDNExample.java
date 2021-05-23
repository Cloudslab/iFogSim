package org.cloudbus.cloudsim.sdn.graph.example;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.Activity;
import org.cloudbus.cloudsim.sdn.Processing;
import org.cloudbus.cloudsim.sdn.Request;
import org.cloudbus.cloudsim.sdn.SDNDatacenter;
import org.cloudbus.cloudsim.sdn.Transmission;
import org.cloudbus.cloudsim.sdn.example.SDNBroker;
import org.cloudbus.cloudsim.sdn.example.SimpleNetworkOperatingSystem;
import org.cloudbus.cloudsim.sdn.example.VmAllocationPolicyCombinedLeastFullFirst;
import org.cloudbus.cloudsim.sdn.example.Workload;
import org.cloudbus.cloudsim.sdn.example.VmSchedulerSpaceSharedEnergy;

/** A simple example showing how to create a datacenter with one host and run one cloudlet on it */
public class GraphicSDNExample {
	
	private String physicalTopologyFile = "";
	private String deploymentFile = "";
	private String workloads_background = "";
	private String workloads = "";
	
	private JTextArea outputArea;
	
	private SDNBroker broker;
	
	public GraphicSDNExample(String phy, String vir, String wlbk, String wl, JTextArea area){
		physicalTopologyFile = phy;
		deploymentFile = vir;
		workloads_background = wlbk;
		workloads = wl;
		outputArea = area;
	}

	public boolean simulate() {
		
		//append("Starting CloudSim SDN...");

		try {
			// Initialize
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events
			CloudSim.init(num_user, calendar, trace_flag);

			// Create a Datacenter
			SDNDatacenter datacenter = createSDNDatacenter("Datacenter_0", physicalTopologyFile);

			// Broker
			broker = createBroker();
			int brokerId = broker.getId();

			broker.submitDeployApplication(datacenter, deploymentFile);
			broker.submitRequests(workloads_background);
			broker.submitRequests(workloads);
			
			// Sixth step: Starts the simulation
			CloudSim.startSimulation();
			CloudSim.stopSimulation();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			//append("====== RUNNING ERROR ======");
		}
		return false;
	}
	
	public void output(){
		try {
			// Final step: Print hosts' total utilization.
			List<Host> hostList = nos.getHostList();
			
			printEnergyConsumption(hostList);
			
			//Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			printCloudletList(newList);
			
			List<Workload> wls = broker.getWorkloads();
			printWorkloadList(wls);
			
			append("CloudSim SDN finished!");
			
		} catch (Exception e) {
			e.printStackTrace();
			append("====== OUTPUT ERROR ======");
		}
	}
	
	private void printEnergyConsumption(List<Host> hostList) {
		double totalEnergyConsumption = 0;
		for(Host host:hostList) {
			double energy = ((VmSchedulerSpaceSharedEnergy) host.getVmScheduler()).getUtilizationEnergyConsumption();
			append("Host #"+host.getId()+": "+energy);
			totalEnergyConsumption+= energy;

			printHostUtilizationHistory(((VmSchedulerSpaceSharedEnergy) host.getVmScheduler()).getUtilizationHisotry());

		} 
		append("Total energy consumed: "+totalEnergyConsumption);
		
	}

	private void printHostUtilizationHistory(
			List<org.cloudbus.cloudsim.sdn.example.VmSchedulerSpaceSharedEnergy.HistoryEntry> utilizationHisotry) {
		for(org.cloudbus.cloudsim.sdn.example.VmSchedulerSpaceSharedEnergy.HistoryEntry h:utilizationHisotry) {
			append(h.startTime+", "+h.usedMips);
		}
	}

	/**
	 * Creates the datacenter.
	 *
	 * @param name the name
	 *
	 * @return the datacenter
	 */
	private SimpleNetworkOperatingSystem nos;
	private SDNDatacenter createSDNDatacenter(String name, String physicalTopology) {

		// In order to get Host information, pre-create NOS.
		
		nos = new SimpleNetworkOperatingSystem(physicalTopology);
		List<Host> hostList = nos.getHostList();

		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// Create Datacenter with previously set parameters
		SDNDatacenter datacenter = null;
		try {
			datacenter = new SDNDatacenter(name, characteristics, new VmAllocationPolicyCombinedLeastFullFirst(hostList), storageList, 0, nos);
			//datacenter = new SDNDatacenter(name, characteristics, new VmAllocationPolicyCombinedMostFullFirst(hostList), storageList, 0, nos);
			//datacenter = new SDNDatacenter(name, characteristics, new VmAllocationPolicyMipsLeastFullFirst(hostList), storageList, 0, nos);
			
			nos.setDatacenter(datacenter);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return datacenter;
	}

	// We strongly encourage users to develop their own broker policies, to
	// submit vms and cloudlets according
	// to the specific rules of the simulated scenario
	/**
	 * Creates the broker.
	 *
	 * @return the datacenter broker
	 */
	private SDNBroker createBroker() {
		SDNBroker broker = null;
		try {
			broker = new SDNBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}
	
	public String indent = ",";
	public String tabSize = "10";
	public String fString = 	"%"+tabSize+"s"+indent;
	public String fInt = 	"%"+tabSize+"d"+indent;
	public String fFloat = 	"%"+tabSize+".2f"+indent;
	
	private void printCloudletList(List<Cloudlet> list) {
		int size = list.size();
		String content = "";
		Cloudlet cloudlet;

		append("");
		append("========== OUTPUT ==========");
		
		content = String.format(fString, "Cloudlet_ID") + 
				  String.format(fString, "STATUS" ) + 
				  String.format(fString, "DataCenter_ID") + 
				  String.format(fString, "VM_ID") + 
				  String.format(fString, "Length") + 
				  String.format(fString, "Time") + 
				  String.format(fString, "Start Time") + 
				  String.format(fString, "Finish Time");
		append(content);

		//DecimalFormat dft = new DecimalFormat("######.##");
		for (int i = 0; i < size; i++) {
			cloudlet = list.get(i);
			printCloudlet(cloudlet);
		}
	}
	
	private void printCloudlet(Cloudlet cloudlet) {
		String content = String.format(fInt, cloudlet.getCloudletId());

		if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
			content = content +
					  String.format(fString, "SUCCESS") +
					  String.format(fInt, cloudlet.getResourceId()) +
					  String.format(fInt, cloudlet.getVmId()) +
					  String.format(fInt, cloudlet.getCloudletLength()) +
					  String.format(fFloat, cloudlet.getActualCPUTime()) +
					  String.format(fFloat, cloudlet.getExecStartTime()) +
					  String.format(fFloat, cloudlet.getFinishTime());
		}
		else {
			content += "FAILED";
		}
		append(content);
	}
	
	private double startTime, finishTime;
	private void printWorkloadList(List<Workload> wls) {
		int[] appIdNum = new int[SDNBroker.appId];
		double[] appIdTime = new double[SDNBroker.appId];
		double[] appIdStartTime = new double[SDNBroker.appId];
		double[] appIdFinishTime = new double[SDNBroker.appId];
		
		double serveTime, totalTime = 0;

		append(" ");
		append("========== OUTPUT ==========");

		printRequestTitle(wls.get(0).request);
		append(" ");

		for(Workload wl:wls) {
			startTime = finishTime = -1;
			printRequest(wl.request);
			
			serveTime= (finishTime - startTime);
			append(String.format(fFloat, serveTime));
			totalTime += serveTime;
			
			appIdNum[wl.appId] ++;
			appIdTime[wl.appId] += serveTime;
			if(appIdStartTime[wl.appId] <=0) {
				appIdStartTime[wl.appId] = wl.time;
			}
			appIdFinishTime[wl.appId] = wl.time;
			append(" ");
		}
		for(int i=0; i<SDNBroker.appId; i++) {
			append("App Id ("+i+"): "+appIdNum[i]+" requests, Start=" + appIdStartTime[i]+
					", Finish="+appIdFinishTime[i]+", Rate="+(double)appIdNum[i]/(appIdFinishTime[i] - appIdStartTime[i])+
					" req/sec, Response time=" + appIdTime[i]/appIdNum[i]);
			
		}
		append("Average Response Time:"+(totalTime / wls.size()));
		
	}

	private void printRequestTitle(Request req) {
		String content = String.format(fString, "Req_ID");
		//Log.print(String.format(fFloat, req.getStartTime()));
		//Log.print(String.format(fFloat, req.getFinishTime()));
		
		List<Activity> acts = req.getRemovedActivities();
		for(Activity act:acts) {
			if(act instanceof Transmission) {
				Transmission tr=(Transmission)act;
				content += 
						String.format(fString, "Tr:Size") +
						String.format(fString, "Tr:Channel") +
						String.format(fString, "Tr:time") +
						String.format(fString, "Tr:Start") +
						String.format(fString, "Tr:End");
				
				printRequestTitle(tr.getPackage().getPayload());
			}
			else {
				content += 
						String.format(fString, "Pr:Size") +
						String.format(fString, "Pr:time") +
						String.format(fString, "Pr:Start") +
						String.format(fString, "Pr:End");
			}
		}
		append(content);
	}
	
	private void printRequest(Request req) {
		String content = String.format(fInt, req.getRequestId());
		//Log.print(String.format(fFloat, req.getStartTime()));
		//Log.print(String.format(fFloat, req.getFinishTime()));
		
		List<Activity> acts = req.getRemovedActivities();
		for(Activity act:acts) {
			if(act instanceof Transmission) {
				Transmission tr=(Transmission)act;
				content +=
						String.format(fInt, tr.getPackage().getSize()) +
						String.format(fInt, tr.getPackage().getFlowId()) +
				
						String.format(fFloat, tr.getPackage().getFinishTime() - tr.getPackage().getStartTime()) +
						String.format(fFloat, tr.getPackage().getStartTime()) +
						String.format(fFloat, tr.getPackage().getFinishTime());
				
				printRequest(tr.getPackage().getPayload());
			}
			else {
				Processing pr=(Processing)act;
				content +=
						String.format(fInt, pr.getCloudlet().getCloudletLength()) +

						String.format(fFloat, pr.getCloudlet().getActualCPUTime()) +
						String.format(fFloat, pr.getCloudlet().getExecStartTime()) +
						String.format(fFloat, pr.getCloudlet().getFinishTime());

				if(startTime == -1) startTime = pr.getCloudlet().getExecStartTime();
				finishTime=pr.getCloudlet().getFinishTime();
			}
		}
		append(content);
	}
	
	private void append(String content){
		outputArea.append(content+"\n");
	}
}