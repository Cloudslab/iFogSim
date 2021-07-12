package org.fog.entities;

import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.utils.*;
import org.fog.utils.distribution.Distribution;

public class Sensor2 extends Sensor {

    private int transmissionStartDelay = Config.TRANSMISSION_START_DELAY;

    public Sensor2(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, Distribution transmitDistribution, int cpuLength, int nwLength, String tupleType, String destModuleName) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation, transmitDistribution, cpuLength, nwLength, tupleType, destModuleName);
    }

    public Sensor2(String name, int userId, String appId, int gatewayDeviceId, double latency, GeoLocation geoLocation, Distribution transmitDistribution, String tupleType) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation, transmitDistribution, tupleType);
    }

    public Sensor2(String name, String tupleType, int userId, String appId, Distribution transmitDistribution) {
        super(name, tupleType, userId, appId, transmitDistribution);
    }

    @Override
    public void startEntity() {
        send(getGatewayDeviceId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.SENSOR_JOINED, getGeoLocation());
        send(getId(), getTransmitDistribution().getNextValue() + transmissionStartDelay, FogEvents.EMIT_TUPLE);
    }

    public void transmit() {
        AppEdge _edge = null;
        for (AppEdge edge : getApp().getEdges()) {
            if (edge.getSource().equals(getTupleType()))
                _edge = edge;
        }
        long cpuLength = (long) _edge.getTupleCpuLength();
        long nwLength = (long) _edge.getTupleNwLength();

        Tuple2 tuple = new Tuple2(getAppId(), FogUtils.generateTupleId(), Tuple.UP, cpuLength, 1, nwLength, getOutputSize(),
                new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        tuple.setUserId(getUserId());
        tuple.setTupleType(getTupleType());

        tuple.setDestModuleName(_edge.getDestination());
        tuple.setSrcModuleName(getSensorName());
        Logger.debug(getName(), "Sending tuple with tupleId = " + tuple.getCloudletId());

        tuple.setDestinationDeviceId(getGatewayDeviceId());

        int actualTupleId = updateTimings(getSensorName(), tuple.getDestModuleName());
        tuple.setActualTupleId(actualTupleId);

        send(getGatewayDeviceId(), getLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
    }

    public void setTransmissionStartDelay(int transmissionStartDelay) {
        this.transmissionStartDelay = transmissionStartDelay;
    }

    public int getTransmissionStartDelay() {
        return transmissionStartDelay;
    }
}
