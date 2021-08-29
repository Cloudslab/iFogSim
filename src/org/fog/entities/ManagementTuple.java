package org.fog.entities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.fog.application.Application;
import org.fog.utils.ModuleLaunchConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by Samodha Pallewatta on 9/2/2019.
 */
public class ManagementTuple extends Tuple {

    // management tuples are routed by device id, so direction doesn't matter/
    public static final int NONE = -1;

    public static final int PLACEMENT_REQUEST = 1;
    public static final int SERVICE_DISCOVERY_INFO = 2;
    public static final int RESOURCE_UPDATE = 3;
    public static final int DEPLOYMENTREQUEST = 4;

    public int managementTupleType;
    protected PlacementRequest placementRequest;
    protected Pair<String, Integer> serviceDiscoveryInfor;
    protected Map<Application, List<ModuleLaunchConfig>> deployementSet;
    protected Pair<Integer, Map<String, Double>> resourceData;

    //todo check use of this
    public Double processingDelay = 0.0;

    //todo cloudlet data hard coded
    public ManagementTuple(String appId, int cloudletId, int direction, int tupleType) {
        super(appId, cloudletId, direction, 5, 1, 50, 50, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        managementTupleType = tupleType;
    }

    public ManagementTuple(int cloudletId, int direction, int tupleType) {
        super("Management Tuple", cloudletId, direction, 5, 1, 50, 50, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        managementTupleType = tupleType;
    }

    public void setData(PlacementRequest placementRequest) {
        this.placementRequest = placementRequest;
    }

    public PlacementRequest getPlacementRequest() {
        return placementRequest;
    }

    public void setServiceDiscoveryInfor(Pair<String, Integer> serviceDiscoveryInfor) {
        this.serviceDiscoveryInfor = serviceDiscoveryInfor;
    }

    public Pair<String, Integer> getServiceDiscoveryInfor() {
        return serviceDiscoveryInfor;
    }

    public void setDeployementSet(Map<Application, List<ModuleLaunchConfig>> deployementSet) {
        this.deployementSet = deployementSet;
    }

    public Map<Application, List<ModuleLaunchConfig>> getDeployementSet() {
        return deployementSet;
    }

    public Pair<Integer, Map<String, Double>> getResourceData() {
        return resourceData;
    }

    public void setResourceData(Pair<Integer, Map<String, Double>> resourceData) {
        this.resourceData = resourceData;
    }
}
