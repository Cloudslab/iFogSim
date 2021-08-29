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
 * Created by Samodha Pallewatta on 6/1/2021.
 */
public class MicroservicesAppSample1 {

    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static boolean CLOUD = false;

    static int proxyServers = 2; // proxy server
    static Integer[] gatewayDevices = new Integer[]{3, 3};        // GW devices
    static Integer[] mobilesPerL2 = new Integer[]{3, 2, 1, 2, 3, 1};   // eg : client end devices ( mobiles )
    private static int l2Num = 0; // fog adding l1 nodes
    static Integer deviceNum = 0;

    // l2 devices can contain multiple resources.
    static boolean diffResource = true;
    static Integer[] cpus = new Integer[]{2800, 6000};
    static Integer[] ram = new Integer[]{2048, 4096};

    static double ECG_TRANSMISSION_TIME = 5;

    //cluster link latency 2ms
    static Double clusterLatency = 2.0;

    //application
    static List<Application> applications = new ArrayList<>();
    static int appCount = 1;
    static List<Pair<Double, Double>> qosValues = new ArrayList<>();
    static int appNum = 0;


    /**
     * Config properties
     * SIMULATION_MODE -> dynamic
     * PR_PROCESSING_MODE -> SEQUENTIAL
     * ENABLE_RESOURCE_DATA_SHARING -> true
     * DYNAMIC_CLUSTERING -> false
     */
    public static void main(String[] args) {

        try {
            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            FogBroker broker = new FogBroker("broker");

            /**
             * Microservices-based application creation - a single application is selected for this
             */
            String appId = "ECG_monitoring";
            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            applications.add(application);

            /**
             * Clustered Fog node creation.
             * 01. Create devices (Client,FON,FCN,Cloud)
             * 02. Generate cluster connection.
             * 03. Identify devices monitored by each FON - In this case each device contributes to placement by acting as a FON and setFON for each device
             */
            createFogDevices(broker.getId());

            List<Integer> clusterLevelIdentifier = new ArrayList<>();
            clusterLevelIdentifier.add(2);

            Map<Integer, List<FogDevice>> monitored = new HashMap<>();
            for (FogDevice f : fogDevices) {
                if (((MicroserviceFogDevice) f).getDeviceType() == MicroserviceFogDevice.FON || ((MicroserviceFogDevice) f).getDeviceType() == MicroserviceFogDevice.CLOUD) {
                    List<FogDevice> fogDevices = new ArrayList<>();
                    fogDevices.add(f);
                    monitored.put(f.getId(), fogDevices);
                    ((MicroserviceFogDevice) f).setFonID(f.getId());
                }
                if (((MicroserviceFogDevice) f).getDeviceType() == MicroserviceFogDevice.CLIENT) {
                    ((MicroserviceFogDevice) f).setFonID(f.getParentId());
                }
            }

            /**
             * Central controller for performing preprocessing functions
             */
            int placementAlgo = PlacementLogicFactory.DISTRIBUTED_MICROSERVICES_PLACEMENT;
            MicroservicesController microservicesController = new MicroservicesController("controller", fogDevices, sensors, applications, clusterLevelIdentifier, clusterLatency, placementAlgo, monitored);


            // generate placement requests
            List<PlacementRequest> placementRequests = new ArrayList<>();
            for (Sensor s : sensors) {
                Map<String, Integer> placedMicroservicesMap = new HashMap<>();
                placedMicroservicesMap.put("client", s.getGatewayDeviceId());
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
    private static void createFogDevices(int userId) {
        FogDevice cloud = createFogDevice("cloud", 80000000, 49152000, 100, 12500000, 0, 0.01, 16 * 103, 16 * 83.25, MicroserviceFogDevice.CLOUD); // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.setParentId(-1);

        for (int i = 0; i < proxyServers; i++) {
            FogDevice proxy = createFogDevice("proxy-server-" + i, 10000, 8192, 12500000, 1250000, 1, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FON); // creates the fog device Proxy Server (level=1)
            proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
            proxy.setUplinkLatency(150); // latency of connection from Proxy Server to the Cloud is 150 ms
            fogDevices.add(cloud);
            fogDevices.add(proxy);

            for (int j = 0; j < gatewayDevices[i]; j++) {
                FogDevice l2 = addL2Devices(j + "", userId, proxy.getId(), l2Num);
                l2Num++;
            }
        }
    }

    private static FogDevice addL2Devices(String id, int userId, int parentId, int parentPosition) {
        FogDevice dept;
        if (diffResource) {
            int pos = deviceNum % 2;
            dept = createFogDevice("L2-" + id, cpus[pos], ram[pos], 1250000, 18750, 2, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FON);
            deviceNum = deviceNum + 1;
        } else {
            dept = createFogDevice("L2-" + id, 2800, 2048, 1250000, 18750, 2, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FON);
        }
        fogDevices.add(dept);
        dept.setParentId(parentId);
        dept.setUplinkLatency(30); // latency of connection between gateways and proxy server is 4 ms
        for (int i = 0; i < mobilesPerL2[parentPosition]; i++) {
            String mobileId = id + "-" + i;
            FogDevice mobile = addMobile(mobileId, userId, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            mobile.setUplinkLatency(20); // latency of connection between the smartphone and proxy server is 4 ms
            fogDevices.add(mobile);
        }
        return dept;
    }

    private static FogDevice addMobile(String id, int userId, int parentId) {

        Application application = applications.get(0);
        String appId = application.getAppId();
        double throughput = 200;

        FogDevice mobile = createFogDevice("m-" + id, 1000, 2048, 18750, 250, 3, 0, 87.53, 82.44, MicroserviceFogDevice.CLIENT);
        mobile.setParentId(parentId);

        Sensor eegSensor = new Sensor("s-" + id, "ECG", userId, appId, new DeterministicDistribution(1000 / (throughput / 9 * 10))); // inter-transmission time of EEG sensor follows a deterministic distribution
        eegSensor.setApp(application);
        sensors.add(eegSensor);

        Actuator display = new Actuator("a-" + id, userId, appId, "DISPLAY");
        actuators.add(display);

        eegSensor.setGatewayDeviceId(mobile.getId());
        eegSensor.setLatency(5.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms

        display.setGatewayDeviceId(mobile.getId());
        display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
        display.setApp(application);

        appNum++;
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

    private static void createClusterConnections(String levelIdentifier, List<FogDevice> fogDevices, Double clusterLatency) {
        Map<Integer, List<FogDevice>> fogDevicesByParent = new HashMap<>();
        for (FogDevice fogDevice : fogDevices) {
            if (fogDevice.getName().startsWith(levelIdentifier)) {
                if (fogDevicesByParent.containsKey(fogDevice.getParentId())) {
                    fogDevicesByParent.get(fogDevice.getParentId()).add(fogDevice);
                } else {
                    List<FogDevice> sameParentList = new ArrayList<>();
                    sameParentList.add(fogDevice);
                    fogDevicesByParent.put(fogDevice.getParentId(), sameParentList);
                }
            }
        }

        for (int parentId : fogDevicesByParent.keySet()) {
            List<Integer> clusterNodeIds = new ArrayList<>();
            for (FogDevice fogdevice : fogDevicesByParent.get(parentId)) {
                clusterNodeIds.add(fogdevice.getId());
            }
            for (FogDevice fogDevice : fogDevicesByParent.get(parentId)) {
                List<Integer> clusterNodeIdsTemp = new ArrayList<>(clusterNodeIds);
                clusterNodeIds.remove((Object) fogDevice.getId());
                ((MicroserviceFogDevice) fogDevice).setClusterMembers(clusterNodeIds);
                Map<Integer, Double> latencyMap = new HashMap<>();
                for (int id : clusterNodeIds) {
                    latencyMap.put(id, clusterLatency);
                }
                ((MicroserviceFogDevice) fogDevice).setClusterMembersToLatencyMap(latencyMap);
                ((MicroserviceFogDevice) fogDevice).setIsInCluster(true);
                clusterNodeIds = clusterNodeIdsTemp;

            }
        }
    }

    private static void connectWithLatencies() {
        for (FogDevice fogDevice : fogDevices) {
            if (fogDevice.getParentId() >= 0) {
                FogDevice parent = (FogDevice) CloudSim.getEntity(fogDevice.getParentId());
                if (parent == null)
                    continue;
                double latency = fogDevice.getUplinkLatency();
                parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
                parent.getChildrenIds().add(fogDevice.getId());
            }
        }
    }

    private static Application createApplication(String appId, int userId) {

        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        /*
         * Adding modules (vertices) to the application model (directed graph)
         */
        application.addAppModule("client", 128, 605, 100); // adding module Client to the application model MB,MIPS,MB,kbps
        application.addAppModule("ECGFeature_Extractor", 256, 630, 200); // adding module Concentration Calculator to the application model
        application.addAppModule("ECG_Analyser", 512, 100, 2000); // adding module Connector to the application model

        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */
        if (ECG_TRANSMISSION_TIME == 10)
            application.addAppEdge("ECG", "client", 2000, 500, "ECG", Tuple.UP, AppEdge.SENSOR); // adding edge from EEG (sensor) to Client module carrying tuples of type EEG
        else
            application.addAppEdge("ECG", "client", 3000, 500, "ECG", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("client", "ECGFeature_Extractor", 3500, 500, "_SENSOR", Tuple.UP, AppEdge.MODULE); // adding edge from Client to Concentration Calculator module carrying tuples of type _SENSOR
        application.addAppEdge("ECGFeature_Extractor", "ECG_Analyser", 100, 10000, 1000, "ECG_FEATURES", Tuple.UP, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Concentration Calculator to Connector module carrying tuples of type PLAYER_GAME_STATE
        application.addAppEdge("ECGFeature_Extractor", "client", 14, 500, "ECG_FEATURE_ANALYSIS", Tuple.DOWN, AppEdge.MODULE);  // adding edge from Concentration Calculator to Client module carrying tuples of type CONCENTRATION
        application.addAppEdge("ECG_Analyser", "client", 100, 28, 1000, "LONG_TERM_ANALYSIS", Tuple.DOWN, AppEdge.MODULE); // adding periodic edge (period=1000ms) from Connector to Client module carrying tuples of type GLOBAL_GAME_STATE
        application.addAppEdge("client", "DISPLAY", 1000, 500, "EMERGENCY_NOTIFICATION", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type SELF_STATE_UPDATE
        application.addAppEdge("client", "DISPLAY", 1000, 500, "LONG_TERM_ANALYSIS_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);  // adding edge from Client module to Display (actuator) carrying tuples of type GLOBAL_STATE_UPDATE

        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("client", "ECG", "_SENSOR", new FractionalSelectivity(0.9)); // 0.9 tuples of type _SENSOR are emitted by Client module per incoming tuple of type EEG
        application.addTupleMapping("client", "ECG_FEATURE_ANALYSIS", "EMERGENCY_NOTIFICATION", new FractionalSelectivity(1.0)); // 1.0 tuples of type SELF_STATE_UPDATE are emitted by Client module per incoming tuple of type CONCENTRATION
        application.addTupleMapping("ECGFeature_Extractor", "_SENSOR", "ECG_FEATURE_ANALYSIS", new FractionalSelectivity(1.0)); // 1.0 tuples of type CONCENTRATION are emitted by Concentration Calculator module per incoming tuple of type _SENSOR
        application.addTupleMapping("client", "LONG_TERM_ANALYSIS", "LONG_TERM_ANALYSIS_UPDATE", new FractionalSelectivity(1.0)); // 1.0 tuples of type GLOBAL_STATE_UPDATE are emitted by Client module per incoming tuple of type GLOBAL_GAME_STATE

        /*
         * Defining application loops to monitor the latency of.
         * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
         */
        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("ECG");
            add("client");
            add("ECGFeature_Extractor");
            add("client");
            add("DISPLAY");
        }});
        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};
        application.setLoops(loops);

        //todo check why this is needed
//        application.setConnectedSensorInfo("ECG",ECG_TRANSMISSION_TIME);
        application.setSpecialPlacementInfo("ECG_Analyser", "cloud");
        if (CLOUD) {
            application.setSpecialPlacementInfo("ECGFeature_Extractor", "cloud");
        }
        application.createDAG();
        return application;
    }
}
