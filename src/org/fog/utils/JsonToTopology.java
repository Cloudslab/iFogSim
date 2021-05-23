package org.fog.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.PhysicalTopology;
import org.fog.entities.Sensor;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class JsonToTopology {

	private static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	private static List<Sensor> sensors = new ArrayList<Sensor>();
	private static List<Actuator> actuators = new ArrayList<Actuator>();
 
	private static boolean isFogDevice(String name){
		for(FogDevice fogDevice : fogDevices){
			if(fogDevice.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
		
	}
	
	private static FogDevice getFogDevice(String name){
		for(FogDevice fogDevice : fogDevices){
			if(fogDevice.getName().equalsIgnoreCase(name))
				return fogDevice;
		}
		return null;
	}
	
	private static boolean isActuator(String name){
		for(Actuator actuator : actuators){
			if(actuator.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	private static Actuator getActuator(String name){
		for(Actuator actuator : actuators){
			if(actuator.getName().equalsIgnoreCase(name))
				return actuator;
		}
		return null;
	}
	
	private static boolean isSensor(String name){
		for(Sensor sensor : sensors){
			if(sensor.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	private static Sensor getSensor(String name){
		for(Sensor sensor : sensors){
			if(sensor.getName().equalsIgnoreCase(name))
				return sensor;
		}
		return null;
	}
	
	public static PhysicalTopology getPhysicalTopology(int userId, String appId, String physicalTopologyFile) throws Exception{
				
		fogDevices = new ArrayList<FogDevice>();
		sensors = new ArrayList<Sensor>();
		actuators = new ArrayList<Actuator>();
		
		
		try {
			JSONObject doc = (JSONObject) JSONValue.parse(new FileReader(physicalTopologyFile));
    		JSONArray nodes = (JSONArray) doc.get("nodes");
    		@SuppressWarnings("unchecked")
			Iterator<JSONObject> iter =nodes.iterator(); 
			while(iter.hasNext()){
				JSONObject node = iter.next();
				String nodeType = (String) node.get("type");
				String nodeName = (String) node.get("name");
				
				if(nodeType.equalsIgnoreCase("FOG_DEVICE")){
					long mips = (Long) node.get("mips");
					int ram = new BigDecimal((Long)node.get("ram")).intValueExact();
					long upBw = new BigDecimal((Long)node.get("upBw")).intValueExact();
					long downBw = new BigDecimal((Long)node.get("downBw")).intValueExact();
					int level = new BigDecimal((Long)node.get("level")).intValue();
					double ratePerMips = new BigDecimal((Double)node.get("ratePerMips")).doubleValue();
					
					FogDevice fogDevice = createFogDevice(nodeName, mips, ram, upBw, downBw, level, ratePerMips);
					fogDevice.setParentId(-1);
					
					fogDevices.add(fogDevice);

				} else if(nodeType.equals("SENSOR")){
					String sensorType = node.get("sensorType").toString();
					int distType = new BigDecimal((Long)node.get("distribution")).intValue();
					Distribution distribution = null;
					if(distType == Distribution.DETERMINISTIC)
						distribution = new DeterministicDistribution(new BigDecimal((Double)node.get("value")).doubleValue());
					else if(distType == Distribution.NORMAL){
						distribution = new NormalDistribution(new BigDecimal((Double)node.get("mean")).doubleValue(), 
								new BigDecimal((Double)node.get("stdDev")).doubleValue());
					} else if(distType == Distribution.UNIFORM){
						distribution = new UniformDistribution(new BigDecimal((Double)node.get("min")).doubleValue(), 
								new BigDecimal((Double)node.get("max")).doubleValue());
					}
					System.out.println("Sensor type : "+sensorType);
					sensors.add(new Sensor(nodeName, sensorType, userId, appId, distribution));
				} else if(nodeType.equals("ACTUATOR")){
					String actuatorType = node.get("actuatorType").toString();
					actuators.add(new Actuator(nodeName, userId, appId, actuatorType));
				}
			}
				
			JSONArray links = (JSONArray) doc.get("links");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> linksIter =links.iterator(); 
			while(linksIter.hasNext()){
				JSONObject link = linksIter.next();
				String src = (String) link.get("source");  
				String dst = (String) link.get("destination");
				double lat = (Double) link.get("latency");
				
				connectEntities(src, dst, lat);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PhysicalTopology physicalTopology = new PhysicalTopology();
		physicalTopology.setFogDevices(fogDevices);
		physicalTopology.setActuators(actuators);
		physicalTopology.setSensors(sensors);
		return physicalTopology;
	}
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new PowerModelLinear(107.339, 83.4333)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

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

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}

	private static void connectEntities(String src, String dst, double lat) {
		if(isFogDevice(src) && isFogDevice(dst)){
			FogDevice srcDev = getFogDevice(src);
			FogDevice destDev = getFogDevice(dst);
			FogDevice southernDev = (srcDev.getLevel() > destDev.getLevel())?srcDev:destDev;
			FogDevice northernDev = (srcDev.getLevel() > destDev.getLevel())?destDev:srcDev;
			southernDev.setUplinkLatency(lat);
			southernDev.setParentId(northernDev.getId());
		} else if(isFogDevice(src) && isSensor(dst)){
			FogDevice srcDev = getFogDevice(src);
			Sensor sensor = getSensor(dst);
			sensor.setLatency(lat);
			sensor.setGatewayDeviceId(srcDev.getId());
		} else if(isSensor(src) && isFogDevice(dst)){
			FogDevice fogDevice = getFogDevice(dst);
			Sensor sensor = getSensor(src);
			sensor.setLatency(lat);
			sensor.setGatewayDeviceId(fogDevice.getId());
		} else if(isFogDevice(src) && isActuator(dst)){
			FogDevice fogDevice = getFogDevice(src);
			Actuator actuator = getActuator(dst);
			actuator.setLatency(lat);
			actuator.setGatewayDeviceId(fogDevice.getId());
		} else if(isActuator(src) && isFogDevice(dst)){
			FogDevice fogDevice = getFogDevice(dst);
			Actuator actuator = getActuator(src);
			actuator.setLatency(lat);
			actuator.setGatewayDeviceId(fogDevice.getId());
		}
		
	}	
}
