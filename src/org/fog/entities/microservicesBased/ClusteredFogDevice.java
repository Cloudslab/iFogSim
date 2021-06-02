package org.fog.entities.microservicesBased;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Tuple;
import org.fog.placement.microservicesBased.MicroservicePlacementLogic;
import org.fog.placement.microservicesBased.PlacementLogicOutput;
import org.fog.utils.*;

import java.util.*;

/**
 * Created by Samodha Pallewatta
 */
public class ClusteredFogDevice extends FogDevice {

    // tuple and destination cluster device ID
    protected Queue<Pair<Tuple, Integer>> clusterTupleQueue;
    protected Map<Integer, Double> clusterNodetoLatencyMap;
    protected List<Integer> clusterNodeIds;

    /**
     * Flag denoting whether the link connecting to cluster from this FogDevice is busy
     */
    protected boolean isClusterLinkBusy;
    protected double clusterLinkBandwidth;
    protected boolean isInCluster = false;

    /**
     * Device type (1.client device 2.FCN 3.FON 4.Cloud)
     * in this work client device only holds the clientModule of the app and does not participate in processing and placement of microservices ( microservices can be shared among users,
     * thus for security resons client devices are not used for that)
     */
    protected String deviceType = null;
    public static final String CLIENT = "client";
    public static final String FCN = "fcn"; // fog computation node
    public static final String FON = "fon"; // fog orchestration node
    public static final String CLOUD = "cloud"; // cloud datacenter


    /**
     * closest FON id. If this device is a FON its own id is assigned
     */
    protected int fonID = -1;

    /**
     * used to forward tuples towards the destination device
     * map of <destinationID,nextDeviceID> based on shortest path.
     */
    protected Map<Integer, Integer> routingTable = new HashMap<>();


    protected ControllerComponent controllerComponent;

    protected List<PlacementRequest> placementRequests = new ArrayList<>();

    protected Map<Application, String> clientModulesToBeDeployed = new HashMap<>();

    public ClusteredFogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth, double clusterLinkBandwidth, double uplinkLatency, double ratePerMips, String deviceType) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);

        setClusterLinkBandwidth(clusterLinkBandwidth);
        clusterTupleQueue = new LinkedList<>();
        setClusterLinkBusy(false);
        setClusterNodeIds(new ArrayList<Integer>());
        setClusterNodeToLatency(new HashMap<>());

        setDeviceType(deviceType);

    }

    @Override
    protected void registerOtherEntity() {

        // for energy consumption update
        sendNow(getId(), FogEvents.RESOURCE_MGMT);

        // todo deploy client modules - change this.
//        if (getDeviceType().equals(ClusteredFogDevice.CLIENT)) {
//            for (Application app : clientModulesToBeDeployed.keySet()) {
//                sendNow(getId(), FogEvents.ACTIVE_APP_UPDATE, app);
//                sendNow(getId(), FogEvents.APP_SUBMIT, app);
//                sendNow(getId(), FogEvents.LAUNCH_MODULE, new AppModule(app.getModuleByName(clientModulesToBeDeployed.get(app))));
//            }
//        }
    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE:
                updateClusterTupleQueue();
                break;
            case FogEvents.PROCESS_PRS:
                processPlacementRequests();
                break;
            case FogEvents.RECEIVE_PR:
                addPlacementRequest((PlacementRequest) ev.getData());
                break;
            case FogEvents.UPDATE_SERVICE_DISCOVERY:
                updateServiceDiscovery(ev);
                break;
            case FogEvents.TRANSMIT_PR:
                transmitPR((PlacementRequest) ev.getData());
                break;
            case FogEvents.MANAGEMENT_TUPLE_ARRIVAL:
                processManagementTuple(ev);
                break;
            case FogEvents.UPDATE_RESOURCE_INFO:
                updateResourceInfo(ev);
                break;
            default:
                super.processOtherEvent(ev);
                break;
        }
    }

    private void updateResourceInfo(SimEvent ev) {
        Pair<Integer, Map<String, Double>> pair = (Pair<Integer, Map<String, Double>>) ev.getData();
        int deviceId = pair.getFirst();
        getControllerComponent().updateResourceInfo(deviceId, pair.getSecond());
    }

    public Map<String, Double> getResourceAvailabilityOfDevice() {
        return getControllerComponent().resourceAvailability.get(getId());
    }


    public void addPlacementRequest(PlacementRequest pr) {
        placementRequests.add(pr);
        if (Config.PR_PROCESSING_MODE == Config.SEQUENTIAL && placementRequests.size() == 1)
            sendNow(getId(), FogEvents.PROCESS_PRS);
    }


    public void setInCluster(boolean incluster) {
        isInCluster = incluster;
    }

    public boolean isInCluster() {
        return isInCluster;
    }


    private void updateClusterTupleQueue() {
        if (!getClusterTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getClusterTupleQueue().poll();
            sendThroughFreeClusterLink(pair.getFirst(), pair.getSecond());
        } else {
            setClusterLinkBusy(false);
        }
    }

    private void sendThroughFreeClusterLink(Tuple tuple, Integer clusterNodeID) {
        double networkDelay = tuple.getCloudletFileSize() / getClusterLinkBandwidth();
        setClusterLinkBusy(true);
        double latency = (getClusterNodeToLatencyMap()).get(clusterNodeID);
        send(getId(), networkDelay, FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE);

        if (tuple instanceof ManagementTuple) {
            send(clusterNodeID, networkDelay + latency + ((ManagementTuple) tuple).processingDelay, FogEvents.MANAGEMENT_TUPLE_ARRIVAL, tuple);
            //todo
//            if (Config.ENABLE_NETWORK_USAGE_AT_PLACEMENT)
//                NetworkUsageMonitor.sendingManagementTuple(latency, tuple.getCloudletFileSize());
        } else {
            send(clusterNodeID, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
            NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
        }
    }

    protected void sendToCluster(Tuple tuple, int clusterNodeID) {
        if (getClusterNodeIds().contains(clusterNodeID)) {
            if (!isClusterLinkBusy) {
                sendThroughFreeClusterLink(tuple, clusterNodeID);
            } else {
                clusterTupleQueue.add(new Pair<Tuple, Integer>(tuple, clusterNodeID));
            }
        }
    }

    public List<Integer> getClusterNodeIds() {
        return clusterNodeIds;
    }

    public double getClusterLinkBandwidth() {
        return clusterLinkBandwidth;
    }

    public void setClusterNodeToLatency(Map<Integer, Double> clusterNodeToLatency) {
        clusterNodetoLatencyMap = clusterNodeToLatency;
    }

    public Map<Integer, Double> getClusterNodeToLatencyMap() {
        return clusterNodetoLatencyMap;
    }

    public void setClusterNodeIds(List<Integer> nodeIds) {
        clusterNodeIds = nodeIds;
    }

    private void setClusterLinkBusy(boolean busy) {
        this.isClusterLinkBusy = busy;
    }

    private void setClusterLinkBandwidth(double clusterLinkBandwidth) {
        this.clusterLinkBandwidth = clusterLinkBandwidth;
    }

    public Queue<Pair<Tuple, Integer>> getClusterTupleQueue() {
        return clusterTupleQueue;
    }

    protected void setDeviceType(String deviceType) {
        if (deviceType.equals(ClusteredFogDevice.CLIENT) || deviceType.equals(ClusteredFogDevice.FCN) ||
                deviceType.equals(ClusteredFogDevice.FON) || deviceType.equals(ClusteredFogDevice.CLOUD))
            this.deviceType = deviceType;
        else
            Logger.error("Incompatible Device Type", "Device type not included in device type enums in ClusteredFogDevice class");
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void addRoutingTable(Map<Integer, Integer> routingTable) {
        this.routingTable = routingTable;
    }

    protected void processTupleArrival(SimEvent ev) {

        TupleM tuple = (TupleM) ev.getData();

        Logger.debug(getName(), "Received tuple " + tuple.getCloudletId() + "with tupleType = " + tuple.getTupleType() + "\t| Source : " +
                CloudSim.getEntityName(ev.getSource()) + "|Dest : " + CloudSim.getEntityName(ev.getDestination()));

        if (deviceType.equals(ClusteredFogDevice.CLOUD)) {
            updateCloudTraffic();
        }

        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);

        if (FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())) {
        }

        if (tuple.getDirection() == Tuple.ACTUATOR) {
            sendTupleToActuator(tuple);
            return;
        }

        if (getHost().getVmList().size() > 0) {
            final AppModule operator = (AppModule) getHost().getVmList().get(0);
            if (CloudSim.clock() > 0) {
                getHost().getVmScheduler().deallocatePesForVm(operator);
                getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add((double) getHost().getTotalMips());
                    }
                });
            }
        }

        if (deviceType.equals(ClusteredFogDevice.CLOUD) && tuple.getDestModuleName() == null) {
            sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
        }

        // these are resultant tuples and created periodic tuples
        if (tuple.getDestinationDeviceId() == -1) {
            // ACTUATOR tuples already handled above. Only UP and DOWN left
            if (tuple.getDirection() == Tuple.UP) {
                int destination = controllerComponent.getDestinationDeviceId(tuple.getDestModuleName());
                if (destination == -1) {
                    System.out.println("Service DiscoveryInfo missing. Tuple routing stopped");
                    return;
                }
                tuple.setDestinationDeviceId(destination);
                tuple.setSourceDeviceId(getId());
            } else if (tuple.getDirection() == Tuple.DOWN) {
                int destination = tuple.getDeviceForMicroservice(tuple.getDestModuleName());
                tuple.setDestinationDeviceId(destination);
                tuple.setSourceDeviceId(getId());
            }
        }

        if (tuple.getDestinationDeviceId() == getId()) {
            int vmId = -1;
            for (Vm vm : getHost().getVmList()) {
                if (((AppModule) vm).getName().equals(tuple.getDestModuleName()))
                    vmId = vm.getId();
            }
            if (vmId < 0
                    || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) &&
                    tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId)) {
                return;
            }
            tuple.setVmId(vmId);
            tuple.addToTraversedMicroservices(getId(), tuple.getDestModuleName());

            updateTimingsOnReceipt(tuple);

            executeTuple(ev, tuple.getDestModuleName());
        } else {
            if (tuple.getDestinationDeviceId() != -1) {
                int nextDeviceToSend = routingTable.get(tuple.getDestinationDeviceId());
                if (nextDeviceToSend == parentId)
                    sendUp(tuple);
                else if (childrenIds.contains(nextDeviceToSend))
                    sendDown(tuple, nextDeviceToSend);
                else if (clusterNodeIds.contains(nextDeviceToSend))
                    sendToCluster(tuple, nextDeviceToSend);
                else
                    Logger.error("Routing error", "Routing table of " + getName() + "does not contain next device for destination Id" + tuple.getDestinationDeviceId());
            } else {
                if (tuple.getDirection() == Tuple.DOWN) {
                    if (appToModulesMap.containsKey(tuple.getAppId())) {
                        if (appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())) {
                            int vmId = -1;
                            for (Vm vm : getHost().getVmList()) {
                                if (((AppModule) vm).getName().equals(tuple.getDestModuleName()))
                                    vmId = vm.getId();
                            }
                            if (vmId < 0
                                    || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) &&
                                    tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId)) {
                                return;
                            }
                            tuple.setVmId(vmId);
                            //Logger.error(getName(), "Executing tuple for operator " + moduleName);

                            updateTimingsOnReceipt(tuple);

                            executeTuple(ev, tuple.getDestModuleName());

                            return;
                        }
                    }


                    for (int childId : getChildrenIds())
                        sendDown(tuple, childId);

                } else {
                    Logger.error("Routing error", "Destination id -1 for UP tuple");
                }

            }
        }
    }

    /**
     * Both cloud and FON participates in placement process
     */
    public void initializeController(LoadBalancer loadBalancer, MicroservicePlacementLogic mPlacement, Map<Integer, Map<String, Double>> resourceAvailability, Map<String, Application> applications, List<FogDevice> fogDevices) {
        if (getDeviceType() == ClusteredFogDevice.FON || getDeviceType() == ClusteredFogDevice.CLOUD) {
            controllerComponent = new ControllerComponent(getId(), loadBalancer, mPlacement, resourceAvailability, applications, fogDevices);
        } else
            Logger.error("Controller init failed", "FON controller initialized for device " + getName() + " of type " + getDeviceType());
    }

    /**
     * FCN and Client devices
     */
    public void initializeController(LoadBalancer loadBalancer) {
        if (getDeviceType() != ClusteredFogDevice.FON) {
            controllerComponent = new ControllerComponent(getId(), loadBalancer);
            controllerComponent.updateResources(getId(), ControllerComponent.CPU, getHost().getTotalMips());
            controllerComponent.updateResources(getId(), ControllerComponent.RAM, getHost().getRam());
            controllerComponent.updateResources(getId(), ControllerComponent.STORAGE, getHost().getStorage());
        }
    }

    public ControllerComponent getControllerComponent() {
        return controllerComponent;
    }

    public List<PlacementRequest> getPlacementRequests() {
        return placementRequests;
    }

    public void setPlacementRequests(List<PlacementRequest> placementRequests) {
        this.placementRequests = placementRequests;
    }

    protected void processPlacementRequests() {
        System.out.println("Execute placement");
        if (Config.PR_PROCESSING_MODE == Config.PERIODIC && placementRequests.size() == 0) {
            send(getId(), Config.PLACEMENT_INTERVAL, FogEvents.PROCESS_PRS);
            return;
        }
        long startTime = System.nanoTime();

        List<PlacementRequest> placementRequests = new ArrayList<>();

        if (Config.PR_PROCESSING_MODE == Config.PERIODIC) {
            placementRequests.addAll(this.placementRequests);
            this.placementRequests.clear();
        } else if (Config.PR_PROCESSING_MODE == Config.SEQUENTIAL) {
            placementRequests.add(this.placementRequests.get(0));
            this.placementRequests.remove(0);
        }

        PlacementLogicOutput placementLogicOutput = getControllerComponent().executeApplicationPlacementLogic(placementRequests);
        long endTime = System.nanoTime();
        System.out.println("Placement Algorithm Completed. Time : " + (endTime - startTime) / 1e6);

        Map<Integer, Map<Application, List<ModuleLaunchConfig>>> perDevice = placementLogicOutput.getPerDevice();
        Map<Integer, List<Pair<String, Integer>>> serviceDicovery = placementLogicOutput.getServiceDiscoveryInfo();
        Map<PlacementRequest, Integer> placementRequestStatus = placementLogicOutput.getPrStatus();

        int fogDeviceCount = 0;
        StringBuilder placementString = new StringBuilder();
        for (int deviceID : perDevice.keySet()) {
            ClusteredFogDevice f = (ClusteredFogDevice) CloudSim.getEntity(deviceID);
            if (!f.getDeviceType().equals(ClusteredFogDevice.CLOUD))
                fogDeviceCount++;
            placementString.append(CloudSim.getEntity(deviceID).getName() + " : ");
            for (Application app : perDevice.get(deviceID).keySet()) {
                if (Config.SIMULATION_MODE == "STATIC") {
                    //ACTIVE_APP_UPDATE
                    sendNow(deviceID, FogEvents.ACTIVE_APP_UPDATE, app);
                    //APP_SUBMIT
                    sendNow(deviceID, FogEvents.APP_SUBMIT, app);
                    for (ModuleLaunchConfig moduleLaunchConfig : perDevice.get(deviceID).get(app)) {
                        String microserviceName = moduleLaunchConfig.getModule().getName();
                        placementString.append(microserviceName + " , ");
                        //LAUNCH_MODULE
                        sendNow(deviceID, FogEvents.LAUNCH_MODULE, new AppModule(app.getModuleByName(microserviceName)));
                        sendNow(deviceID, FogEvents.LAUNCH_MODULE_INSTANCE, moduleLaunchConfig);
                    }
                }
            }
            if (Config.SIMULATION_MODE == "DYNAMIC") {
                //todo
                transmitModulesToDeply(deviceID, perDevice.get(deviceID));
            }
            placementString.append("\n");
        }
        System.out.println(placementString.toString());
        for (int clientDevice : serviceDicovery.keySet()) {
            for (Pair serviceData : serviceDicovery.get(clientDevice)) {
                if (Config.SIMULATION_MODE == "DYNAMIC") {
                    transmitServiceDiscoveryData(clientDevice, serviceData);
                } else if (Config.SIMULATION_MODE == "STATIC")
                    sendNow(clientDevice, FogEvents.UPDATE_SERVICE_DISCOVERY, serviceData);
            }
        }

        for (PlacementRequest pr : placementRequestStatus.keySet()) {
            if (placementRequestStatus.get(pr) != -1) {
                if (Config.SIMULATION_MODE == "DYNAMIC")
                    transmitPR(pr, placementRequestStatus.get(pr));

                else if (Config.SIMULATION_MODE == "STATIC")
                    sendNow(placementRequestStatus.get(pr), FogEvents.RECEIVE_PR, pr);

            }
        }

        if (Config.PR_PROCESSING_MODE == Config.PERIODIC)
            send(getId(), Config.PLACEMENT_INTERVAL, FogEvents.PROCESS_PRS);
        else if (Config.PR_PROCESSING_MODE == Config.SEQUENTIAL && !this.placementRequests.isEmpty())
            sendNow(getId(), FogEvents.PROCESS_PRS);
    }

    public boolean setClientModulesToBeDeplyed(Application app, String moduleName) {
        if (!clientModulesToBeDeployed.keySet().contains(app)) {
            clientModulesToBeDeployed.put(app, moduleName);
            // only one client module per application
            return true;
        } else {
            Logger.error("Duplicate client module", "Client module for same app added already");
            return false;
        }
    }

    public List<Integer> getClientServiceNodeIds(Application application, String
            microservice, Map<String, Integer> placed, Map<String, Integer> placementPerPr) {
        List<String> clientServices = getClientServices(application, microservice);
        List<Integer> nodeIDs = new LinkedList<>();
        for (String clientService : clientServices) {
            if (placed.get(clientService) != null)
                nodeIDs.add(placed.get(clientService));
            else
                nodeIDs.add(placementPerPr.get(clientService));
        }

        return nodeIDs;

    }

    public List<String> getClientServices(Application application, String microservice) {
        List<String> clientServices = new LinkedList<>();

        for (AppEdge edge : application.getEdges()) {
            if (edge.getDestination().equals(microservice) && edge.getDirection() == Tuple.UP)
                clientServices.add(edge.getSource());
        }


        return clientServices;
    }

    protected void updateServiceDiscovery(SimEvent ev) {
        Pair<String, Integer> placement = (Pair) ev.getData();
        this.controllerComponent.addServiceDiscoveryInfo(placement.getFirst(), placement.getSecond());
    }

    protected void processModuleArrival(SimEvent ev) {
        // assumed that a new object of AppModule is sent
        //todo what if an existing module is sent again in another placement cycle -> vertical scaling instead of having two vms
        AppModule module = (AppModule) ev.getData();
        String appId = module.getAppId();
        if (!appToModulesMap.containsKey(appId)) {
            appToModulesMap.put(appId, new ArrayList<String>());
        }
        if (!appToModulesMap.get(appId).contains(module.getName())) {
            appToModulesMap.get(appId).add(module.getName());
            processVmCreate(ev, false);
            boolean result = getVmAllocationPolicy().allocateHostForVm(module);
            if (result) {
                getVmList().add(module);
                if (module.isBeingInstantiated()) {
                    module.setBeingInstantiated(false);
                }
                initializePeriodicTuples(module);
                module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
                        .getAllocatedMipsForVm(module));

                // if client module remove from map
                if (module.getName().equals("client" + appId))
                    clientModulesToBeDeployed.remove(applicationMap.get(appId));

                System.out.println("Module " + module.getName() + "created on " + getName() + " under Launch module");
                Logger.debug("Module deploy success", "Module " + module.getName() + " placement on " + getName() + " successful. vm id : " + module.getId());
            } else {
                Logger.error("Module deploy error", "Module " + module.getName() + " placement on " + getName() + " failed");
            }
        }
    }


    public void setFonID(int fonDeviceId) {
        fonID = fonDeviceId;
    }

    public int getFonId() {
        return fonID;
    }

    /**
     * Used by Client Devices to generate management tuple with pr and send it to FON
     *
     * @param placementRequest
     */
    private void transmitPR(PlacementRequest placementRequest) {
        transmitPR(placementRequest, fonID);
    }

    private void transmitPR(PlacementRequest placementRequest, Integer deviceId) {
        ManagementTuple prTuple = new ManagementTuple(placementRequest.getApplicationId(), FogUtils.generateTupleId(), ManagementTuple.NONE, ManagementTuple.PLACEMENT_REQUEST);
        prTuple.setData(placementRequest);
        prTuple.setDestinationDeviceId(deviceId);
        sendNow(getId(), FogEvents.MANAGEMENT_TUPLE_ARRIVAL, prTuple);
    }

    private void transmitServiceDiscoveryData(int clientDevice, Pair serviceData) {
        ManagementTuple sdTuple = new ManagementTuple(FogUtils.generateTupleId(), ManagementTuple.NONE, ManagementTuple.SERVICE_DISCOVERY_INFO);
        sdTuple.setServiceDiscoveryInfor(serviceData);
        sdTuple.setDestinationDeviceId(clientDevice);
        sendNow(getId(), FogEvents.MANAGEMENT_TUPLE_ARRIVAL, sdTuple);
    }

    private void transmitModulesToDeply(int deviceID, Map<Application, List<ModuleLaunchConfig>> applicationListMap) {
        ManagementTuple moduleTuple = new ManagementTuple(FogUtils.generateTupleId(), ManagementTuple.NONE, ManagementTuple.DEPLOYMENTREQUEST);
        moduleTuple.setDeployementSet(applicationListMap);
        moduleTuple.setDestinationDeviceId(deviceID);
        sendNow(getId(), FogEvents.MANAGEMENT_TUPLE_ARRIVAL, moduleTuple);
    }

    private void processManagementTuple(SimEvent ev) {
        ManagementTuple tuple = (ManagementTuple) ev.getData();
        if (tuple.getDestinationDeviceId() == getId()) {
            if (tuple.managementTupleType == ManagementTuple.PLACEMENT_REQUEST) {
                sendNow(getId(), FogEvents.RECEIVE_PR, tuple.getPlacementRequest());
            } else if (tuple.managementTupleType == ManagementTuple.SERVICE_DISCOVERY_INFO) {
                sendNow(getId(), FogEvents.UPDATE_SERVICE_DISCOVERY, tuple.getServiceDiscoveryInfor());
            } else if (tuple.managementTupleType == ManagementTuple.DEPLOYMENTREQUEST) {
                deployModules(tuple.getDeployementSet());
            } else if (tuple.managementTupleType == ManagementTuple.RESOURCE_UPDATE) {
                sendNow(getId(), FogEvents.UPDATE_RESOURCE_INFO, tuple.getResourceData());
            }
        } else if (tuple.getDestinationDeviceId() != -1) {
            int nextDeviceToSend = routingTable.get(tuple.getDestinationDeviceId());
            if (nextDeviceToSend == parentId)
                sendUp(tuple);
            else if (childrenIds.contains(nextDeviceToSend))
                sendDown(tuple, nextDeviceToSend);
            else if (clusterNodeIds.contains(nextDeviceToSend))
                sendToCluster(tuple, nextDeviceToSend);
            else
                Logger.error("Routing error", "Routing table of " + getName() + "does not contain next device for destination Id" + tuple.getDestinationDeviceId());
        } else
            Logger.error("Routing error", "Management tuple destination id is -1");
    }

    private void deployModules(Map<Application, List<ModuleLaunchConfig>> deployementSet) {
        for (Application app : deployementSet.keySet()) {
            //ACTIVE_APP_UPDATE
            sendNow(getId(), FogEvents.ACTIVE_APP_UPDATE, app);
            //APP_SUBMIT
            sendNow(getId(), FogEvents.APP_SUBMIT, app);
            for (ModuleLaunchConfig moduleLaunchConfig : deployementSet.get(app)) {
                String microserviceName = moduleLaunchConfig.getModule().getName();
                //LAUNCH_MODULE
                if (Config.SIMULATION_MODE == "STATIC") {
                    sendNow(getId(), FogEvents.LAUNCH_MODULE, new AppModule(app.getModuleByName(microserviceName)));
                } else if (Config.SIMULATION_MODE == "DYNAMIC") {
                    send(getId(), Config.MODULE_DEPLYMENT_TIME, FogEvents.LAUNCH_MODULE, new AppModule(app.getModuleByName(microserviceName)));
                }
                sendNow(getId(), FogEvents.LAUNCH_MODULE_INSTANCE, moduleLaunchConfig);
            }
        }
    }

    /**
     * Updating the number of modules of an application module on this device
     *
     * @param ev instance of SimEvent containing the module and no of instances
     */
    protected void updateModuleInstanceCount(SimEvent ev) {
        ModuleLaunchConfig config = (ModuleLaunchConfig) ev.getData();
        String appId = config.getModule().getAppId();
        String moduleName = config.getModule().getName();
        if (!moduleInstanceCount.containsKey(appId)) {
            Map<String, Integer> m = new HashMap<>();
            m.put(moduleName, config.getInstanceCount());
            moduleInstanceCount.put(appId, m);
        } else if (!moduleInstanceCount.get(appId).containsKey(moduleName)) {
            moduleInstanceCount.get(appId).put(moduleName, config.getInstanceCount());
        } else {
            int count = config.getInstanceCount() + moduleInstanceCount.get(appId).get(moduleName);
            moduleInstanceCount.get(appId).put(moduleName, count);
        }

        // in FONs resource availability is updated by placement algorithm
        if (getDeviceType() != FON) {
            double mips = getControllerComponent().getAvailableResource(getId(), ControllerComponent.CPU) - (config.getModule().getMips() * config.getInstanceCount());
            getControllerComponent().updateResources(getId(), ControllerComponent.CPU, mips);
            double ram = getControllerComponent().getAvailableResource(getId(), ControllerComponent.RAM) - (config.getModule().getRam() * config.getInstanceCount());
            getControllerComponent().updateResources(getId(), ControllerComponent.RAM, ram);
            double storage = getControllerComponent().getAvailableResource(getId(), ControllerComponent.STORAGE) - (config.getModule().getSize() * config.getInstanceCount());
            getControllerComponent().updateResources(getId(), ControllerComponent.STORAGE, storage);
        }
        if (isInCluster && Config.ENABLE_RESOURCE_DATA_SHARING) {
            for (Integer deviceId : clusterNodeIds) {
                ManagementTuple managementTuple = new ManagementTuple(FogUtils.generateTupleId(), ManagementTuple.NONE, ManagementTuple.RESOURCE_UPDATE);
                Pair<Integer, Map<String, Double>> data = new Pair<>(getId(), getControllerComponent().resourceAvailability.get(getId()));
                managementTuple.setResourceData(data);
                managementTuple.setDestinationDeviceId(deviceId);
                sendNow(getId(), FogEvents.MANAGEMENT_TUPLE_ARRIVAL, managementTuple);
            }
        }
    }

    protected void sendDownFreeLink(Tuple tuple, int childId) {
        if (tuple instanceof ManagementTuple) {
            double networkDelay = tuple.getCloudletFileSize() / getDownlinkBandwidth();
            setSouthLinkBusy(true);
            double latency = getChildToLatencyMap().get(childId);
            send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
            send(childId, networkDelay + latency + ((ManagementTuple) tuple).processingDelay, FogEvents.MANAGEMENT_TUPLE_ARRIVAL, tuple);
            //todo
//            if (Config.ENABLE_NETWORK_USAGE_AT_PLACEMENT)
//                NetworkUsageMonitor.sendingManagementTuple(latency, tuple.getCloudletFileSize());
        } else
            super.sendDownFreeLink(tuple, childId);
    }

    protected void sendUpFreeLink(Tuple tuple) {
        if (tuple instanceof ManagementTuple) {
            double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
            setNorthLinkBusy(true);
            send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
            send(parentId, networkDelay + getUplinkLatency() + ((ManagementTuple) tuple).processingDelay, FogEvents.MANAGEMENT_TUPLE_ARRIVAL, tuple);
            //todo
//            if (Config.ENABLE_NETWORK_USAGE_AT_PLACEMENT)
//                NetworkUsageMonitor.sendingManagementTuple(getUplinkLatency(), tuple.getCloudletFileSize());
        } else {
            super.sendUpFreeLink(tuple);
        }

    }

}
