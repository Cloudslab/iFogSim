package org.fog.test.perfeval;

import org.apache.commons.math3.util.Pair;
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
 * 1. creation of clusters among fog nodes using dynamic clustering
 * 2. mobility of end user devices and microservice migration
 *
 * @author Samodha Pallewatta
 */

/**
 * Config properties
 * SIMULATION_MODE -> static
 * PR_PROCESSING_MODE -> PERIODIC
 * ENABLE_RESOURCE_DATA_SHARING -> false (not needed as FONs placed at the highest level.
 */
public class CrowdSensing_Microservices_RandomMobility_Clustering {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static Map<Integer, Integer> userMobilityPattern = new HashMap<Integer, Integer>();
    static LocationHandler locator;

    static double SENSOR_TRANSMISSION_TIME = 10;
    static int numberOfMobileUser = 1;

    // if random mobility generator for users is True, new random dataset will be created for each user
    static boolean randomMobility_generator = false; // To use random datasets
    static boolean renewDataset = false; // To overwrite existing random datasets
    static List<Integer> clusteringLevels = new ArrayList<Integer>(); // The selected fog layers for clustering

    //application
    static List<Application> applications = new ArrayList<>();
    static List<Pair<Double, Double>> qosValues = new ArrayList<>();

    public static void main(String[] args) {

        try {

            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            FogBroker broker = new FogBroker("broker");

            Application microservicesApplication = createApplication("example", broker.getId());
            applications.add(microservicesApplication);

            //
            DataParser dataObject = new DataParser();
            locator = new LocationHandler(dataObject);

            String datasetReference = References.dataset_reference;

            if (randomMobility_generator) {
                datasetReference = References.dataset_random;
                createRandomMobilityDatasets(References.random_walk_mobility_model, datasetReference, renewDataset);
            }

            /**
             * Clustered Fog node creation.
             * 01. Create devices (Client,FON,FCN,Cloud)
             * 02. Generate cluster connection.
             * 03. Identify devices monitored by each FON
             */
            createMobileUser(broker.getId(), applications.get(0).getAppId(), datasetReference);
            createFogDevices(broker.getId(), applications.get(0).getAppId());

            /**
             * Central controller for performing preprocessing functions
             */
            List<Application> appList = new ArrayList<>();
            for (Application application : applications)
                appList.add(application);


            List<Integer> clusterLevelIdentifier = new ArrayList<>();
            clusterLevelIdentifier.add(2);

            int placementAlgo = PlacementLogicFactory.CLUSTERED_MICROSERVICES_PLACEMENT;
            MicroservicesMobilityClusteringController microservicesController = new MicroservicesMobilityClusteringController("controller", fogDevices, sensors, appList, clusterLevelIdentifier, 2.0, placementAlgo, locator);


            // generate placement requests
            List<PlacementRequest> placementRequests = new ArrayList<>();
            for (Sensor s : sensors) {
                Map<String, Integer> placedMicroservicesMap = new HashMap<>();
                placedMicroservicesMap.put("sensorModule", s.getGatewayDeviceId());
                PlacementRequest p = new PlacementRequest(s.getAppId(), s.getGatewayDeviceId(), s.getGatewayDeviceId(), placedMicroservicesMap);
                placementRequests.add(p);
            }

            microservicesController.submitPlacementRequests(placementRequests, 0);


            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            Log.printLine("VRGame finished!");
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
    private static void createFogDevices(int userId, String appId) throws NumberFormatException, IOException {
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

    private static void createMobileUser(int userId, String appId, String datasetReference) throws IOException {

        for (int id = 1; id <= numberOfMobileUser; id++)
            userMobilityPattern.put(id, References.DIRECTIONAL_MOBILITY);

        locator.parseUserInfo(userMobilityPattern, datasetReference);

        List<String> mobileUserDataIds = locator.getMobileUserDataId();

        for (int i = 0; i < numberOfMobileUser; i++) {
            FogDevice mobile = addMobile("mobile_" + i, userId, appId, References.NOT_SET); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
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

    private static FogDevice addMobile(String name, int userId, String appId, int parentId) {
        FogDevice mobile = createFogDevice(name, 200, 2048, 10000, 270, 0, 87.53, 82.44, MicroserviceFogDevice.CLIENT);
        mobile.setParentId(parentId);
        //locator.setInitialLocation(name,drone.getId());
        Sensor mobileSensor = new Sensor("sensor-" + name, "M-SENSOR", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
        mobileSensor.setApp(applications.get(0));
        sensors.add(mobileSensor);

        mobileSensor.setGatewayDeviceId(mobile.getId());
        mobileSensor.setLatency(0.0);  // hIn this scenario sensor directly sends data. So we consider sensor_ mobileDevice to be a single device. (a mobile sensor)

        return mobile;
    }

    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        /*
         * Adding modules (vertices) to the application model (directed graph)
         */
        application.addAppModule("sensorModule", 10,150,100); // this transmits sensor data
        application.addAppModule("webFE", 512, 250,200);
        application.addAppModule("processingMservice", 512, 400,200);
        application.addAppModule("database", 512,150,2048);

        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */
        application.addAppEdge("M-SENSOR", "sensorModule", 0, 0, "M-SENSOR", Tuple.UP, AppEdge.SENSOR); // as sensor and mobile device are both same. this is a dummy edge.
        application.addAppEdge("sensorModule", "webFE", 2000, 500, "M-SENSOR-OUTPUT", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("webFE", "processingMservice", 3500, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("processingMservice", "database", 1000, 1000, "PROCESSED_DATA", Tuple.UP, AppEdge.MODULE);

        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("sensorModule", "M-SENSOR", "M-SENSOR-OUTPUT", new FractionalSelectivity(1.0));
        application.addTupleMapping("webFE", "M-SENSOR-OUTPUT", "RAW_DATA", new FractionalSelectivity(1.0));
        application.addTupleMapping("processingMservice", "RAW_DATA", "PROCESSED_DATA", new FractionalSelectivity(1.0));

      application.setSpecialPlacementInfo("database", "cloud");

        return application;
    }


}