package org.fog.entities;

import org.fog.application.Application;
import org.fog.placement.MicroservicePlacementLogic;
import org.fog.placement.PlacementLogicOutput;

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
    protected ServiceDiscovery serviceDiscoveryInfo;

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
        serviceDiscoveryInfo = new ServiceDiscovery(deviceId);
    }

    /**
     * For FCN
     *
     * @param loadBalancer
     */
    public ControllerComponent(Integer deviceId, LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        setDeviceId(deviceId);
        serviceDiscoveryInfo = new ServiceDiscovery(deviceId);
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

    public void addServiceDiscoveryInfo(String microserviceName, Integer deviceID) {
        this.serviceDiscoveryInfo.addServiceDIscoveryInfo(microserviceName, deviceID);
        System.out.println("Service Discovery Info ADDED (device:" + this.deviceId + ") for microservice :" + microserviceName + " , destDevice : " + deviceID);
    }

    public int getDestinationDeviceId(String destModuleName) {
        return loadBalancer.getDeviceId(destModuleName, serviceDiscoveryInfo);
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

    public void updateResourceInfo(int deviceId, Map<String, Double> resources) {
        resourceAvailability.put(deviceId, resources);
    }

    public void removeServiceDiscoveryInfo(String microserviceName, Integer deviceID) {
        this.serviceDiscoveryInfo.removeServiceDIscoveryInfo(microserviceName, deviceID);
    }

    public void removeMonitoredDevice(FogDevice fogDevice) {
        this.fogDeviceList.remove(fogDevice);
    }

    public void addMonitoredDevice(FogDevice fogDevice) {
        this.fogDeviceList.add(fogDevice);
    }


}

class ServiceDiscovery {
    protected Map<String, List<Integer>> serviceDiscoveryInfo = new HashMap<>();
    int deviceId ;

    public ServiceDiscovery(Integer deviceId) {
        this.deviceId =deviceId;
    }

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

    public void removeServiceDIscoveryInfo(String microserviceName, Integer deviceID) {
        if (serviceDiscoveryInfo.containsKey(microserviceName) && serviceDiscoveryInfo.get(microserviceName).contains(new Integer(deviceID))) {
            System.out.println("Service Discovery Info REMOVED (device:" + this.deviceId + ") for microservice :" + microserviceName + " , destDevice : " + deviceID);
            serviceDiscoveryInfo.get(microserviceName).remove(new Integer(deviceID));
            if (serviceDiscoveryInfo.get(microserviceName).size() == 0)
                serviceDiscoveryInfo.remove(microserviceName);
        }
    }
}









