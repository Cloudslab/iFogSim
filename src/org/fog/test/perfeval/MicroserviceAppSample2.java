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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
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
 * DYNAMIC_CLUSTERING -> false
 */
public class MicroserviceAppSample2 {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static boolean CLOUD = false;

    static int l3FogNodes = 1; // proxy server
    static Integer[] l2FogNodesPerL3 = new Integer[]{6};        // GW devices
    static Integer[] l1FogNodesPerL2 = new Integer[]{3, 0, 0, 0, 0, 0};   // eg : client end devices ( mobiles )
    private static int l2Num = 0; // fog adding l1 nodes
    static Integer deviceNum = 0;

    // l2 devices can contain multiple resources.
    static boolean diffResource = true;
    static Integer[] cpus = new Integer[]{2800, 6000};
    static Integer[] ram = new Integer[]{2048, 4096};

    static double EEG_TRANSMISSION_TIME = 5;

    //cluster link latency 2ms
    static Double clusterLatency = 2.0;

    //application
    static List<Application> applications = new ArrayList<>();
    static int appCount = 1;
    static List<Pair<Double, Double>> qosValues = new ArrayList<>();
    static int appNum = 0;

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
            String fileName = "src/org/fog/test/perfeval/ApplicationConfig.json";
            applications = generateAppsFromFile(fileName);

            appNum = 0;

            /**
             * Clustered Fog node creation.
             * 01. Create devices (Client,FON,FCN,Cloud)
             * 02. Generate cluster connection.
             * 03. Identify devices monitored by each FON
             */
            createFogDevices(broker.getId());

            List<Integer> clusterLevelIdentifier = new ArrayList<>();
            clusterLevelIdentifier.add(2);

            /**
             * Central controller for performing preprocessing functions
             */
            List<Application> appList = new ArrayList<>();
            for (Application application : applications)
                appList.add(application);

            int placementAlgo = PlacementLogicFactory.CLUSTERED_MICROSERVICES_PLACEMENT;
            MicroservicesController microservicesController = new MicroservicesController("controller", fogDevices, sensors, appList, clusterLevelIdentifier, clusterLatency, placementAlgo);


            // generate placement requests
            List<PlacementRequest> placementRequests = new ArrayList<>();
            for (Sensor s : sensors) {
                Map<String, Integer> placedMicroservicesMap = new HashMap<>();
                placedMicroservicesMap.put("client" + s.getAppId(), s.getGatewayDeviceId());
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

    private static List<Application> generateAppsFromFile(String fileName) {
        List<Application> apps = new ArrayList<>();
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(fileName)) {
            //Read JSON file
            Object obj = jsonParser.parse(reader);

            JSONArray appParamList = (JSONArray) obj;

            for (int i = 0; i < appParamList.size(); i++) {
                apps.add(createApplication((JSONObject) appParamList.get(i)));
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return apps;
    }

    /**
     * Creates the fog devices in the physical topology of the simulation.
     *
     * @param userId
     */
    private static void createFogDevices(int userId) {
        FogDevice cloud = createFogDevice("cloud", 80000000, 49152000, 100, 12500000, 0, 0.01, 16 * 103, 16 * 83.25, MicroserviceFogDevice.CLOUD); // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.setParentId(-1);

        for (int i = 0; i < l3FogNodes; i++) {
            FogDevice proxy = createFogDevice("proxy-server-" + i, 10000, 8192, 12500000, 1250000, 1, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FON); // creates the fog device Proxy Server (level=1)
            proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
            proxy.setUplinkLatency(150); // latency of connection from Proxy Server to the Cloud is 150 ms
            fogDevices.add(cloud);
            fogDevices.add(proxy);

            for (int j = 0; j < l2FogNodesPerL3[i]; j++) {
                FogDevice l2 = addL2Devices(j + "", userId, proxy.getId(), l2Num);
                l2Num++;
            }
        }
    }

    private static FogDevice addL2Devices(String id, int userId, int parentId, int parentPosition) {
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
            FogDevice mobile = addMobile(mobileId, userId, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            mobile.setUplinkLatency(20); // latency of connection between the smartphone and proxy server is 4 ms
            fogDevices.add(mobile);
        }
        return dept;
    }

    private static FogDevice addMobile(String id, int userId, int parentId) {

        Application application = applications.get(appNum % appCount);
        String appId = application.getAppId();
        double throughput = 200;

        FogDevice mobile = createFogDevice("m-" + id, 1000, 2048, 18750, 250, 3, 0, 87.53, 82.44, MicroserviceFogDevice.CLIENT);
        mobile.setParentId(parentId);

        Sensor eegSensor = new Sensor("s-" + id, "sensor" + appId, userId, appId, new DeterministicDistribution(1000 / (throughput / 9 * 10))); // inter-transmission time of EEG sensor follows a deterministic distribution
        eegSensor.setApp(application);
        sensors.add(eegSensor);

        Actuator display = new Actuator("a-" + id, userId, appId, "actuator" + appId);
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


    @SuppressWarnings({"serial"})
    private static Application createApplication(JSONObject applicationParameters) {

        String appId = (String) applicationParameters.get("appId");
        int userId = Math.toIntExact((long) applicationParameters.get("userId"));
        Application application = Application.createApplication(appId, userId); // creates an empty application model (empty directed graph)

        String client = "client" + appId;
        String mService1 = "mService1" + appId;
        String mService2 = "mService2" + appId;
//        String mService3 = "mService3" + appId;
        String sensor = "sensor" + appId;
        String actuator = "actuator" + appId;

        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */
        application.addAppEdge(sensor, client, 1000, (Double) applicationParameters.get("nwLength"), sensor, Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge(client, mService1, (Double) applicationParameters.get("cpu_c_m1"), (Double) applicationParameters.get("nw_c_m1"), "c_m1" + appId, Tuple.UP, AppEdge.MODULE);
        application.addAppEdge(mService1, mService2, (Double) applicationParameters.get("cpu_m1_m2"), (Double) applicationParameters.get("nw_m1_m2"), "m1_m2" + appId, Tuple.UP, AppEdge.MODULE);
//        application.addAppEdge(mService1, mService3, (Double) applicationParameters.get("cpu_m1_m3"), (Double) applicationParameters.get("nw_m1_m3"), "m1_m3" + appId, Tuple.UP, AppEdge.MODULE);

        application.addAppEdge(mService2, client, 28, 200, "m2_c" + appId, Tuple.DOWN, AppEdge.MODULE);
//        application.addAppEdge(mService3, client, 28, 200, "m3_c" + appId, Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge(client, actuator, 28, 200, "a_m2c" + appId, Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge(client, actuator, 28, 200, "a_m3c" + appId, Tuple.DOWN, AppEdge.ACTUATOR);

        application.addAppModule(client, 128, Math.toIntExact((long) applicationParameters.get("client")), 100);
        application.addAppModule(mService1, 512, Math.toIntExact((long) applicationParameters.get("mService1")), 200);
        application.addAppModule(mService2, 512, Math.toIntExact((long) applicationParameters.get("mService2")), 200);
//        application.addAppModule(mService3, 512, Math.toIntExact((long) applicationParameters.get("mService3")), 200);


        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping(client, sensor, "c_m1" + appId, new FractionalSelectivity(0.9));
        application.addTupleMapping(client, "m2_c" + appId, "a_m2c" + appId, new FractionalSelectivity(1.0));
        application.addTupleMapping(client, "m3_c" + appId, "a_m3c" + appId, new FractionalSelectivity(1.0));

        application.addTupleMapping(mService1, "c_m1" + appId, "m1_m2" + appId, new FractionalSelectivity(1.0));
        application.addTupleMapping(mService1, "c_m1" + appId, "m1_m3" + appId, new FractionalSelectivity(1.0));

        application.addTupleMapping(mService2, "m1_m2" + appId, "m2_c" + appId, new FractionalSelectivity(1.0));

//        application.addTupleMapping(mService3, "m1_m3" + appId, "m3_c" + appId, new FractionalSelectivity(1.0));

        /*
         * Defining application loops to monitor the latency of.
         * Here, we add only one loop for monitoring : EEG(sensor) -> Client -> Concentration Calculator -> Client -> DISPLAY (actuator)
         */
        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add(sensor);
            add(client);
            add(mService1);
            add(mService2);
            add(client);
            add(actuator);
        }});

//        final AppLoop loop2 = new AppLoop(new ArrayList<String>() {{
//            add(client);
//            add(mService1);
//            add(mService3);
//            add(client);
//        }});

        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
//            add(loop2);
        }};
        application.setLoops(loops);

        return application;
    }


}