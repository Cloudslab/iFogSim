package org.fog.adaptive.models;

/**
 * Device type classification for mobility management
 * Paper 3: Adaptive Fog Computing Architecture
 *
 * @author Narek Naltakyan
 */
public enum DeviceType {
    /**
     * Static devices - do not change location
     * Examples: fixed sensors, smart home devices, infrastructure
     */
    STATIC,

    /**
     * Dynamic devices - change location during operation
     * Examples: vehicles, wearables, mobile sensors
     * Higher request frequency to location service
     */
    DYNAMIC,

    /**
     * Predictable devices - move on known paths
     * Examples: public transit, delivery vehicles
     * Can use predictive fog node assignment
     */
    PREDICTABLE
}
