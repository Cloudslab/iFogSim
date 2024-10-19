package org.fog.test.perfeval;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.*;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.*;
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.RandomMobilityGenerator;
import org.fog.mobilitydata.References;
import org.fog.placement.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.*;
import org.fog.utils.distribution.DeterministicDistribution;
import org.json.simple.parser.ParseException;
import org.fog.utils.FractionalSelectivity;

import java.io.IOException;
import java.util.*;

public class CardiovascularHealthMonitoringApplication {
    static List<FogDevice> fogDevices = new ArrayList<>();
    static List<Sensor> sensors = new ArrayList<>();
    static List<Actuator> actuators = new ArrayList<>();

    static Map<Integer, Integer> userMobilityPattern = new HashMap<>();
    static LocationHandler locator;

    static boolean CLOUD = false;

    static double SENSOR_TRANSMISSION_TIME = 10;
    static int numberOfMobileUser = 5;

    static Double clusterLatency = 2.0;

    static boolean randomMobility_generator = true;
    static boolean renewDataset = false;
    static List<Integer> clusteringLevels = new ArrayList<>();

    public static void main(String[] args) {
        Log.printLine("Starting Cardiovascular Health Monitoring Application...");

        try {
            Log.disable();
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "Cardiovascular Health Monitoring Application CHM)";
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

            List<Application> appList = new ArrayList<>();
            appList.add(application);

            int placementAlgo = PlacementLogicFactory.CLUSTERED_MICROSERVICES_PLACEMENT;
            MicroservicesMobilityClusteringController microservicesController = new MicroservicesMobilityClusteringController("controller", fogDevices, sensors, appList, clusterLevelIdentifier, clusterLatency, placementAlgo, locator);

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

            double totalDataTransferred = 0;
            for (FogDevice device : fogDevices) {
                totalDataTransferred += device.getTotalDataTransferred();
            }
            double simulationTime = CloudSim.clock();
            double throughput = totalDataTransferred / simulationTime;
            System.out.println("Network Throughput: " + throughput + " bytes/sec");

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

    private static void createFogDevices(int userId, Application app) throws NumberFormatException, IOException {
        locator.parseResourceInfo();

        if (locator.getLevelWiseResources(locator.getLevelID("Cloud")).size() == 1) {
            FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0.01, 16 * 103, 16 * 83.25, MicroserviceFogDevice.CLOUD);
            cloud.setParentId(References.NOT_SET);
            locator.linkDataWithInstance(cloud.getId(), locator.getLevelWiseResources(locator.getLevelID("Cloud")).get(0));
            cloud.setLevel(0);
            fogDevices.add(cloud);

            for (int i = 0; i < locator.getLevelWiseResources(locator.getLevelID("Proxy")).size(); i++) {
                FogDevice proxy = createFogDevice("proxy-server_" + i, 2800, 4000, 10000, 10000, 0.0, 107.339, 83.4333, MicroserviceFogDevice.FON);
                locator.linkDataWithInstance(proxy.getId(), locator.getLevelWiseResources(locator.getLevelID("Proxy")).get(i));
                proxy.setParentId(cloud.getId());
                proxy.setUplinkLatency(100);
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
            FogDevice mobile = addMobile("mobile_" + i, userId, app, References.NOT_SET);
            mobile.setUplinkLatency(2);
            locator.linkDataWithInstance(mobile.getId(), mobileUserDataIds.get(i));
            mobile.setLevel(3);
            fogDevices.add(mobile);
        }
    }

    private static MicroserviceFogDevice createFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, double ratePerMips, double busyPower, double idlePower, String deviceType) {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
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

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

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
        Sensor mobileSensor = new Sensor("s-" + name, "SENSOR", userId, app.getAppId(), new DeterministicDistribution(SENSOR_TRANSMISSION_TIME));
        mobileSensor.setApp(app);
        sensors.add(mobileSensor);
        Actuator mobileDisplay = new Actuator("a-" + name, userId, app.getAppId(), "DISPLAY");
        actuators.add(mobileDisplay);

        mobileSensor.setGatewayDeviceId(mobile.getId());
        mobileSensor.setLatency(6.0);

        mobileDisplay.setGatewayDeviceId(mobile.getId());
        mobileDisplay.setLatency(1.0);
        mobileDisplay.setApp(app);

        return mobile;
    }

    private static Application createApplication(String appId, int userId) {
        Application application = Application.createApplication(appId, userId);

        application.addAppModule("clientModule", 128, 150, 100);
        application.addAppModule("mService1", 512, 250, 200);
        application.addAppModule("mService2", 512, 350, 200);
        application.addAppModule("mService3", 2048, 450, 1000);

        application.addAppEdge("SENSOR", "clientModule", 1000, 500, "SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("clientModule", "mService1", 2000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mService1", "mService2", 2500, 500, "FILTERED_DATA1", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("mService1", "mService3", 4000, 500, "FILTERED_DATA2", Tuple.UP, AppEdge.MODULE);

        application.addAppEdge("mService2", "clientModule", 14, 500, "RESULT1", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("mService3", "clientModule", 28, 500, "RESULT2", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("clientModule", "DISPLAY", 14, 500, "RESULT1_DISPLAY", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addAppEdge("clientModule", "DISPLAY", 14, 500, "RESULT2_DISPLAY", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("clientModule", "SENSOR", "RAW_DATA", (SelectivityModel) new FractionalSelectivity(0.9));
        application.addTupleMapping("mService1", "RAW_DATA", "FILTERED_DATA1", (SelectivityModel) new FractionalSelectivity(1.0));
        application.addTupleMapping("mService1", "RAW_DATA", "FILTERED_DATA2", (SelectivityModel) new FractionalSelectivity(1.0));
        application.addTupleMapping("mService2", "FILTERED_DATA1", "RESULT1", (SelectivityModel) new FractionalSelectivity(1.0));
        application.addTupleMapping("mService3", "FILTERED_DATA2", "RESULT2", (SelectivityModel) new FractionalSelectivity(1.0));
        application.addTupleMapping("clientModule", "RESULT1", "RESULT1_DISPLAY", (SelectivityModel) new FractionalSelectivity(1.0));
        application.addTupleMapping("clientModule", "RESULT2", "RESULT2_DISPLAY", (SelectivityModel) new FractionalSelectivity(1.0));

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

        return application;
    }
}