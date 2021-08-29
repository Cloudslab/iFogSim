package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.entities.MicroserviceFogDevice;
import org.fog.entities.PlacementRequest;
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.RandomMobilityGenerator;
import org.fog.mobilitydata.References;
import org.fog.placement.LocationHandler;
import org.fog.placement.MicroservicesController;
import org.fog.placement.MicroservicesMobilityClusteringController;
import org.fog.placement.PlacementLogicFactory;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.*;

/**
 * Simulation setup for Microservices Application
 * This test covers featured such as,
 * 1. creation of clusters among fog nodes
 * 2. horizontally scaling microservices (instead of vertical scaling) and load balancing among them
 * 3. routing based on destination device id using service discovery.
 * 4. heterogeneity of device resources.
 *
 * @author Samodha Pallewatta
 */

/**
 * Config properties
 * SIMULATION_MODE -> dynamic or static
 * PR_PROCESSING_MODE -> PERIODIC
 * ENABLE_RESOURCE_DATA_SHARING -> false (not needed as FONs placed at the highest level.
 * DYNAMIC_CLUSTERING -> true (for clustered) and false (for not clustered) * (also compatible with static clustering)
 */
public class CardiovascularHealthMonitoringApplication {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static Map<Integer, Integer> userMobilityPattern = new HashMap<Integer, Integer>();
    static LocationHandler locator;

    static boolean CLOUD = false;

    static double SENSOR_TRANSMISSION_TIME = 10;
    static int numberOfMobileUser = 5;

    //cluster link latency 2ms
    static Double clusterLatency = 2.0;

    // TODO: 8/8/2021  not required for this scenario
    // if random mobility generator for users is True, new random dataset will be created for each user
    static boolean randomMobility_generator = true; // To use random datasets
    static boolean renewDataset = false; // To overwrite existing random datasets
    static List<Integer> clusteringLevels = new ArrayList<Integer>(); // The selected fog layers for clustering


    public static void main(String[] args) {

        Log.printLine("Starting Cardiovascular Health Monitoring Application...");

        try {

            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "Cardiovascular Health Monitoring Application CHM)"; // identifier of the application

            FogBroker broker = new FogBroker("broker");

            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            DataParser dataObject = new DataParser();
            locator = new LocationHandler(dataObject);

            String datasetReference = References.dataset_reference;

            if (randomMobility_generator) {
                datasetReference = References.dataset_random;
                createRandomMobilityDatasets(References.random_walk_mobility_model, datasetReference, renewDataset);
            }

            createMobileUser(broker.getId(), application, datasetReference);
            createFogDevices(broker.getId(), application);

            List<Integer> clusterLevelIdentifier = new ArrayList<>();
            clusterLevelIdentifier.add(2);

            /**
             * Central controller for performing preprocessing functions
             */
            List<Application> appList = new ArrayList<>();
            appList.add(application);


            int placementAlgo = PlacementLogicFactory.CLUSTERED_MICROSERVICES_PLACEMENT;
            MicroservicesMobilityClusteringController microservicesController = new MicroservicesMobilityClusteringController("controller", fogDevices, sensors, appList, clusterLevelIdentifier, clusterLatency, placementAlgo,locator);

            // generate placement requests
            List<PlacementRequest> placementRequests = new ArrayList<>();
            for (Sensor s : sensors) {
                Map<String, Integer> placedMicroservicesMap = new HashMap<>();
                placedMicroservicesMap.put("clientModule", s.getGatewayDeviceId());
                PlacementRequest p = new PlacementRequest(s.getAppId(), s.getId(), s.getGatewayDeviceId(), placedMicroservicesMap);
                placementRequests.add(p);
            }

            microservicesController.submitPlacementRequests(placementRequests, 1);

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            Log.printLine("CHM app finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void createRandomMobilityDatasets(int mobilityModel, String datasetReference, boolean renewDataset) throws IOException, ParseException {
        RandomMobilityGenerator randMobilityGenerator = new RandomMobilityGenerator();
        for (int i = 0; i < numberOfMobileUser; i++) {

            randMobilityGenerator.createRandomData(mobilityModel, i + 1, datasetReference, renewDataset);
        }
    }

    /**
     * Creates the fog devices in the physical topology of the simulation.
     *
     * @param userId
     */
    private static void createFogDevices(int userId, Application app) throws NumberFormatException, IOException {
        locator.parseResourceInfo();


        if (locator.getLevelWiseResources(locator.getLevelID("Cloud")).size() == 1) {

            FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0.01, 16 * 103, 16 * 83.25, MicroserviceFogDevice.CLOUD); // creates the fog device Cloud at the apex of the hierarchy with level=0
            cloud.setParentId(References.NOT_SET);
            locator.linkDataWithInstance(cloud.getId(), locator.getLevelWiseResources(locator.getLevelID("Cloud")).get(0));
            cloud.setLevel(0);
            fogDevices.add(cloud);

            for (int i = 0; i < locator.getLevelWiseResources(locator.getLevelID("Proxy")).size(); i++) {

                FogDevice proxy = createFogDevice("proxy-server_" + i, 2800, 4000, 10000, 10000, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FON); // creates the fog device Proxy Server (level=1)
                locator.linkDataWithInstance(proxy.getId(), locator.getLevelWiseResources(locator.getLevelID("Proxy")).get(i));
                proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
                proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms
                proxy.setLevel(1);
                fogDevices.add(proxy);

            }

            for (int i = 0; i < locator.getLevelWiseResources(locator.getLevelID("Gateway")).size(); i++) {

                FogDevice gateway = createFogDevice("gateway_" + i, 2800, 4000, 10000, 10000, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FCN);
                locator.linkDataWithInstance(gateway.getId(), locator.getLevelWiseResources(locator.getLevelID("Gateway")).get(i));
                gateway.setParentId(locator.determineParent(gateway.getId(), References.SETUP_TIME));
                gateway.setUplinkLatency(4);
                gateway.setLevel(2);
                fogDevices.add(gateway);
            }

        }
    }

    private static void createMobileUser(int userId, Application app, String datasetReference) throws IOException {

        for (int id = 1; id <= numberOfMobileUser; id++)
            userMobilityPattern.put(id, References.DIRECTIONAL_MOBILITY);

        locator.parseUserInfo(userMobilityPattern, datasetReference);

        List<String> mobileUserDataIds = locator.getMobileUserDataId();

        for (int i = 0; i < numberOfMobileUser; i++) {
            FogDevice mobile = addMobile("mobile_" + i, userId, app, References.NOT_SET); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 2 ms
            locator.linkDataWithInstance(mobile.getId(), mobileUserDataIds.get(i));
            mobile.setLevel(3);

            fogDevices.add(mobile);
        }

    }

    /**
     * Creates a vanilla fog device
     *
     * @param nodeName    name of the device to be used in simulation
     * @param mips        MIPS
     * @param ram         RAM
     * @param upBw        uplink bandwidth
     * @param downBw      downlink bandwidth
     * @param ratePerMips cost rate per MIPS used
     * @param busyPower
     * @param idlePower
     * @return
     */
    private static MicroserviceFogDevice createFogDevice(String nodeName, long mips,
                                                         int ram, long upBw, long downBw, double ratePerMips, double busyPower, double idlePower, String deviceType) {

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
                new FogLinearPowerModel(busyPower, idlePower)
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

        MicroserviceFogDevice fogdevice = null;
        try {
            fogdevice = new MicroserviceFogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 10000, 0, ratePerMips, deviceType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fogdevice;
    }

    private static FogDevice addMobile(String name, int userId, Application app, int parentId) {
        FogDevice mobile = createFogDevice(name, 200, 2048, 10000, 270, 0, 87.53, 82.44, MicroserviceFogDevice.CLIENT);
        mobile.setParentId(parentId);
        //locator.setInitialLocation(name,drone.getId());
        Sensor mobileSensor = new Sensor("s-" + name, "SENSOR", userId, app.getAppId(), new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
        mobileSensor.setApp(app);
        sensors.add(mobileSensor);
        Actuator mobileDisplay = new Actuator("a-" + name, userId, app.getAppId(), "DISPLAY");
        actuators.add(mobileDisplay);

        mobileSensor.setGatewayDeviceId(mobile.getId());
        mobileSensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms

        mobileDisplay.setGatewayDeviceId(mobile.getId());
        mobileDisplay.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
        mobileDisplay.setApp(app);

        return mobile;
    }


    @SuppressWarnings({"serial"})
    private static Application createApplication(String appId, int userId) {

        Application application = Application.createApplication(appId, userId);

        /*
         * Adding modules (vertices) to the application model (directed graph)
         */
        application.addAppModule("clientModule", 128, 150, 100);
        application.addAppModule("mService1", 512, 250, 200);
        application.addAppModule("mService2", 512, 350, 200);
        application.addAppModule("mService3", 2048, 450, 1000);

        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */

        application.addAppEdge("SENSOR", "clientModule", 1000, 500, "SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("clientModule", "mService1", 2000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mService1", "mService2", 2500, 500, "FILTERED_DATA1", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mService1", "mService3", 4000, 500, "FILTERED_DATA2", Tuple.UP, AppEdge.MODULE);

        application.addAppEdge("mService2", "clientModule", 14, 500, "RESULT1", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("mService3", "clientModule", 28, 500, "RESULT2", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("clientModule", "DISPLAY", 14, 500, "RESULT1_DISPLAY", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge("clientModule", "DISPLAY", 14, 500, "RESULT2_DISPLAY", Tuple.DOWN, AppEdge.ACTUATOR);


        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("clientModule", "SENSOR", "RAW_DATA", new FractionalSelectivity(0.9));
        application.addTupleMapping("mService1", "RAW_DATA", "FILTERED_DATA1", new FractionalSelectivity(1.0));
        application.addTupleMapping("mService1", "RAW_DATA", "FILTERED_DATA2", new FractionalSelectivity(1.0));
        application.addTupleMapping("mService2", "FILTERED_DATA1", "RESULT1", new FractionalSelectivity(1.0));
        application.addTupleMapping("mService3", "FILTERED_DATA2", "RESULT2", new FractionalSelectivity(1.0));
        application.addTupleMapping("clientModule", "RESULT1", "RESULT1_DISPLAY", new FractionalSelectivity(1.0));
        application.addTupleMapping("clientModule", "RESULT2", "RESULT2_DISPLAY", new FractionalSelectivity(1.0));

        application.setSpecialPlacementInfo("mService3", "cloud");
        if (CLOUD) {
            application.setSpecialPlacementInfo("mService1", "cloud");
            application.setSpecialPlacementInfo("mService2", "cloud");
        }

        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("SENSOR");
            add("clientModule");
            add("mService1");
            add("mService2");
            add("clientModule");
            add("DISPLAY");
        }});

        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};
        application.setLoops(loops);


//        application.createDAG();

        return application;
    }


}