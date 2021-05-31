package org.fog.placement.microservicesBased;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.Tuple;
import org.fog.entities.microservicesBased.ClusteredFogDevice;
import org.fog.entities.microservicesBased.PlacementRequest;
import org.fog.utils.Logger;

import java.util.*;

/**
 * Created by Samodha Pallewatta on 5/27/2021.
 */
public class ClusteredMicroservicePlacementLogic implements MicroservicePlacementLogic {
    /**
     * Fog network related details
     */
    List<FogDevice> fogDevices; //fog devices considered by FON for placements of requests
    List<PlacementRequest> placementRequests; // requests to be processed
    protected Map<Integer, Map<String, Double>> resourceAvailability = new HashMap<>();
    private Map<String, Application> applicationInfo = new HashMap<>();

    int fonID;

    protected Map<Integer, Double> currentCpuLoad;
    protected Map<Integer, List<String>> currentModuleMap = new HashMap<>();
    protected Map<Integer, Map<String, Double>> currentModuleLoadMap = new HashMap<>();
    protected Map<Integer, Map<String, Integer>> currentModuleInstanceNum = new HashMap<>();


    public ClusteredMicroservicePlacementLogic(int fonID) {
        setFONId(fonID);
    }

    public void setFONId(int id) {
        fonID = id;
    }

    public int getFonID() {
        return fonID;
    }

    @Override
    public PlacementLogicOutput run(List<FogDevice> fogDevices, Map<String, Application> applicationInfo, Map<Integer, Map<String, Double>> resourceAvailability, List<PlacementRequest> pr) {
        this.fogDevices = fogDevices;
        this.placementRequests = pr;
        this.resourceAvailability = resourceAvailability;
        this.applicationInfo = applicationInfo;

        setCurrentCpuLoad(new HashMap<Integer, Double>());
        setCurrentModuleMap(new HashMap<>());
        for (FogDevice dev : fogDevices) {
            getCurrentCpuLoad().put(dev.getId(), 0.0);
            getCurrentModuleMap().put(dev.getId(), new ArrayList<>());
            currentModuleLoadMap.put(dev.getId(), new HashMap<String, Double>());
            currentModuleInstanceNum.put(dev.getId(), new HashMap<String, Integer>());
        }

        mapModules();
        PlacementLogicOutput placement = generatePlacementMap();
        postProcessing();
        return placement;
    }

    private PlacementLogicOutput generatePlacementMap() {
        Map<Integer, Map<String, Integer>> placement = new HashMap<>();
        for (PlacementRequest placementRequest : placementRequests) {
            placement.put(placementRequest.getPlacementRequestId(), placementRequest.getMappedMicroservices());
        }

        //todo it assumed that modules are not shared among applications.
        // <deviceid, < app, list of modules to deploy > this is to remove deploying same module more than once on a certain device.
        Map<Integer, Map<Application, List<String>>> perDevice = new HashMap<>();
        Map<Integer, List<Pair<String, Integer>>> serviceDiscoveryInfo = new HashMap<>();
        List<Integer> completedPrs = new ArrayList<>();
        if (placement != null) {
            for (int prID : placement.keySet()) {
                //retrieve application
                PlacementRequest placementRequest = null;
                for (PlacementRequest pr : placementRequests) {
                    if (pr.getPlacementRequestId() == prID)
                        placementRequest = pr;
                }
                Application application = applicationInfo.get(placementRequest.getApplicationId());
                for (String microserviceName : placement.get(prID).keySet()) {
                    int deviceID = placement.get(prID).get(microserviceName);

                    if (perDevice.containsKey(deviceID)) {
                        if (perDevice.get(deviceID).containsKey(application)) {
                            if (!perDevice.get(deviceID).get(application).contains(microserviceName))
                                perDevice.get(deviceID).get(application).add(microserviceName);
                        } else {
                            List<String> microservices = new ArrayList<>();
                            microservices.add(microserviceName);
                            perDevice.get(deviceID).put(application, microservices);
                        }
                    } else {
                        Map<Application, List<String>> m = new HashMap<>();
                        List<String> microservices = new ArrayList<>();
                        microservices.add(microserviceName);
                        m.put(application, microservices);
                        perDevice.put(deviceID, m);
                    }

                    //service discovery info propagation
                    List<Integer> clientDevices = getClientServiceNodeIds(application, microserviceName, placementRequest.getMappedMicroservices(), placement.get(prID));
                    for (int clientDevice : clientDevices) {
                        if (serviceDiscoveryInfo.containsKey(clientDevice))
                            serviceDiscoveryInfo.get(clientDevice).add(new Pair<>(microserviceName, deviceID));
                        else {
                            List<Pair<String, Integer>> s = new ArrayList<>();
                            s.add(new Pair<>(microserviceName, deviceID));
                            serviceDiscoveryInfo.put(clientDevice, s);
                        }
                    }
                }
                completedPrs.add(placementRequest.getPlacementRequestId());
            }
        }

        return new PlacementLogicOutput(perDevice, serviceDiscoveryInfo, completedPrs);
    }

    public List<Integer> getClientServiceNodeIds(Application application, String
            microservice, Map<String, Integer> placed, Map<String, Integer> placementPerPr) {
        List<String> clientServices = getClientServices(application, microservice);
        List<Integer> nodeIDs = new LinkedList<>();
        for (String clientService : clientServices) {
            if (placed.get(clientService) != null)
                nodeIDs.add(placed.get(clientService));
            else if (placementPerPr.get(clientService) != null)
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

    @Override
    public void postProcessing() {

    }

    public void setCurrentCpuLoad(Map<Integer, Double> currentCpuLoad) {
        this.currentCpuLoad = currentCpuLoad;
    }

    public Map<Integer, List<String>> getCurrentModuleMap() {
        return currentModuleMap;
    }

    public void setCurrentModuleMap(Map<Integer, List<String>> currentModuleMap) {
        this.currentModuleMap = currentModuleMap;
    }

    public void mapModules() {
        //update based on initially placed microservice
        for (PlacementRequest pr : placementRequests) {
            List<String> placedModules = new ArrayList<String>();
            Application app = applicationInfo.get(pr.getApplicationId());
            for (String placed : pr.getMappedMicroservices().keySet()) {
                placedModules.add(placed);
                int deviceId = pr.getMappedMicroservices().get(placed);
                getCurrentCpuLoad().put(deviceId, getModule(placed, app).getMips() + getCurrentCpuLoad().get(deviceId));
                if (!currentModuleMap.get(deviceId).contains(placed))
                    currentModuleMap.get(deviceId).add(placed);

                if (!currentModuleLoadMap.get(deviceId).containsKey(placed))
                    currentModuleLoadMap.get(deviceId).put(placed, getModule(placed, app).getMips());
                else
                    currentModuleLoadMap.get(deviceId).put(placed, getModule(placed, app).getMips() + currentModuleLoadMap.get(deviceId).get(placed));

                if (!currentModuleInstanceNum.get(deviceId).containsKey(placed))
                    currentModuleInstanceNum.get(deviceId).put(placed, 1);
                else
                    currentModuleInstanceNum.get(deviceId).put(placed, currentModuleInstanceNum.get(deviceId).get(placed) + 1);

            }
        }

        Map<PlacementRequest, Integer> deviceToPlace = new HashMap<>();
        //initiate with the  parent of the client device for this
        for (PlacementRequest placementRequest : placementRequests) {
            deviceToPlace.put(placementRequest, getDevice(placementRequest.getGatewayDeviceId()).getParentId());
        }

        Map<PlacementRequest, Integer> clusterNode = new HashMap<>();

        Map<PlacementRequest, List<String>> toPlace = new HashMap<>();

        int placementCompleteCount = 0;
        while (placementCompleteCount < placementRequests.size()) {
            if (toPlace.isEmpty()) {
                for (PlacementRequest placementRequest : placementRequests) {
                    Application app = applicationInfo.get(placementRequest.getApplicationId());
                    List<String> modulesToPlace = getModulesToPlace(placementRequest.getMappedMicroservices().keySet(), app);
                    if (modulesToPlace.isEmpty())
                        placementCompleteCount++;
                    else
                        toPlace.put(placementRequest, modulesToPlace);
                }
            }
            for (PlacementRequest placementRequest : placementRequests) {
                Application app = applicationInfo.get(placementRequest.getApplicationId());
                int deviceId = deviceToPlace.get(placementRequest);
                // if not cluster
                if (deviceId != -1) {
                    FogDevice device = getDevice(deviceId);
                    List<String> placed = new ArrayList<>();
                    if (toPlace.containsKey(placementRequest)) {
                        for (String microservice : toPlace.get(placementRequest)) {
                            // try to place
                            if (getModule(microservice, app).getMips() + getCurrentCpuLoad().get(deviceId) <= device.getHost().getTotalMips()) {
                                Logger.debug("ModulePlacementEdgeward", "Placement of operator " + microservice + " on device " + device.getName() + " successful.");
                                getCurrentCpuLoad().put(deviceId, getModule(microservice, app).getMips() + getCurrentCpuLoad().get(deviceId));
                                System.out.println("Placement of operator " + microservice + " on device " + device.getName() + " successful.");

                                if (!currentModuleMap.get(deviceId).contains(microservice))
                                    currentModuleMap.get(deviceId).add(microservice);

                                placementRequest.getMappedMicroservices().put(microservice, deviceId);

                                //currentModuleLoad
                                if (!currentModuleLoadMap.get(deviceId).containsKey(microservice))
                                    currentModuleLoadMap.get(deviceId).put(microservice, getModule(microservice, app).getMips());
                                else
                                    currentModuleLoadMap.get(deviceId).put(microservice, getModule(microservice, app).getMips() + currentModuleLoadMap.get(deviceId).get(microservice));


                                //currentModuleInstance
                                if (!currentModuleInstanceNum.get(deviceId).containsKey(microservice))
                                    currentModuleInstanceNum.get(deviceId).put(microservice, 1);
                                else
                                    currentModuleInstanceNum.get(deviceId).put(microservice, currentModuleInstanceNum.get(deviceId).get(microservice) + 1);

                                placed.add(microservice);
                            }
                        }
                        for (String m : placed) {
                            toPlace.get(placementRequest).remove(m);
                        }
                        if (!toPlace.get(placementRequest).isEmpty()) {
                            if (((ClusteredFogDevice) device).isInCluster()) {
                                deviceToPlace.put(placementRequest, -1);
                                clusterNode.put(placementRequest, deviceId);
                            } else {
                                deviceToPlace.put(placementRequest, device.getParentId());
                            }
                        }
                        if (toPlace.get(placementRequest).isEmpty())
                            toPlace.remove(placementRequest);
                    }
                } else {
                    if (toPlace.containsKey(placementRequest)) {
                        int clusterDeviceId = clusterNode.get(placementRequest);
                        FogDevice device = getDevice(clusterDeviceId);
                        List<Integer> clusterDeviceIds = ((ClusteredFogDevice) device).getClusterNodeIds();
                        List<Integer> sortedClusterDevices = new ArrayList<>();
                        for (Integer id : clusterDeviceIds) {
                            //sort list from min to max
                            if (sortedClusterDevices.isEmpty())
                                sortedClusterDevices.add(id);
                            else {
                                boolean isPlaced =false;
                                for (int i = 0; i < sortedClusterDevices.size(); i++) {
                                    double sorted = resourceAvailability.get(sortedClusterDevices.get(i)).get("cpu") -
                                            getCurrentCpuLoad().get(sortedClusterDevices.get(i));
                                    double current = resourceAvailability.get(id).get("cpu") -
                                            getCurrentCpuLoad().get(id);
                                    if (sorted < current) {
                                        continue;
                                    } else {
                                        sortedClusterDevices.add(i, id);
                                        isPlaced =true;
                                        break;
                                    }
                                }
                                if(!isPlaced)
                                    sortedClusterDevices.add(id);
                            }

                        }

                        List<String> placed = new ArrayList<>();
                        for (String microservice : toPlace.get(placementRequest)) {
                            for (int id : sortedClusterDevices) {
                                // try to place
                                if (getModule(microservice, app).getMips() + getCurrentCpuLoad().get(id) <= device.getHost().getTotalMips()) {
                                    FogDevice placedDevice = getDevice(id);
                                    Logger.debug("ModulePlacementEdgeward", "Placement of operator " + microservice + " on device " + placedDevice.getName() + " successful.");
                                    getCurrentCpuLoad().put(id, getModule(microservice, app).getMips() + getCurrentCpuLoad().get(id));
                                    System.out.println("Placement of operator " + microservice + " on device " + placedDevice.getName() + " successful.");

                                    if (!currentModuleMap.get(id).contains(microservice))
                                        currentModuleMap.get(id).add(microservice);

                                    placementRequest.getMappedMicroservices().put(microservice, id);

                                    //currentModuleLoad
                                    if (!currentModuleLoadMap.get(id).containsKey(microservice))
                                        currentModuleLoadMap.get(id).put(microservice, getModule(microservice, app).getMips());
                                    else
                                        currentModuleLoadMap.get(id).put(microservice, getModule(microservice, app).getMips() + currentModuleLoadMap.get(id).get(microservice));


                                    //currentModuleInstance
                                    if (!currentModuleInstanceNum.get(id).containsKey(microservice))
                                        currentModuleInstanceNum.get(id).put(microservice, 1);
                                    else
                                        currentModuleInstanceNum.get(id).put(microservice, currentModuleInstanceNum.get(id).get(microservice) + 1);

                                    placed.add(microservice);
                                    break;
                                }
                            }
                        }

                        for (String m : placed) {
                            toPlace.get(placementRequest).remove(m);
                        }
                        if (!toPlace.get(placementRequest).isEmpty()) {
                            //check
                            deviceToPlace.put(placementRequest, device.getParentId());
                        }
                        if (toPlace.get(placementRequest).isEmpty())
                            toPlace.remove(placementRequest);
                    }
                }
            }
        }

    }

    public Map<Integer, Double> getCurrentCpuLoad() {
        return currentCpuLoad;
    }

    private AppModule getModule(String moduleName, Application app) {
        for (AppModule appModule : app.getModules()) {
            if (appModule.getName().equals(moduleName))
                return appModule;
        }
        return null;
    }

    private FogDevice getDevice(int deviceId) {
        for (FogDevice fogDevice : fogDevices) {
            if (fogDevice.getId() == deviceId)
                return fogDevice;
        }
        return null;
    }

    private List<String> getModulesToPlace(Set<String> placedModules, Application app) {
        List<String> modulesToPlace_1 = new ArrayList<String>();
        List<String> modulesToPlace = new ArrayList<String>();
        for (AppModule module : app.getModules()) {
            if (!placedModules.contains(module.getName()))
                modulesToPlace_1.add(module.getName());
        }
        /*
         * Filtering based on whether modules (to be placed) lower in physical topology are already placed
         */
        for (String moduleName : modulesToPlace_1) {
            boolean toBePlaced = true;

            for (AppEdge edge : app.getEdges()) {
                //CHECK IF OUTGOING DOWN EDGES ARE PLACED
                if (edge.getSource().equals(moduleName) && edge.getDirection() == Tuple.DOWN && !placedModules.contains(edge.getDestination()))
                    toBePlaced = false;
                //CHECK IF INCOMING UP EDGES ARE PLACED
                if (edge.getDestination().equals(moduleName) && edge.getDirection() == Tuple.UP && !placedModules.contains(edge.getSource()))
                    toBePlaced = false;
            }
            if (toBePlaced)
                modulesToPlace.add(moduleName);
        }

        return modulesToPlace;
    }


}
