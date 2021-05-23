package org.fog.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.FogBroker;
import org.fog.entities.PhysicalTopology;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.utils.JsonToTopology;

/**
 * Simulation setup for EEG Beam Tractor Game extracting physical topology 
 * @author Harshit Gupta
 *
 */
public class CleanFromJson {

	public static void main(String[] args) {

		Log.printLine("Starting VRGame...");

		try {
			Log.disable();
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false; // mean trace events

			CloudSim.init(num_user, calendar, trace_flag);

			String appId = "vr_game";
			
			FogBroker broker = new FogBroker("broker");
			
			Application application = createApplication(appId, broker.getId());
			application.setUserId(broker.getId());
			
			/*
			 * Creating the physical topology from specified JSON file
			 */
			PhysicalTopology physicalTopology = JsonToTopology.getPhysicalTopology(broker.getId(), appId, "topologies/routerTopology");
						
			Controller controller = new Controller("master-controller", physicalTopology.getFogDevices(), physicalTopology.getSensors(), 
					physicalTopology.getActuators());
			
			controller.submitApplication(application, 0, new ModulePlacementEdgewards(physicalTopology.getFogDevices(), 
					physicalTopology.getSensors(), physicalTopology.getActuators(), 
					application, ModuleMapping.createModuleMapping()));
			
			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("VRGame finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
	
	
	@SuppressWarnings({ "serial" })
	private static Application createApplication(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId);
		application.addAppModule("client", 10);
		application.addAppModule("classifier", 10);
		application.addAppModule("tuner", 10);
		
		application.addTupleMapping("client", "TEMP", "_SENSOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "CLASSIFICATION", "ACTUATOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "CLASSIFICATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "HISTORY", new FractionalSelectivity(0.1));
		application.addTupleMapping("tuner", "HISTORY", "TUNING_PARAMS", new FractionalSelectivity(1.0));
	
		application.addAppEdge("TEMP", "client", 1000, 100, "TEMP", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client", "classifier", 8000, 100, "_SENSOR", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("classifier", "tuner", 1000000, 100, "HISTORY", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("classifier", "client", 1000, 100, "CLASSIFICATION", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("tuner", "classifier", 1000, 100, "TUNING_PARAMS", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("client", "MOTOR", 1000, 100, "ACTUATOR", Tuple.DOWN, AppEdge.ACTUATOR);
		
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("TEMP");add("client");add("classifier");add("client");add("MOTOR");}});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("classifier");add("tuner");add("classifier");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
		
		application.setLoops(loops);
		
		//GeoCoverage geoCoverage = new GeoCoverage(-100, 100, -100, 100);
		return application;
	}
}