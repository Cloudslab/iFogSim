package org.fog.placement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.mobilitydata.References;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.MigrationDelayMonitor;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.json.simple.JSONObject;


public class MobilityController extends SimEntity{
	
	public static boolean ONLY_CLOUD = false;
		
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	private LocationHandler locator;
	private Map<Integer, Integer> parentReference;


	private Map<String, Application> applications;
	private Map<String, Integer> appLaunchDelays;
	

	private Map<String, ModulePlacement> appModulePlacementPolicy;
	
	public MobilityController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, LocationHandler locator) {
		super(name);
		this.applications = new HashMap<String, Application>();
		setLocator(locator);
		setAppLaunchDelays(new HashMap<String, Integer>());
		setParentReference(new HashMap<Integer, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
		for(FogDevice fogDevice : fogDevices){
			fogDevice.setControllerId(getId());
		}
		setFogDevices(fogDevices);
		setActuators(actuators);
		setSensors(sensors);
		connectWithLatencies();
	}

	private void setParentReference(HashMap<Integer, Integer> parentReference) {
		// TODO Auto-generated method stub
		this.parentReference = parentReference;
	}

	private FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices()){
			if(id==fogDevice.getId())
				return fogDevice;
		}
		return null;
	}
	
	private void connectWithLatencies(){
		
		for (String dataId: locator.getDataIdsLevelReferences().keySet())
		{
			for(int instenceId: locator.getInstenceDataIdReferences().keySet())
			{
				if(locator.getInstenceDataIdReferences().get(instenceId).equals(dataId))
				{
					FogDevice fogDevice = getFogDeviceById(instenceId);
					if(locator.getDataIdsLevelReferences().get(dataId)==locator.getLevelID("User") && fogDevice.getParentId()==References.NOT_SET){
						int parentID = locator.determineParent(fogDevice.getId(),References.INIT_TIME);
						parentReference.put(fogDevice.getId(),parentID);
						fogDevice.setParentId(parentID);
					}
					else
						parentReference.put(fogDevice.getId(),fogDevice.getParentId());
				}
			}
		}
		
		
		FogDevice cloud = getCloud();
		parentReference.put(cloud.getId(),cloud.getParentId());
		
		for(FogDevice fogDevice : getFogDevices()){
			FogDevice parent = getFogDeviceById(parentReference.get(fogDevice.getId()));
			if(parent == null)
				continue;
			double latency = fogDevice.getUplinkLatency();
			parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
			parent.getChildrenIds().add(fogDevice.getId());
			System.out.println("Child "+fogDevice.getName()+"\t----->\tParent "+parent.getName());
		}
	}
	
	@Override
	public void startEntity() {
		for(String appId : applications.keySet()){
			if(getAppLaunchDelays().get(appId)==0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		
		sendNow(getId(), FogEvents.MOBILITY_SUBMIT);
		
		for(FogDevice dev : getFogDevices())
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);

	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.MOBILITY_SUBMIT:
			processMobilityData();
			break;
		case FogEvents.MOBILITY_MANAGEMENT:
			processMobility(ev);
			break;
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);
			break;
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();
			break;
		case FogEvents.STOP_SIMULATION:
			CloudSim.stopSimulation();
			printTimeDetails();
			printPowerDetails();
			printCostDetails();
			printNetworkUsageDetails();
			printMigrationDelayDetails();
			System.exit(0);
			break;
			
		}
	}
	
	private void printMigrationDelayDetails() {
		// TODO Auto-generated method stub
		System.out.println("Total time required for module migration = "+MigrationDelayMonitor.getMigrationDelay());
	}

	/*private void printFogDeviceChildren(int deviceID) {
		// TODO Auto-generated method stub
		System.out.println("Childs of "+getFogDeviceById(deviceID).getName());
		for(Integer childId:getFogDeviceById(deviceID).getChildrenIds())
			System.out.println(getFogDeviceById(childId).getName()+"("+childId+")");
		
	}*/

	@SuppressWarnings("unchecked")
	private void processMobility(SimEvent ev) {
		// TODO Auto-generated method stub
		FogDevice fogDevice = (FogDevice) ev.getData();
		FogDevice prevParent = getFogDeviceById(parentReference.get(fogDevice.getId()));
		FogDevice newParent = getFogDeviceById(locator.determineParent(fogDevice.getId(),CloudSim.clock()));
		System.out.println(CloudSim.clock()+" Starting Mobility Management for "+fogDevice.getName());
		parentReference.put(fogDevice.getId(),newParent.getId());
		List<String>migratingModules = new ArrayList<String>();
		if(prevParent.getId()!=newParent.getId()) {
			//printFogDeviceChildren(newParent.getId());
			//printFogDeviceChildren(prevParent.getId());
			
			//common ancestor policy
			List<Integer>newParentPath = getPathsToCloud(newParent.getId());
			List<Integer>prevParentPath = getPathsToCloud(prevParent.getId());
			int commonAncestor = determineAncestor(newParentPath,prevParentPath);
			
			
			fogDevice.setParentId(newParent.getId());
			System.out.println("Child "+fogDevice.getName()+"\t----->\tParent "+newParent.getName());
			newParent.getChildToLatencyMap().put(fogDevice.getId(), fogDevice.getUplinkLatency());
			newParent.addChild(fogDevice.getId());
			prevParent.removeChild(fogDevice.getId());
			for(String applicationName:fogDevice.getActiveApplications()){
				migratingModules = getAppModulePlacementPolicy().get(applicationName).getModulesOnPath().get(fogDevice.getId()).get(prevParent.getId());
				getAppModulePlacementPolicy().get(applicationName).getModulesOnPath().get(fogDevice.getId()).remove(prevParent.getId());
				getAppModulePlacementPolicy().get(applicationName).getModulesOnPath().get(fogDevice.getId()).put(newParent.getId(),migratingModules);	
				for(String moduleName:migratingModules){
					double upDelay = getUpDelay(prevParent.getId(),commonAncestor,getApplications().get(applicationName).getModuleByName(moduleName));
					double downDelay = getDownDelay(newParent.getId(),commonAncestor,getApplications().get(applicationName).getModuleByName(moduleName));
					JSONObject jsonSend = new JSONObject();
					jsonSend.put("module", getApplications().get(applicationName).getModuleByName(moduleName));
					jsonSend.put("delay", upDelay);
					
					JSONObject jsonReceive = new JSONObject();
					jsonReceive.put("module", getApplications().get(applicationName).getModuleByName(moduleName));
					jsonReceive.put("delay", downDelay);
					jsonReceive.put("application", getApplications().get(applicationName));
					
					send(prevParent.getId(),upDelay, FogEvents.MODULE_SEND, jsonSend);
					send(newParent.getId(),downDelay, FogEvents.MODULE_RECEIVE, jsonReceive);
					System.out.println("Migrating "+moduleName+" from "+prevParent.getName()+" to "+newParent.getName());
				}
			}
			
			// = get
			//printFogDeviceChildren(newParent.getId());
			//printFogDeviceChildren(prevParent.getId());
		}
		
		
		
	}

	private double getDownDelay(int deviceID, int commonAncestorID, AppModule module) {
		// TODO Auto-generated method stub
		double networkDelay = 0.0;
		while(deviceID!=commonAncestorID){	
			networkDelay = networkDelay + module.getSize()/getFogDeviceById(deviceID).getDownlinkBandwidth();
			deviceID = getFogDeviceById(deviceID).getParentId();
		}
		return networkDelay;
	}

	private double getUpDelay(int deviceID, int commonAncestorID, AppModule module) {
		// TODO Auto-generated method stub
		double networkDelay = 0.0;
		while(deviceID!=commonAncestorID){	
			networkDelay = networkDelay + module.getSize()/getFogDeviceById(deviceID).getUplinkBandwidth();
			deviceID = getFogDeviceById(deviceID).getParentId();
		}
		return networkDelay;
	}

	private int determineAncestor(List<Integer> newParentPath, List<Integer> prevParentPath) {
		// TODO Auto-generated method stub
		List<Integer> common = newParentPath.stream().filter(prevParentPath::contains).collect(Collectors.toList());
		return common.get(0);
	}

	private List<Integer> getPathsToCloud(int deviceID) {
		// TODO Auto-generated method stub
		List<Integer>path = new ArrayList<Integer>();
		while(!locator.isCloud(deviceID)){
			path.add(deviceID);
			deviceID = getFogDeviceById(deviceID).getParentId();
		}
		path.add(getCloud().getId());
		return path;
	}

	private void processMobilityData() {
		// TODO Auto-generated method stub
		List<Double>timeSheet = new ArrayList<Double>();
		for(FogDevice fogDevice : getFogDevices()){
			if(locator.isAMobileDevice(fogDevice.getId())) {
				timeSheet = locator.getTimeSheet(fogDevice.getId());
				for(double timeEntry:timeSheet)
					send(getId(), timeEntry, FogEvents.MOBILITY_MANAGEMENT,fogDevice);
			}
		}
	}

	private void printNetworkUsageDetails() {
		System.out.println("Total network usage = "+NetworkUsageMonitor.getNetworkUsage()/Config.MAX_SIMULATION_TIME);		
	}

	private FogDevice getCloud(){
		for(FogDevice dev : getFogDevices())
			if(dev.getName().equals("cloud"))
				return dev;
		return null;
	}
	
	private void printCostDetails(){
		System.out.println("Cost of execution in cloud = "+getCloud().getTotalCost());
	}
	
	private void printPowerDetails() {
		for(FogDevice fogDevice : getFogDevices()){
			System.out.println(fogDevice.getName() + " : Energy Consumed = "+fogDevice.getEnergyConsumption());
		}
	}

	/*
	private String getStringForLoopId(int loopId){
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}
	*/
	private void printTimeDetails() {
		System.out.println("=========================================");
		System.out.println("============== RESULTS ==================");
		System.out.println("=========================================");
		System.out.println("EXECUTION TIME : "+ (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
		System.out.println("=========================================");
		//System.out.println("APPLICATION LOOP DELAYS");
		//System.out.println("=========================================");
		//for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){
			/*double average = 0, count = 0;
			for(int tupleId : TimeKeeper.getInstance().getLoopIdToTupleIds().get(loopId)){
				Double startTime = 	TimeKeeper.getInstance().getEmitTimes().get(tupleId);
				Double endTime = 	TimeKeeper.getInstance().getEndTimes().get(tupleId);
				if(startTime == null || endTime == null)
					break;
				average += endTime-startTime;
				count += 1;
			}
			System.out.println(getStringForLoopId(loopId) + " ---> "+(average/count));*/
			//System.out.println(getStringForLoopId(loopId) + " ---> "+TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
		//}
		System.out.println("=========================================");
		System.out.println("TUPLE CPU EXECUTION DELAY");
		System.out.println("=========================================");
		
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()){
			System.out.println(tupleType + " ---> "+TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
		}
		
		System.out.println("=========================================");
	}

	protected void manageResources(){
		send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
	}
	
	private void processTupleFinished(SimEvent ev) {
	}
	
	@Override
	public void shutdownEntity() {	
	}
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement){
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors){
			sensor.setApp(getApplications().get(sensor.getAppId()));
		}
		for(Actuator ac : actuators){
			ac.setApp(getApplications().get(ac.getAppId()));
		}
		
		for(AppEdge edge : application.getEdges()){
			if(edge.getEdgeType() == AppEdge.ACTUATOR){
				String moduleName = edge.getSource();
				for(Actuator actuator : getActuators()){
					if(actuator.getActuatorType().equalsIgnoreCase(edge.getDestination()))
						application.getModuleByName(moduleName).subscribeActuator(actuator.getId(), edge.getTupleType());
				}
			}
		}	
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application){
		System.out.println(CloudSim.clock()+" Submitted application "+ application.getAppId());
		FogUtils.appIdToGeoCoverageMap.put(application.getAppId(), application.getGeoCoverage());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		for(FogDevice fogDevice : fogDevices){
			sendNow(fogDevice.getId(), FogEvents.ACTIVE_APP_UPDATE, application);
		}
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
		for(Integer deviceId : deviceToModuleMap.keySet()){
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
			}
		}
	}

	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		for(Sensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}
	
	public LocationHandler getLocator() {
		return locator;
	}

	public void setLocator(LocationHandler locator) {
		this.locator = locator;
	}
}