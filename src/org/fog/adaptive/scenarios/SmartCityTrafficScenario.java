package org.fog.adaptive.scenarios;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.adaptive.baas.BatchProcessor;
import org.fog.adaptive.baas.ProtocolConverter;
import org.fog.adaptive.federation.MasterNode;
import org.fog.adaptive.federation.MasterOfMasters;
import org.fog.adaptive.loadbalancing.DynamicAreaManager;
import org.fog.adaptive.loadbalancing.ThresholdScalingController;
import org.fog.adaptive.mobility.LocationService;
import org.fog.adaptive.models.*;
import org.fog.adaptive.resourcesharing.ResourceSharingManager;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.scheduler.StreamOperatorScheduler;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.fog.policy.AppModuleAllocationPolicy;

import java.util.*;
import java.util.LinkedList;

/**
 * Smart City Traffic Management Scenario
 * From Paper 2: Section "Smart City Traffic Management Application"
 *
 * Simulates:
 * - 400 intersection monitoring points
 * - Vehicle counts, speed measurements, traffic flow
 * - Expected results:
 *   * 78% bandwidth reduction
 *   * 15% improvement in intersection wait times
 *   * 99.8% reliability
 *   * 38% energy savings
 *
 * @author Narek Naltakyan
 */
public class SmartCityTrafficScenario {

    private static final int NUM_INTERSECTIONS = 400;
    private static final int NUM_FOG_CLUSTERS = 4;
    private static final int UPDATE_INTERVAL_SECONDS = 10;

    private List<FogDevice> fogNodes;
    private List<MasterNode> masterNodes;
    private MasterOfMasters globalMaster;
    private LocationService locationService;
    private ProtocolConverter protocolConverter;
    private Map<String, DynamicAreaManager> areaManagers;

    public SmartCityTrafficScenario() {
        this.fogNodes = new ArrayList<>();
        this.masterNodes = new ArrayList<>();
        this.areaManagers = new HashMap<>();
        this.locationService = new LocationService();
        this.protocolConverter = new ProtocolConverter();
    }

    /**
     * Main simulation entry point
     */
    public static void main(String[] args) {
        Log.printLine("========================================");
        Log.printLine("Smart City Traffic Management Scenario");
        Log.printLine("Paper 2: BaaS Framework Testing");
        Log.printLine("========================================");

        try {
            SmartCityTrafficScenario scenario = new SmartCityTrafficScenario();
            scenario.setupSimulation();
            scenario.runSimulation();
            scenario.printResults();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Setup simulation topology
     */
    private void setupSimulation() {
        Log.printLine("\n[Setup] Creating fog computing infrastructure...");

        // Initialize CloudSim
        int num_user = 1;
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false;

        CloudSim.init(num_user, calendar, trace_flag);

        // Create Master-of-Masters (Paper 6)
        globalMaster = new MasterOfMasters(true);
        MasterOfMasters slaveMaster = new MasterOfMasters(false);
        globalMaster.setSlaveMaster(slaveMaster);

        Log.printLine("[Setup] Created Master-of-Masters with fault tolerance");

        // Create fog clusters (Paper 4)
        for (int i = 0; i < NUM_FOG_CLUSTERS; i++) {
            createFogCluster(i);
        }

        // Create intersection sensors
        createIntersectionSensors();

        Log.printLine(String.format(
            "[Setup] Created %d fog clusters with %d intersection sensors",
            NUM_FOG_CLUSTERS, NUM_INTERSECTIONS));
    }

    /**
     * Create a fog cluster with master node
     */
    private void createFogCluster(int clusterId) {
        String clusterName = "Cluster_" + clusterId;

        // Create master node for cluster
        MasterNode master = new MasterNode(clusterName);
        masterNodes.add(master);
        globalMaster.registerRegionalMaster(master);

        // Create fog nodes in cluster
        int nodesPerCluster = 5;
        for (int i = 0; i < nodesPerCluster; i++) {
            FogDevice fogNode = createFogNode(clusterName + "_Fog_" + i,
                                            4000, 8192, 10000, 270);
            fogNodes.add(fogNode);
            master.addLocalNode(fogNode);

            // Create geographic area for this fog node (Paper 1)
            double baseLat = 40.0 + (clusterId * 0.1);
            double baseLon = -74.0 + (clusterId * 0.1);
            GeographicArea area = new GeographicArea(baseLat, baseLon, 5.0); // 5km radius

            DynamicAreaManager areaManager = new DynamicAreaManager(fogNode, area);
            areaManagers.put(fogNode.getName(), areaManager);

            locationService.registerFogNode(fogNode, area);
        }

        // Create sleeping node pool (Paper 6)
        List<MasterOfMasters.SleepingNode> sleepingPool = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            FogDevice sleepingNode = createFogNode(clusterName + "_Sleep_" + i,
                                                  4000, 8192, 10000, 270);
            sleepingPool.add(new MasterOfMasters.SleepingNode(sleepingNode));
        }
        globalMaster.registerSleepingNodePool(clusterName, sleepingPool);
    }

    /**
     * Create fog node following iFogSim pattern
     */
    private FogDevice createFogNode(String name, long mips, int ram,
                                    long storage, long bw) {
        try {
            // Create processing elements
            List<Pe> peList = new ArrayList<Pe>();
            peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

            int hostId = FogUtils.generateEntityId();
            int internalBw = 10000;

            // Create host with power model
            PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(internalBw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(107.339, 83.4333)
            );

            List<Host> hostList = new ArrayList<Host>();
            hostList.add(host);

            // Create characteristics
            String arch = "x86";
            String os = "Linux";
            String vmm = "Xen";
            double time_zone = 10.0;
            double cost = 3.0;
            double costPerMem = 0.05;
            double costPerStorage = 0.001;
            double costPerBw = 0.0;

            LinkedList<Storage> storageList = new LinkedList<Storage>();

            FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

            // Create fog device
            FogDevice fogDevice = new FogDevice(
                name,
                characteristics,
                new AppModuleAllocationPolicy(hostList),
                storageList,
                10,  // schedulingInterval
                bw,  // uplinkBandwidth
                bw,  // downlinkBandwidth
                0,   // uplinkLatency
                0.01 // ratePerMips
            );

            return fogDevice;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create intersection sensors
     */
    private void createIntersectionSensors() {
        Log.printLine("\n[Setup] Creating intersection monitoring sensors...");

        for (int i = 0; i < NUM_INTERSECTIONS; i++) {
            // Geographic distribution (Paper 2: 80% load in one region)
            double lat, lon;
            if (i < NUM_INTERSECTIONS * 0.8) {
                // Hotspot region (Cluster 0)
                lat = 40.0 + Math.random() * 0.05;
                lon = -74.0 + Math.random() * 0.05;
            } else {
                // Distributed in other regions
                int cluster = 1 + (i % 3);
                lat = 40.0 + (cluster * 0.1) + Math.random() * 0.05;
                lon = -74.0 + (cluster * 0.1) + Math.random() * 0.05;
            }

            locationService.updateDeviceLocation(
                "Intersection_" + i, lat, lon, DeviceType.STATIC);
        }
    }

    /**
     * Run simulation
     */
    private void runSimulation() {
        Log.printLine("\n[Simulation] Starting traffic monitoring simulation...");

        int simulationDuration = 7200; // 2 hours in seconds
        int currentTime = 0;

        while (currentTime < simulationDuration) {
            // Simulate traffic data collection
            simulateTrafficData(currentTime);

            // Update fog node loads
            updateFogNodeLoads(currentTime);

            // Manage sleeping nodes (Paper 6)
            for (MasterNode master : masterNodes) {
                LoadMetrics load = generateLoadMetrics();
                globalMaster.manageSleepingNodes(master.getClusterId(), load);
            }

            // Adaptive deployment check (Paper 3)
            locationService.checkAdaptiveDeployment();

            currentTime += UPDATE_INTERVAL_SECONDS;

            if (currentTime % 600 == 0) { // Log every 10 minutes
                Log.printLine(String.format("[Time %ds] Simulation progress...", currentTime));
            }
        }

        Log.printLine("\n[Simulation] Completed!");
    }

    /**
     * Simulate traffic data collection and processing
     */
    private void simulateTrafficData(int currentTime) {
        // Each intersection sends data every 10 seconds (Paper 2)
        for (int i = 0; i < NUM_INTERSECTIONS; i++) {
            // Create UDP packet
            byte[] data = generateTrafficData();
            ProtocolConverter.UdpPacket packet =
                new ProtocolConverter.UdpPacket("Intersection_" + i, data);

            // Convert to TCP batch
            ProtocolConverter.TcpBatch batch = protocolConverter.convertUdpToTcp(packet);

            if (batch != null) {
                // Process with differential privacy (Paper 2)
                BatchProcessor processor = new BatchProcessor();
                BatchProcessor.ProcessedBatch processed =
                    processor.processBatchWithPrivacy(batch);
            }
        }
    }

    /**
     * Generate simulated traffic data
     */
    private byte[] generateTrafficData() {
        // Simulate vehicle count and speed data
        Random rand = new Random();
        int vehicleCount = rand.nextInt(50);
        int avgSpeed = 20 + rand.nextInt(40);

        return String.format("V:%d,S:%d", vehicleCount, avgSpeed).getBytes();
    }

    /**
     * Update fog node loads and trigger adaptive mechanisms
     */
    private void updateFogNodeLoads(int currentTime) {
        for (FogDevice fog : fogNodes) {
            LoadMetrics load = generateLoadMetrics();

            // Dynamic area management (Paper 1)
            DynamicAreaManager areaManager = areaManagers.get(fog.getName());
            if (areaManager != null) {
                GeographicArea newArea = areaManager.adjustArea(load, 100);

                // Notify location service of area change
                locationService.updateFogNodeArea(fog, newArea);
            }

            // Threshold-based scaling (Paper 1)
            ThresholdScalingController scaler = new ThresholdScalingController(fog);
            ThresholdScalingController.ScalingAction action =
                scaler.makeScalingDecision(load, areaManager.getCurrentArea().isMinimumSize());

            if (action.getType() != ThresholdScalingController.ScalingType.NONE) {
                Log.printLine(String.format("[%ds] Scaling action: %s", currentTime, action));
            }
        }
    }

    /**
     * Generate simulated load metrics
     */
    private LoadMetrics generateLoadMetrics() {
        Random rand = new Random();
        LoadMetrics metrics = new LoadMetrics();

        // Simulate varying load levels
        metrics.setCpuUtilization(0.3 + rand.nextDouble() * 0.5);
        metrics.setMemoryUtilization(0.4 + rand.nextDouble() * 0.4);
        metrics.setQueueDepth(rand.nextDouble() * 0.6);
        metrics.setRequestRate(50 + rand.nextInt(150));

        return metrics;
    }

    /**
     * Print simulation results
     */
    private void printResults() {
        Log.printLine("\n========================================");
        Log.printLine("Simulation Results");
        Log.printLine("========================================");

        // Expected results from Paper 2:
        Log.printLine("\nExpected Results (from Paper 2):");
        Log.printLine("- Bandwidth reduction: 78%");
        Log.printLine("- Intersection wait time improvement: 15%");
        Log.printLine("- Reliability: 99.8%");
        Log.printLine("- Energy savings: 38%");
        Log.printLine("- Cache hit rate: 85%");

        Log.printLine("\nSimulation completed successfully!");
        Log.printLine("Check iFogSim logs for detailed metrics");
    }
}
