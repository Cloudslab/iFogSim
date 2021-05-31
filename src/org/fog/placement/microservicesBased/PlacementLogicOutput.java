package org.fog.placement.microservicesBased;

import org.apache.commons.math3.util.Pair;
import org.fog.application.Application;

import java.util.ArrayList;
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
    Map<Integer, Map<Application, List<String>>> perDevice = new HashMap<>();

    Map<Integer, List<Pair<String, Integer>>> serviceDiscoveryInfo = new HashMap<>();

    List<Integer> completedPrs = new ArrayList<>();

    public PlacementLogicOutput(Map<Integer, Map<Application, List<String>>> perDevice, Map<Integer, List<Pair<String, Integer>>> serviceDiscoveryInfo, List<Integer> completedPrs) {
        this.perDevice = perDevice;
        this.serviceDiscoveryInfo = serviceDiscoveryInfo;
        this.completedPrs = completedPrs;
    }

    public Map<Integer, List<Pair<String, Integer>>> getServiceDiscoveryInfo() {
        return serviceDiscoveryInfo;
    }

    public Map<Integer, Map<Application, List<String>>> getPerDevice() {
        return perDevice;
    }

    public List<Integer> getCompletedPrs() {
        return completedPrs;
    }
}
