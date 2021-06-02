package org.fog.entities.microservicesBased;

import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.utils.*;
import org.fog.utils.distribution.Distribution;

/**
 * Created by Samodha Pallewatta on 7/26/2020.
 */
public class SensorM extends Sensor {

    private int transmissionStartDelay = Config.TRANSMISSION_START_DELAY;

    public SensorM(String name, int userId, String appId, int gatewayDeviceId, double latency, Application application, GeoLocation geoLocation, Distribution transmitDistribution, int cpuLength, int nwLength, String tupleType, String destModuleName) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation, transmitDistribution, cpuLength, nwLength, tupleType, destModuleName);
        setApp(application);
    }

    public SensorM(String name, int userId, String appId, int gatewayDeviceId, double latency, Application application, GeoLocation geoLocation, Distribution transmitDistribution, String tupleType) {
        super(name, userId, appId, gatewayDeviceId, latency, geoLocation, transmitDistribution, tupleType);
        setApp(application);
    }

    public SensorM(String name, String tupleType, int userId, String appId, Application application, Distribution transmitDistribution) {
        super(name, tupleType, userId, appId, transmitDistribution);
        setApp(application);
    }

    @Override
    public void startEntity() {
        send(getGatewayDeviceId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.SENSOR_JOINED, getGeoLocation());
        send(getId(), getTransmitDistribution().getNextValue()+transmissionStartDelay, FogEvents.EMIT_TUPLE);
    }

    public void transmit() {
        AppEdge _edge = null;
        for (AppEdge edge : getApp().getEdges()) {
            if (edge.getSource().equals(getTupleType()))
                _edge = edge;
        }
        long cpuLength = (long) _edge.getTupleCpuLength();
        long nwLength = (long) _edge.getTupleNwLength();

        TupleM tuple = new TupleM(getAppId(), FogUtils.generateTupleId(), Tuple.UP, cpuLength, 1, nwLength, getOutputSize(),
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
