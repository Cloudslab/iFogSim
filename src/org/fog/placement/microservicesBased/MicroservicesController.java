package org.fog.placement.microservicesBased;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.microservicesBased.*;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Samodha Pallewatta on 7/31/2020.
 */
public class MicroservicesController extends SimEntity {

    private List<FogDevice> fogDevices;
    private List<Sensor> sensors;
    private Map<String, Application> applications = new HashMap<>();
    private PlacementLogicFactory placementLogicFactory = new PlacementLogicFactory();
    private Map<PlacementRequest, Integer> placementRequestDelayMap = new HashMap<>();

    /**
     * @param name
     * @param fogDevices
     * @param sensors
     * @param applications
     */
    public MicroservicesController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Application> applications, int placementLogic, Map<Integer, List<FogDevice>> monitoredDevices) {
        super(name);
        this.fogDevices = fogDevices;
        this.sensors = sensors;
        for (Application app : applications) {
            this.applications.put(app.getAppId(), app);
        }

        initializeControllers(placementLogic, monitoredDevices);
        generateRoutingTable();
    }

    private void initializeControllers(int placementLogic, Map<Integer, List<FogDevice>> monitored) {
        for (FogDevice device : fogDevices) {
            LoadBalancer loadBalancer = new WRRLoadBalancer();
            ClusteredFogDevice cdevice = (ClusteredFogDevice) device;

            //responsible for placement decision making
            if (cdevice.getDeviceType().equals(ClusteredFogDevice.FON) || cdevice.getDeviceType().equals(ClusteredFogDevice.CLOUD)) {
                List<FogDevice> monitoredDevices = monitored.get(cdevice.getId());
                MicroservicePlacementLogic microservicePlacementLogic = placementLogicFactory.getPlacementLogic(placementLogic, cdevice.getId());
                cdevice.initializeController(loadBalancer, microservicePlacementLogic, getResourceInfo(monitoredDevices), applications, monitoredDevices);
            } else if (cdevice.getDeviceType().equals(ClusteredFogDevice.FCN) || cdevice.getDeviceType().equals(ClusteredFogDevice.CLIENT)) {
                cdevice.initializeController(loadBalancer);
            }
        }
    }

    private FogDevice getDevice(int id) {
        for (FogDevice f : fogDevices) {
            if (f.getId() == id)
                return f;
        }
        return null;
    }

    private void generateRoutingTable() {
        // <source device id>  ->  <dest device id,next device to route to>
        Map<Integer, Map<Integer, Integer>> routing = new HashMap<>();
        Map<String, Map<String, String>> routingString = new HashMap<>();
        int size = fogDevices.size();

        int[][] routingMatrix = new int[size][size];
        double[][] distanceMatrix = new double[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                routingMatrix[row][column] = -1;
                distanceMatrix[row][column] = -1;
            }
        }

        boolean change = true;
        boolean firstIteration = true;
        while (change || firstIteration) {
            change = false;
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    double dist = distanceMatrix[row][column];
                    FogDevice rFog = fogDevices.get(row);
                    FogDevice cFog = fogDevices.get(column);
                    if (firstIteration && dist < 0) {
                        if (row == column) {
                            dist = 0;
                        } else {
                            dist = directlyConnectedDist(rFog, cFog);
                        }
                        if (dist >= 0) {
                            change = true;
                            distanceMatrix[row][column] = dist;
                            distanceMatrix[column][row] = dist;

                            // directly connected
                            routingMatrix[row][column] = cFog.getId();
                            routingMatrix[column][row] = rFog.getId();
                        }
                    }
                    if (dist < 0) {
                        Pair<Double, Integer> result = indirectDist(row, column, size, distanceMatrix);
                        dist = result.getFirst();
                        int mid = result.getSecond();
                        if (dist >= 0) {
                            change = true;
                            distanceMatrix[row][column] = dist;
                            routingMatrix[row][column] = routingMatrix[row][mid];
                        }
                    }
                    if (dist > 0) {
                        Pair<Double, Integer> result = indirectDist(row, column, size, distanceMatrix);
                        double distNew = result.getFirst();
                        int mid = result.getSecond();
                        if (distNew < dist) {
                            change = true;
                            distanceMatrix[row][column] = distNew;
                            routingMatrix[row][column] = routingMatrix[row][mid];
                        }
                    }
                }
            }
            firstIteration = false;
        }

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int sourceId = fogDevices.get(row).getId();
                int destId = fogDevices.get(column).getId();
                if (routing.containsKey(sourceId)) {
                    routing.get(sourceId).put(destId, routingMatrix[row][column]);
                    routingString.get(fogDevices.get(row).getName()).put(fogDevices.get(column).getName(), getDevice(routingMatrix[row][column]).getName());
                } else {
                    Map<Integer, Integer> route = new HashMap<>();
                    route.put(destId, routingMatrix[row][column]);
                    routing.put(sourceId, route);

                    Map<String, String> routeS = new HashMap<>();
                    routeS.put(fogDevices.get(column).getName(), getDevice(routingMatrix[row][column]).getName());
                    routingString.put(fogDevices.get(row).getName(), routeS);
                }
            }
        }

        for (FogDevice f : fogDevices) {
            ((ClusteredFogDevice) f).addRoutingTable(routing.get(f.getId()));
        }

        System.out.println("Routing Table : ");
        for (String deviceName : routingString.keySet()) {
            System.out.println(deviceName + " : " + routingString.get(deviceName).toString());
        }
        System.out.println("\n");
    }

    private static Pair<Double, Integer> indirectDist(int row, int dest, int size, double[][] distanceMatrix) {
        double minDistFromDirectConn = distanceMatrix[row][dest];
        int midPoint = -1;
        for (int column = 0; column < size; column++) {
            if (distanceMatrix[row][column] >= 0 && distanceMatrix[column][dest] >= 0) {
                double totalDist = distanceMatrix[row][column] + distanceMatrix[column][dest];
                if (minDistFromDirectConn >= 0 && totalDist < minDistFromDirectConn) {
                    minDistFromDirectConn = totalDist;
                    midPoint = column;
                } else if (minDistFromDirectConn < 0) {
                    minDistFromDirectConn = totalDist;
                    midPoint = column;
                }
            }
        }
        return new Pair<>(minDistFromDirectConn, midPoint);
    }

    private static double directlyConnectedDist(FogDevice rFog, FogDevice cFog) {
        int parent = rFog.getParentId();
        List<Integer> children = rFog.getChildrenIds();
        List<Integer> cluster = ((ClusteredFogDevice) rFog).getClusterNodeIds();
        if (cFog.getId() == parent) {
            return rFog.getUplinkLatency();
        } else if (children != null && children.contains(cFog.getId())) {
            return rFog.getChildToLatencyMap().get(cFog.getId());
        } else if (cluster != null && cluster.contains(cFog.getId())) {
            return ((ClusteredFogDevice) rFog).getClusterNodeToLatencyMap().get(cFog.getId());
        }
        return -1;
    }

    public void startEntity() {
        if (Config.SIMULATION_MODE == "STATIC")
            initiatePlacementRequestProcessing();
        if (Config.SIMULATION_MODE == "DYNAMIC")
            initiatePlacementRequestProcessingDynamic();

        if(Config.ENABLE_RESOURCE_DATA_SHARING){
            shareResourceDataAmongClusterNodes();
        }

        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);

        send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
    }

    private void shareResourceDataAmongClusterNodes() {
        for(FogDevice f:fogDevices){
            if(((ClusteredFogDevice)f).isInCluster()){
                for(int deviceId:((ClusteredFogDevice)f).getClusterNodeIds()){
                    Pair<Integer,Map<String,Double>> resources = new Pair<>(f.getId(),((ClusteredFogDevice)f).getResourceAvailabilityOfDevice());
                    sendNow(deviceId,FogEvents.UPDATE_RESOURCE_INFO,resources);
                }
            }
        }
    }

    private void initiatePlacementRequestProcessingDynamic() {
        for (PlacementRequest p : placementRequestDelayMap.keySet()) {
            processPlacedModules(p);
            if (placementRequestDelayMap.get(p) == 0) {
                sendNow(p.getGatewayDeviceId(), FogEvents.TRANSMIT_PR, p);
            } else
                send(p.getGatewayDeviceId(), placementRequestDelayMap.get(p), FogEvents.TRANSMIT_PR, p);
        }
        if (Config.PR_PROCESSING_MODE == Config.PERIODIC) {
            for (FogDevice f : fogDevices) {
                if (((ClusteredFogDevice) f).getDeviceType() == ClusteredFogDevice.FON) {
                    sendNow(f.getId(), FogEvents.PROCESS_PRS);
                }
            }
        }
    }

    private void initiatePlacementRequestProcessing() {
        for (PlacementRequest p : placementRequestDelayMap.keySet()) {
            processPlacedModules(p);
            int fonId = ((ClusteredFogDevice) getDevice(p.getGatewayDeviceId())).getFonId();
            if (placementRequestDelayMap.get(p) == 0) {
                sendNow(fonId, FogEvents.RECEIVE_PR, p);
            } else
                send(fonId, placementRequestDelayMap.get(p), FogEvents.RECEIVE_PR, p);
        }
        if (Config.PR_PROCESSING_MODE == Config.PERIODIC) {
            for (FogDevice f : fogDevices) {
                if (((ClusteredFogDevice) f).getDeviceType() == ClusteredFogDevice.FON) {
                    sendNow(f.getId(), FogEvents.PROCESS_PRS);
                }
            }
        }
    }

    private void processPlacedModules(PlacementRequest p) {
        for (String placed : p.getMappedMicroservices().keySet()) {
            int deviceId = p.getMappedMicroservices().get(placed);
            Application application = applications.get(p.getApplicationId());
            sendNow(deviceId, FogEvents.ACTIVE_APP_UPDATE, application);
            sendNow(deviceId, FogEvents.APP_SUBMIT, application);
            sendNow(deviceId, FogEvents.LAUNCH_MODULE, new AppModule(application.getModuleByName(placed)));
        }
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.CONTROLLER_RESOURCE_MANAGE:
                manageResources();
                break;
            case FogEvents.STOP_SIMULATION:
                CloudSim.stopSimulation();
                printTimeDetails();
                printPowerDetails();
                printNetworkUsageDetails();
                printQoSDetails();
                System.exit(0);
                break;
        }

    }


    private void printQoSDetails() {
        System.out.println("=========================================");
        System.out.println("APPLICATION QOS SATISFACTION");
        System.out.println("=========================================");
        double success = 0;
        double total = 0;
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToLatencyQoSSuccessCount().keySet()) {
            success += TimeKeeper.getInstance().getLoopIdToLatencyQoSSuccessCount().get(loopId);
            total += TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loopId);
        }

        double successPercentage = success / total * 100;
        System.out.println("Makespan" + " ---> " + successPercentage);
    }

    @Override
    public void shutdownEntity() {
    }

    protected void manageResources() {
        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
    }

    private void printNetworkUsageDetails() {
        System.out.println("Total network usage = " + NetworkUsageMonitor.getNetworkUsage() / Config.MAX_SIMULATION_TIME);
    }

    private FogDevice getCloud() {
        for (FogDevice dev : fogDevices)
            if (dev.getName().equals("cloud"))
                return dev;
        return null;
    }

    private void printPowerDetails() {
        StringBuilder energyInfo = new StringBuilder();
        for (FogDevice fogDevice : fogDevices) {
            String energyPerDevice = fogDevice.getName() + " : Energy Consumed = " + fogDevice.getEnergyConsumption() + "\n";
            energyInfo.append(energyPerDevice);
        }
        System.out.println(energyInfo.toString());
    }

    private String getStringForLoopId(int loopId) {
        for (String appId : applications.keySet()) {
            Application app = applications.get(appId);
            for (AppLoop loop : app.getLoops()) {
                if (loop.getLoopId() == loopId)
                    return loop.getModules().toString();
            }
        }
        return null;
    }

    private void printTimeDetails() {
        System.out.println("=========================================");
        System.out.println("============== RESULTS ==================");
        System.out.println("=========================================");
        System.out.println("EXECUTION TIME : " + (Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime()));
        System.out.println("=========================================");
        System.out.println("APPLICATION LOOP DELAYS");
        System.out.println("=========================================");
        for (Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
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
            System.out.println(getStringForLoopId(loopId) + " ---> " + TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
        }
        System.out.println("=========================================");
        System.out.println("TUPLE CPU EXECUTION DELAY");
        System.out.println("=========================================");

        for (String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
            System.out.println(tupleType + " ---> " + TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
        }

        System.out.println("=========================================");
    }

    private Map<Integer, Map<String, Double>> getResourceInfo(List<FogDevice> fogDevices) {
        Map<Integer, Map<String, Double>> resources = new HashMap<>();
        for (FogDevice device : fogDevices) {
            Map<String, Double> perDevice = new HashMap<>();
            perDevice.put(ControllerComponent.CPU, (double) device.getHost().getTotalMips());
            perDevice.put(ControllerComponent.RAM, (double) device.getHost().getRam());
            perDevice.put(ControllerComponent.STORAGE, (double) device.getHost().getStorage());
            resources.put(device.getId(), perDevice);
        }
        return resources;
    }


    public void submitPlacementRequests(List<PlacementRequest> placementRequests, int delay) {
        for (PlacementRequest p : placementRequests) {
            placementRequestDelayMap.put(p, delay);
        }
    }
}
