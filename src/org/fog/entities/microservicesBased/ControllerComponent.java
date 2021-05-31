package org.fog.entities.microservicesBased;

import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.microservicesBased.MicroservicePlacementLogic;
import org.fog.placement.microservicesBased.PlacementLogicOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Samodha Pallewatta on 8/29/2019.
 */
public class ControllerComponent {

    protected LoadBalancer loadBalancer;
    protected MicroservicePlacementLogic microservicePlacementLogic = null;
    protected ServiceDiscoveryInfo serviceDiscoveryInfo = new ServiceDiscoveryInfo();
    protected MicroservicePlacementInfo microservicePlacementInfo = new MicroservicePlacementInfo();

    protected int deviceId;

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }


    // Resource Availability Info
    /**
     * Resource Identifiers
     */
    public static final String RAM = "ram";
    public static final String CPU = "cpu";
    public static final String STORAGE = "storage";

    /**
     * DeviceID,<ResourceIdentifier,AvailableResourceAmount>
     */
    protected Map<Integer, Map<String, Double>> resourceAvailability = new HashMap<>();


    //Application Info
    private Map<String, Application> applicationInfo = new HashMap<>();

    //FOg Architecture Info
    private List<FogDevice> fogDeviceList;


    /**
     * For FON
     *
     * @param loadBalancer
     * @param mPlacement
     */
    public ControllerComponent(Integer deviceId, LoadBalancer loadBalancer, MicroservicePlacementLogic mPlacement,
                               Map<Integer, Map<String, Double>> resourceAvailability, Map<String, Application> applicationInfo, List<FogDevice> fogDevices) {
        this.fogDeviceList = fogDevices;
        this.loadBalancer = loadBalancer;
        this.applicationInfo = applicationInfo;
        this.microservicePlacementLogic = mPlacement;
        this.resourceAvailability = resourceAvailability;
        setDeviceId(deviceId);
    }

    /**
     * For FCN
     *
     * @param loadBalancer
     */
    public ControllerComponent(Integer deviceId, LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        setDeviceId(deviceId);
    }

    /**
     * 1. execute placement logic -> returns the placement mapping.
     * 2. deploy on devices.
     * 3. update service discovery.
     */
    public PlacementLogicOutput executeApplicationPlacementLogic(List<PlacementRequest> placementRequests) {
        if (microservicePlacementLogic != null) {
            PlacementLogicOutput placement = microservicePlacementLogic.run(fogDeviceList, applicationInfo, resourceAvailability, placementRequests);
            return placement;
        }

        return null;
    }

    public void addServiceDiscoveryInfo(String microserviceName,Integer deviceID) {
        this.serviceDiscoveryInfo.addServiceDIscoveryInfo(microserviceName, deviceID);
    }

    public int getDestinationDeviceId(String destModuleName) {
        return loadBalancer.getDeviceId(destModuleName, serviceDiscoveryInfo);
    }

    //todo
    public void updateMicroservicesPlacementInfo() {

    }

    public Application getApplicationPerId(String appID) {
        return applicationInfo.get(appID);
    }

    public Double getAvailableResource(int deviceID, String resourceIdentifier) {
        if (resourceAvailability.containsKey(deviceID))
            return resourceAvailability.get(deviceID).get(resourceIdentifier);
        else
            return null;
    }

    public void updateResources(int device, String resourceIdentifier, double remainingResourceAmount) {
        if (resourceAvailability.containsKey(device))
            resourceAvailability.get(device).put(resourceIdentifier, remainingResourceAmount);
        else {
            Map<String, Double> resources = new HashMap<>();
            resources.put(resourceIdentifier, remainingResourceAmount);
            resourceAvailability.put(device, resources);
        }
    }
}

class ServiceDiscoveryInfo {
    protected Map<String, List<Integer>> serviceDiscoveryInfo = new HashMap<>();

    public void addServiceDIscoveryInfo(String microservice, Integer device) {
        if (serviceDiscoveryInfo.containsKey(microservice)) {
            List<Integer> deviceList = serviceDiscoveryInfo.get(microservice);
            deviceList.add(device);
            serviceDiscoveryInfo.put(microservice, deviceList);
        } else {
            List<Integer> deviceList = new ArrayList<>();
            deviceList.add(device);
            serviceDiscoveryInfo.put(microservice, deviceList);
        }
    }

    public Map<String, List<Integer>> getServiceDiscoveryInfo() {
        return serviceDiscoveryInfo;
    }
}


//todo not handled yet
class MicroservicePlacementInfo {

    public static int To_BE_DEPLOYED = 1;
    public static int ALREADY_DEPLOYED = 2;
    /**
     * placedMicroservice -> instanceCOunt
     * for deployed microservices
     */
    protected Map<String, Integer> microserviceInstance = new HashMap<>();

    protected Map<String, Integer> microservicesToDeploy = new HashMap<>();

    public int addMicroserviceToDeploy(String microservcieName, int instanceCount) {
        if (microserviceInstance.containsKey(microservcieName)) {
            int count = microserviceInstance.get(microservcieName) + instanceCount;
            microserviceInstance.put(microservcieName, count);
            return ALREADY_DEPLOYED;
        } else if (microservicesToDeploy.containsKey(microservcieName)) {
            int count = microservicesToDeploy.get(microservcieName) + instanceCount;
            microservicesToDeploy.put(microservcieName, count);
        } else {
            microservicesToDeploy.put(microservcieName, instanceCount);
        }
        return To_BE_DEPLOYED;
    }

    public Map<String, Integer> getMicroservicesToDeploy() {
        return microservicesToDeploy;
    }

    public Map<String, Integer> getDeplyedMicroservices() {
        return microserviceInstance;
    }

    public void setDeplyedMicroservices(String microservice, Integer count) {
        microserviceInstance.put(microservice, count);
    }
}





