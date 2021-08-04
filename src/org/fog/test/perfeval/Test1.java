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
import org.fog.placement.MicroservicesController;
import org.fog.placement.PlacementLogicFactory;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

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
 * DYNAMIC_CLUSTERING -> false
 */
public class Test1 {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static boolean CLOUD = true;

    static int l3FogNodes = 1; // proxy server
    static Integer[] l2FogNodesPerL3 = new Integer[]{5};        // GW devices
    static Integer[] l1FogNodesPerL2 = new Integer[]{5, 3, 2, 3, 5};   // eg : client end devices ( mobiles )
    private static int l2Num = 0; // fog adding l1 nodes
    static Integer deviceNum = 0;

    // l2 devices can contain multiple resources.
    static boolean diffResource = true;
    static Integer[] cpus = new Integer[]{2500, 4000};
    static Integer[] ram = new Integer[]{2048, 4096};

    static double SENSOR_TRANSMISSION_TIME = 10;

    //cluster link latency 2ms
    static Double clusterLatency = 2.0;

    public static void main(String[] args) {

        Log.printLine("Starting ........... Service...");

        try {

            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "Service"; // identifier of the application

            FogBroker broker = new FogBroker("broker");

            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            /**
             * Clustered Fog node creation.
             * 01. Create devices (Client,FON,FCN,Cloud)
             * 02. Generate cluster connection.
             * 03. Identify devices monitored by each FON
             */
            createFogDevices(broker.getId(), application);

            List<Integer> clusterLevelIdentifier = new ArrayList<>();
            clusterLevelIdentifier.add(2);

            /**
             * Central controller for performing preprocessing functions
             */
            List<Application> appList = new ArrayList<>();
            appList.add(application);

            int placementAlgo = PlacementLogicFactory.CLUSTERED_MICROSERVICES_PLACEMENT;
            MicroservicesController microservicesController = new MicroservicesController("controller", fogDevices, sensors, appList, clusterLevelIdentifier, clusterLatency, placementAlgo);

            // generate placement requests
            List<PlacementRequest> placementRequests = new ArrayList<>();
            for (Sensor s : sensors) {
                Map<String, Integer> placedMicroservicesMap = new HashMap<>();
                placedMicroservicesMap.put("clientModule", s.getGatewayDeviceId());
                PlacementRequest p = new PlacementRequest(s.getAppId(), s.getId(), s.getGatewayDeviceId(), placedMicroservicesMap);
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

    /**
     * Creates the fog devices in the physical topology of the simulation.
     *
     * @param userId
     */
    private static void createFogDevices(int userId, Application application) {
        FogDevice cloud = createFogDevice("cloud", 80000000, 49152000, 100, 12500000, 0, 0.01, 16 * 103, 16 * 83.25, MicroserviceFogDevice.CLOUD); // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.setParentId(-1);

        for (int i = 0; i < l3FogNodes; i++) {
            FogDevice proxy = createFogDevice("proxy-server-" + i, 10000, 8192, 12500000, 1250000, 1, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FON); // creates the fog device Proxy Server (level=1)
            proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
            proxy.setUplinkLatency(150); // latency of connection from Proxy Server to the Cloud is 150 ms
            fogDevices.add(cloud);
            fogDevices.add(proxy);

            for (int j = 0; j < l2FogNodesPerL3[i]; j++) {
                FogDevice l2 = addL2Devices(j + "", userId, proxy.getId(), l2Num, application);
                l2Num++;
            }
        }
    }

    private static FogDevice addL2Devices(String id, int userId, int parentId, int parentPosition, Application application) {
        FogDevice dept;
        if (diffResource) {
            int pos = deviceNum % 2;
            dept = createFogDevice("L2-" + id, cpus[pos], ram[pos], 1250000, 18750, 2, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FCN);
            deviceNum = deviceNum + 1;
        } else {
            dept = createFogDevice("L2-" + id, 2800, 2048, 1250000, 18750, 2, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FCN);
        }
        fogDevices.add(dept);
        dept.setParentId(parentId);
        dept.setUplinkLatency(30); // latency of connection between gateways and proxy server is 4 ms
        for (int i = 0; i < l1FogNodesPerL2[parentPosition]; i++) {
            String mobileId = id + "-" + i;
            FogDevice mobile = addMobile(mobileId, userId, dept.getId(), application); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            mobile.setUplinkLatency(20); // latency of connection between the smartphone and proxy server is 4 ms
            fogDevices.add(mobile);
        }
        return dept;
    }

    private static FogDevice addMobile(String id, int userId, int parentId, Application application) {

        String appId = application.getAppId();

        FogDevice mobile = createFogDevice("m-" + id, 1000, 2048, 18750, 250, 3, 0, 87.53, 82.44, MicroserviceFogDevice.CLIENT);
        mobile.setParentId(parentId);

        Sensor eegSensor = new Sensor("s-" + id, "SENSOR", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
        eegSensor.setApp(application);
        sensors.add(eegSensor);

        Actuator display = new Actuator("a-" + id, userId, appId, "DISPLAY");
        actuators.add(display);

        eegSensor.setGatewayDeviceId(mobile.getId());
        eegSensor.setLatency(5.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms

        display.setGatewayDeviceId(mobile.getId());
        display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
        display.setApp(application);

        return mobile;
    }

    /**
     * Creates a vanilla fog device
     *
     * @param nodeName    name of the device to be used in simulation
     * @param mips        MIPS
     * @param ram         RAM
     * @param upBw        uplink bandwidth
     * @param downBw      downlink bandwidth
     * @param level       hierarchy level of the device
     * @param ratePerMips cost rate per MIPS used
     * @param busyPower
     * @param idlePower
     * @return
     */
    private static MicroserviceFogDevice createFogDevice(String nodeName, long mips,
                                                         int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower, String deviceType) {

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
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 1250000, 0, ratePerMips, deviceType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        return fogdevice;
    }


    @SuppressWarnings({"serial"})
    private static Application createApplication(String appId, int userId) {

        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        /*
         * Adding modules (vertices) to the application model (directed graph)
         */
        application.addAppModule("clientModule", 128, 150, 100);
        application.addAppModule("mService1", 512, 250, 200);
        application.addAppModule("mService2", 512, 300, 200);
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

        /*
         * Defining application loops to monitor the latency of.
         * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
         */
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

        application.setSpecialPlacementInfo("mService3", "cloud");
        if (CLOUD) {
            application.setSpecialPlacementInfo("mService1", "cloud");
            application.setSpecialPlacementInfo("mService2", "cloud");
        }
//        application.createDAG();

        return application;
    }


}