package org.fog.adaptive.mobility;

import org.fog.adaptive.models.DeviceType;
import org.fog.adaptive.models.GeographicArea;
import org.fog.entities.FogDevice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Location Service - Paper 3: Adaptive Fog Computing Architecture
 *
 * Manages device-to-fog mapping based on geographic location
 * Implements adaptive deployment (cloud vs fog based on density)
 *
 * @author Narek Naltakyan
 */
public class LocationService {

    // Adaptive deployment thresholds from Paper 3
    private static final int THRESHOLD_FOG_DEPLOYMENT = 100; // devices
    private static final int THRESHOLD_QUERY_FREQUENCY = 1000; // queries/minute

    private Map<String, DeviceLocation> deviceLocations;
    private Map<String, FogDevice> fogNodeMap;
    private Map<FogDevice, GeographicArea> fogNodeAreas;
    private boolean deployedInFog;
    private int queryCount;
    private long lastQueryReset;

    public LocationService() {
        this.deviceLocations = new ConcurrentHashMap<>();
        this.fogNodeMap = new ConcurrentHashMap<>();
        this.fogNodeAreas = new ConcurrentHashMap<>();
        this.deployedInFog = false;
        this.queryCount = 0;
        this.lastQueryReset = System.currentTimeMillis();
    }

    /**
     * Adaptive Service Deployment from Paper 3, Section 3.2
     * Migrates to fog when density increases
     */
    public void checkAdaptiveDeployment() {
        long currentTime = System.currentTimeMillis();
        long timeSinceReset = currentTime - lastQueryReset;

        // Calculate queries per minute
        double queriesPerMinute = (queryCount * 60000.0) / timeSinceReset;

        // Decision from Paper 3:
        // IF (client_density < threshold_fog AND query_frequency < threshold_freq):
        //     DEPLOY location service in cloud
        // ELSE:
        //     DEPLOY location service in fog infrastructure
        if (deviceLocations.size() < THRESHOLD_FOG_DEPLOYMENT &&
            queriesPerMinute < THRESHOLD_QUERY_FREQUENCY) {
            if (deployedInFog) {
                System.out.println("[LocationService] Migrating to cloud due to low density");
                deployedInFog = false;
            }
        } else {
            if (!deployedInFog) {
                System.out.println("[LocationService] Migrating to fog infrastructure");
                deployedInFog = true;
            }
        }

        // Reset query counter periodically
        if (timeSinceReset > 60000) { // 1 minute
            queryCount = 0;
            lastQueryReset = currentTime;
        }
    }

    /**
     * Find responsible fog node for a device location
     */
    public FogDevice findFogNode(double latitude, double longitude) {
        queryCount++;

        for (Map.Entry<FogDevice, GeographicArea> entry : fogNodeAreas.entrySet()) {
            if (entry.getValue().contains(latitude, longitude)) {
                return entry.getKey();
            }
        }

        // No fog node found - use default or cloud
        return null;
    }

    /**
     * Update device location
     */
    public void updateDeviceLocation(String deviceId, double lat, double lon,
                                    DeviceType type) {
        DeviceLocation location = deviceLocations.get(deviceId);

        if (location == null) {
            location = new DeviceLocation(deviceId, lat, lon, type);
            deviceLocations.put(deviceId, location);
        } else {
            location.updateLocation(lat, lon);
        }

        // Find and assign fog node
        FogDevice fogNode = findFogNode(lat, lon);
        if (fogNode != null) {
            fogNodeMap.put(deviceId, fogNode);
        }
    }

    /**
     * Register fog node with its coverage area
     */
    public void registerFogNode(FogDevice node, GeographicArea area) {
        fogNodeAreas.put(node, area);
        System.out.println(String.format(
            "[LocationService] Registered fog node %s with area %s",
            node.getName(), area));
    }

    /**
     * Update fog node coverage area (from DynamicAreaManager)
     */
    public void updateFogNodeArea(FogDevice node, GeographicArea newArea) {
        GeographicArea oldArea = fogNodeAreas.get(node);
        fogNodeAreas.put(node, newArea);

        // Reassign devices that are no longer in the area
        reassignDevicesForAreaChange(node, oldArea, newArea);
    }

    /**
     * Reassign devices when fog node area changes
     */
    private void reassignDevicesForAreaChange(FogDevice node,
                                             GeographicArea oldArea,
                                             GeographicArea newArea) {
        List<String> devicesToReassign = new ArrayList<>();

        for (Map.Entry<String, FogDevice> entry : fogNodeMap.entrySet()) {
            if (entry.getValue().equals(node)) {
                String deviceId = entry.getKey();
                DeviceLocation location = deviceLocations.get(deviceId);

                if (location != null) {
                    // Check if device is still in new area
                    if (!newArea.contains(location.getLatitude(), location.getLongitude())) {
                        devicesToReassign.add(deviceId);
                    }
                }
            }
        }

        // Reassign devices to new fog nodes
        for (String deviceId : devicesToReassign) {
            DeviceLocation location = deviceLocations.get(deviceId);
            FogDevice newFog = findFogNode(location.getLatitude(), location.getLongitude());
            if (newFog != null) {
                fogNodeMap.put(deviceId, newFog);
                System.out.println(String.format(
                    "[LocationService] Reassigned device %s from %s to %s",
                    deviceId, node.getName(), newFog.getName()));
            }
        }
    }

    /**
     * Get assignment for static device
     */
    public FogDevice getStaticDeviceAssignment(String deviceId) {
        return fogNodeMap.get(deviceId);
    }

    /**
     * Query frequency for dynamic devices (Paper 3)
     * Dynamic devices query more frequently based on movement speed
     */
    public int getQueryFrequency(DeviceType type, double speed) {
        switch (type) {
            case STATIC:
                return 0; // Static devices don't need to query
            case DYNAMIC:
                // Frequency increases with speed
                // speed in km/h, return queries per minute
                return (int) Math.min(60, Math.max(1, speed / 10));
            case PREDICTABLE:
                // Lower frequency due to prediction
                return (int) Math.max(1, speed / 20);
            default:
                return 1;
        }
    }

    public boolean isDeployedInFog() {
        return deployedInFog;
    }

    public int getDeviceCount() {
        return deviceLocations.size();
    }

    public int getQueryCount() {
        return queryCount;
    }

    /**
     * Device location tracking
     */
    private static class DeviceLocation {
        private String deviceId;
        private double latitude;
        private double longitude;
        private DeviceType type;
        private long lastUpdate;
        private List<LocationPoint> history;

        public DeviceLocation(String id, double lat, double lon, DeviceType type) {
            this.deviceId = id;
            this.latitude = lat;
            this.longitude = lon;
            this.type = type;
            this.lastUpdate = System.currentTimeMillis();
            this.history = new ArrayList<>();
            history.add(new LocationPoint(lat, lon, lastUpdate));
        }

        public void updateLocation(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
            this.lastUpdate = System.currentTimeMillis();
            history.add(new LocationPoint(lat, lon, lastUpdate));

            // Keep last 100 locations
            if (history.size() > 100) {
                history.remove(0);
            }
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public DeviceType getType() { return type; }
        public List<LocationPoint> getHistory() { return history; }
    }

    /**
     * Location history point
     */
    private static class LocationPoint {
        double lat;
        double lon;
        long timestamp;

        LocationPoint(double lat, double lon, long timestamp) {
            this.lat = lat;
            this.lon = lon;
            this.timestamp = timestamp;
        }
    }
}
