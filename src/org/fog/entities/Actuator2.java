package org.fog.entities;

import org.fog.utils.GeoLocation;


public class Actuator2 extends Actuator {
    public Actuator2(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, String actuatorType, String srcModuleName) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation, actuatorType, srcModuleName);
    }
}
