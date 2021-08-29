package org.fog.placement;

import org.apache.commons.math3.util.Pair;
import org.fog.application.Application;
import org.fog.entities.PlacementRequest;
import org.fog.utils.ModuleLaunchConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Samodha Pallewatta on 9/12/2020.
 */
public class PlacementLogicOutput {

    // module placement info
    //todo it assumed that modules are not shared among applications.
    // <deviceid, < app, list of modules to deploy > this is to remove deploying same module more than once on a certain device.
    Map<Integer, Map<Application, List<ModuleLaunchConfig>>> perDevice = new HashMap<>();

    Map<Integer, List<Pair<String, Integer>>> serviceDiscoveryInfo = new HashMap<>();

    //Integer indicates next device to send the placement request (-1 for finished, or device id for others )
    Map<PlacementRequest,Integer> prStatus = new HashMap<>();

    public PlacementLogicOutput(Map<Integer, Map<Application, List<ModuleLaunchConfig>>> perDevice, Map<Integer, List<Pair<String, Integer>>> serviceDiscoveryInfo, Map<PlacementRequest,Integer> prStatus) {
        this.perDevice = perDevice;
        this.serviceDiscoveryInfo = serviceDiscoveryInfo;
        this.prStatus = prStatus;
    }

    public Map<Integer, List<Pair<String, Integer>>> getServiceDiscoveryInfo() {
        return serviceDiscoveryInfo;
    }

    public Map<Integer, Map<Application, List<ModuleLaunchConfig>>> getPerDevice() {
        return perDevice;
    }

    public Map<PlacementRequest,Integer> getPrStatus() {
        return prStatus;
    }
}
