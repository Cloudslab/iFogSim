package org.fog.placement;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.entities.MicroserviceFogDevice;
import org.fog.entities.PlacementRequest;
import org.fog.mobilitydata.References;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.MigrationDelayMonitor;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Samodha Pallewatta on 7/31/2020.
 */
public class MicroservicesMobilityClusteringController extends MicroservicesController {

    private LocationHandler locator;
    private Map<Integer, Integer> parentReference;


    protected Map<Integer, Map<String, PlacementRequest>> perClientDevicePrs = new HashMap<>();  // clientDevice -> <Application -> PR>

    /**
     * @param name
     * @param fogDevices
     * @param sensors
     * @param applications
     */
    public MicroservicesMobilityClusteringController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Application> applications, List<Integer> clusterLevel, Double clusterLatency, int placementLogic, LocationHandler locator) {
        super(name, fogDevices, sensors, applications, clusterLevel, clusterLatency, placementLogic);

        setLocator(locator);
        setParentReference(new HashMap<Integer, Integer>());

        super.init();

    }

    public MicroservicesMobilityClusteringController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Application> applications, List<Integer> clusterLevel, Double clusterLatency, int placementLogic, Map<Integer, List<FogDevice>> monitored, LocationHandler locator) {

        super(name, fogDevices, sensors, applications, clusterLevel, clusterLatency, placementLogic, monitored);

        setLocator(locator);
        setParentReference(new HashMap<Integer, Integer>());

        super.init(monitored);
    }

    @Override
    protected void init() {
        // kept empty as locator should be set before init functions.
    }

    @Override
    protected void init(Map<Integer, List<FogDevice>> monitored) {
        // kept empty as locator should be set before init functions.
    }

    private void setParentReference(HashMap<Integer, Integer> parentReference) {
        // TODO Auto-generated method stub
        this.parentReference = parentReference;
    }

    @Override
    public void startEntity() {
        if (Config.ENABLE_DYNAMIC_CLUSTERING)
            clusteringSubmit(clustering_levels);

        super.startEntity();

        sendNow(getId(), FogEvents.MOBILITY_SUBMIT);
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.MOBILITY_SUBMIT:
                processMobilityData();
                break;
            case FogEvents.MOBILITY_MANAGEMENT:
                processMobility(ev);
                break;
            case FogEvents.STOP_SIMULATION:
                CloudSim.stopSimulation();
                printTimeDetails();
                printPowerDetails();
                printCostDetails();
                printNetworkUsageDetails();
                printMigrationDelayDetails();
                System.exit(0);
                break;
            default:
                super.processEvent(ev);
                break;
        }
    }

    private void printMigrationDelayDetails() {
        // TODO Auto-generated method stub
        System.out.println("Total time required for module migration = " + MigrationDelayMonitor.getMigrationDelay());
    }

    @Override
    public void submitPlacementRequests(List<PlacementRequest> placementRequests, int delay) {
        for (PlacementRequest p : placementRequests) {
            placementRequestDelayMap.put(p, delay);

            int clientDeviceId = p.getGatewayDeviceId();
            String app = p.getApplicationId();
            if (perClientDevicePrs.containsKey(clientDeviceId)) {
                perClientDevicePrs.get(clientDeviceId).put(app, p);
            } else {
                Map<String, PlacementRequest> map = new HashMap<>();
                map.put(app, p);
                perClientDevicePrs.put(clientDeviceId, map);
            }
        }
    }

    @Override
    protected void connectWithLatencies() {
        for (String dataId : locator.getDataIdsLevelReferences().keySet()) {
            for (int instenceId : locator.getInstenceDataIdReferences().keySet()) {
                if (locator.getInstenceDataIdReferences().get(instenceId).equals(dataId)) {
                    FogDevice fogDevice = getFogDeviceById(instenceId);
                    if (locator.getDataIdsLevelReferences().get(dataId) == locator.getLevelID("User") && fogDevice.getParentId() == References.NOT_SET) {
                        int parentID = locator.determineParent(fogDevice.getId(), References.INIT_TIME);
                        parentReference.put(fogDevice.getId(), parentID);
                        fogDevice.setParentId(parentID);
                    } else
                        parentReference.put(fogDevice.getId(), fogDevice.getParentId());
                }
            }
        }


        FogDevice cloud = getCloud();
        parentReference.put(cloud.getId(), cloud.getParentId());

        for (FogDevice fogDevice : fogDevices) {
            FogDevice parent = getFogDeviceById(parentReference.get(fogDevice.getId()));
            if (parent == null)
                continue;
            double latency = fogDevice.getUplinkLatency();
            parent.getChildToLatencyMap().put(fogDevice.getId(), latency);
            parent.getChildrenIds().add(fogDevice.getId());
            System.out.println("Child " + fogDevice.getName() + "\t----->\tParent " + parent.getName());
        }
    }

    private void processMobility(SimEvent ev) {

        // TODO Auto-generated method stub
        FogDevice fogDevice = (FogDevice) ev.getData();
        FogDevice prevParent = getFogDeviceById(parentReference.get(fogDevice.getId()));
        FogDevice newParent = getFogDeviceById(locator.determineParent(fogDevice.getId(), CloudSim.clock()));
        System.out.println(CloudSim.clock() + " Starting Mobility Management for " + fogDevice.getName());
        parentReference.put(fogDevice.getId(), newParent.getId());
        Map<String, Integer> migratingModules = new HashMap<>(); // migrating module _> it's device (can be preParent or  device the same cluster
        setNewOrchestratorNode(fogDevice,newParent);

        if (prevParent.getId() != newParent.getId()) {
            //printFogDeviceChildren(newParent.getId());
            //printFogDeviceChildren(prevParent.getId());

            //common ancestor policy
            List<Integer> newParentPath = getPathsToCloud(newParent.getId());
            List<Integer> prevParentPath = getPathsToCloud(prevParent.getId());
            int commonAncestor = determineAncestor(newParentPath, prevParentPath);


            fogDevice.setParentId(newParent.getId());
            System.out.println("Child " + fogDevice.getName() + "\t----->\tParent " + newParent.getName());
            newParent.getChildToLatencyMap().put(fogDevice.getId(), fogDevice.getUplinkLatency());
            newParent.addChild(fogDevice.getId());
            prevParent.removeChild(fogDevice.getId());

            for (String applicationName : fogDevice.getActiveApplications()) {

                migratingModules = getModulesToMigrate(fogDevice, commonAncestor, applicationName);
                HashMap<String, Double> upDelays = new HashMap<>(); // per migrating module
                HashMap<String, Double> downDelays = new HashMap<>(); // per migrating module

                for (String moduleName : migratingModules.keySet()) {

                    double upDelay = getUpDelay(migratingModules.get(moduleName), commonAncestor, applications.get(applicationName).getModuleByName(moduleName));
                    double downDelay = getDownDelay(newParent.getId(), commonAncestor, applications.get(applicationName).getModuleByName(moduleName));
                    upDelays.put(moduleName, upDelay);
                    downDelays.put(moduleName, downDelay);
                    JSONObject jsonSend = new JSONObject();
                    jsonSend.put("module", applications.get(applicationName).getModuleByName(moduleName));
                    jsonSend.put("delay", upDelay);

                    JSONObject jsonReceive = new JSONObject();
                    jsonReceive.put("module", new AppModule(applications.get(applicationName).getModuleByName(moduleName)));
                    jsonReceive.put("delay", downDelay);
                    jsonReceive.put("application", applications.get(applicationName));

                    send(migratingModules.get(moduleName), upDelay, FogEvents.MODULE_SEND, jsonSend);
                    send(newParent.getId(), downDelay, FogEvents.MODULE_RECEIVE, jsonReceive);
                    System.out.println("Migrating " + moduleName + " from " + prevParent.getName() + " to " + newParent.getName());
                }

                serviceDiscoveryUpdate(fogDevice, migratingModules, applicationName, newParent.getId(), upDelays, downDelays);
                for (String moduleName : migratingModules.keySet()) {
                    //because modules are moved to next parent
                    perClientDevicePrs.get(fogDevice.getId()).get(applicationName).getPlacedMicroservices().put(moduleName, newParent.getId());
                }
            }

            // = get
            //printFogDeviceChildren(newParent.getId());
            //printFogDeviceChildren(prevParent.getId());
        }

        updateRoutingTable(fogDevice);

    }

    private void setNewOrchestratorNode(FogDevice fogDevice, FogDevice newParent) {
        int parentId = newParent.getId();
        while(parentId!=-1){
            if(((MicroserviceFogDevice)newParent).getDeviceType().equals(MicroserviceFogDevice.FON)){
                int currentFon = ((MicroserviceFogDevice)fogDevice).getFonId();
                if(currentFon!=parentId) {
                    ((MicroserviceFogDevice)getFogDeviceById(currentFon)).removeMonitoredDevice(fogDevice);
                    ((MicroserviceFogDevice) fogDevice).setFonID(parentId);
                    ((MicroserviceFogDevice)getFogDeviceById(parentId)).addMonitoredDevice(fogDevice);
                    System.out.println("Orchestrator Node for device : " + fogDevice.getId() + " updated to " + parentId);
                }
                break;
            }
            else{
                parentId =newParent.getParentId();
                if(parentId!=-1)
                    newParent = getFogDeviceById(parentId);
            }
        }
    }

    private void updateRoutingTable(FogDevice fogDevice) {

        for (FogDevice f : fogDevices) {
            if (f.getId() != fogDevice.getId()) {
                // for mobile device update all to parent
                ((MicroserviceFogDevice) fogDevice).updateRoutingTable(f.getId(), fogDevice.getParentId());

                ////for other update route to mobile based on route to parent
                int nextId = ((MicroserviceFogDevice) f).getRoutingTable().get(fogDevice.getParentId());
                if (f.getId() != nextId)
                    ((MicroserviceFogDevice) f).updateRoutingTable(fogDevice.getId(), nextId);
                else
                    ((MicroserviceFogDevice) f).updateRoutingTable(fogDevice.getId(), fogDevice.getId());
            }
        }
    }

    private void serviceDiscoveryUpdate(FogDevice fogDevice, Map<String, Integer> migratingModules, String applicationName, int newParent, HashMap<String, Double> upDelays, HashMap<String, Double> downDelays) {
        PlacementRequest pr = perClientDevicePrs.get(fogDevice.getId()).get(applicationName);

        for (String m : migratingModules.keySet()) {
            List<String> clientMs = getClientMicroservices(m, applicationName);
            for (String clientM : clientMs) {
                JSONObject serviceDiscoveryRemove = new JSONObject();
                serviceDiscoveryRemove.put("service data", new Pair<>(m, migratingModules.get(m)));
                serviceDiscoveryRemove.put("action", "REMOVE");
                send(pr.getPlacedMicroservices().get(clientM), downDelays.get(m), FogEvents.UPDATE_SERVICE_DISCOVERY, serviceDiscoveryRemove);
            }
        }

        for (String m : pr.getPlacedMicroservices().keySet()) {
            if (pr.getPlacedMicroservices().get(m) == fogDevice.getId()) {
                List<String> services = getServiceMicroservice(m, applicationName);
                for (String service : services) {
                    if (migratingModules.containsKey(service)) {
                        JSONObject serviceDiscoveryAdd = new JSONObject();
                        serviceDiscoveryAdd.put("service data", new Pair<>(service, newParent));
                        serviceDiscoveryAdd.put("action", "ADD");
                        send(fogDevice.getId(), upDelays.get(service), FogEvents.UPDATE_SERVICE_DISCOVERY, serviceDiscoveryAdd);
                    }
                }
            }
        }

        for (String m : migratingModules.keySet()) {
            List<String> services = getServiceMicroservice(m, applicationName);
            for (String service : services) {
                if (migratingModules.containsKey(service)) {
                    JSONObject serviceDiscoveryAdd = new JSONObject();
                    serviceDiscoveryAdd.put("service data", new Pair<>(service, newParent));
                    serviceDiscoveryAdd.put("action", "ADD");
                    send(newParent, upDelays.get(service), FogEvents.UPDATE_SERVICE_DISCOVERY, serviceDiscoveryAdd);
                } else {
                    int d = pr.getPlacedMicroservices().get(service);
                    JSONObject serviceDiscoveryAdd = new JSONObject();
                    serviceDiscoveryAdd.put("service data", new Pair<>(service, d));
                    serviceDiscoveryAdd.put("action", "ADD");
                    sendNow(newParent, FogEvents.UPDATE_SERVICE_DISCOVERY, serviceDiscoveryAdd);
                }
            }
        }
    }

    private List<String> getClientMicroservices(String m, String applicationName) {
        List<String> services = new ArrayList<>();
        Application app = applications.get(applicationName);
        for (AppEdge appEdge : app.getEdges()) {
            if (appEdge.getDestination().equals(m) && appEdge.getDirection() == Tuple.UP) {
                if (app.getModuleNames().contains(appEdge.getSource()))
                    services.add(appEdge.getSource());
            }
        }
        return services;
    }

    private List<String> getServiceMicroservice(String m, String applicationName) {
        List<String> services = new ArrayList<>();
        Application app = applications.get(applicationName);
        for (AppEdge appEdge : app.getEdges()) {
            if (appEdge.getSource().equals(m) && appEdge.getDirection() == Tuple.UP) {
                if (app.getModuleNames().contains(appEdge.getDestination()))
                    services.add(appEdge.getDestination());
            }
        }
        return services;
    }

    private Map<String, Integer> getModulesToMigrate(FogDevice mobileDevice, int commonAncestor, String applicationName) {
        Map<String, Integer> migratingModules = new HashMap<>();

        PlacementRequest pr = perClientDevicePrs.get(mobileDevice.getId()).get(applicationName);
        for (String microservice : pr.getPlacedMicroservices().keySet()) {
            int deviceid = pr.getPlacedMicroservices().get(microservice);
            if (deviceid != mobileDevice.getId() && beforeCommonAncestor(deviceid, commonAncestor)) {
                migratingModules.put(microservice, deviceid);
            }
        }

        return migratingModules;
    }

    private boolean beforeCommonAncestor(Integer deviceid, int commonAncestor) {
        FogDevice f = getFogDeviceById(deviceid);
        if (f.getId() == commonAncestor)
            return false;
        while (f.getParentId() != -1) {
            f = getFogDeviceById(f.getParentId());
            if (f.getId() == commonAncestor)
                return true;
        }
        return false;
    }

    private double getDownDelay(int deviceID, int commonAncestorID, AppModule module) {
        // TODO Auto-generated method stub
        double networkDelay = 0.0;
        while (deviceID != commonAncestorID) {
            networkDelay = networkDelay + module.getSize() / getFogDeviceById(deviceID).getDownlinkBandwidth();
            deviceID = getFogDeviceById(deviceID).getParentId();
        }
        return networkDelay;
    }

    private double getUpDelay(int deviceID, int commonAncestorID, AppModule module) {
        // TODO Auto-generated method stub
        double networkDelay = 0.0;
        while (deviceID != commonAncestorID) {
            networkDelay = networkDelay + module.getSize() / getFogDeviceById(deviceID).getUplinkBandwidth();
            deviceID = getFogDeviceById(deviceID).getParentId();
        }
        return networkDelay;
    }

    private int determineAncestor(List<Integer> newParentPath, List<Integer> prevParentPath) {
        // TODO Auto-generated method stub
        List<Integer> common = newParentPath.stream().filter(prevParentPath::contains).collect(Collectors.toList());
        return common.get(0);
    }

    private List<Integer> getPathsToCloud(int deviceID) {
        // TODO Auto-generated method stub
        List<Integer> path = new ArrayList<Integer>();
        while (!locator.isCloud(deviceID)) {
            path.add(deviceID);
            deviceID = getFogDeviceById(deviceID).getParentId();
        }
        path.add(getCloud().getId());
        return path;
    }

    private void processMobilityData() {
        // TODO Auto-generated method stub
        List<Double> timeSheet = new ArrayList<Double>();
        for (FogDevice fogDevice : fogDevices) {
            if (locator.isAMobileDevice(fogDevice.getId())) {
                timeSheet = locator.getTimeSheet(fogDevice.getId());
                for (double timeEntry : timeSheet)
                    send(getId(), timeEntry, FogEvents.MOBILITY_MANAGEMENT, fogDevice);
            }
        }
    }


    public void clusteringSubmit(List Levels) {
        System.out.println(CloudSim.clock() + " Start sending Clustering Request to Fog Devices in level: " + Levels);
        for (int i = 0; i < Levels.size(); i++) {
            int clusterLevel = (int) Levels.get(i);
            for (FogDevice fogDevice : fogDevices) {
                System.out.println(CloudSim.clock() + " fog Device: " + fogDevice.getName() + " with id: " + fogDevice.getId() + " is at level: " + fogDevice.getLevel());
                if ((int) fogDevice.getLevel() == clusterLevel) {
                    JSONObject jsonMessage = new JSONObject();
                    jsonMessage.put("locationsInfo", getLocator());
                    sendNow(fogDevice.getId(), FogEvents.START_DYNAMIC_CLUSTERING, jsonMessage);
                }
            }
        }
    }

    public LocationHandler getLocator() {
        return locator;
    }

    public void setLocator(LocationHandler locator) {
        this.locator = locator;
    }

}
