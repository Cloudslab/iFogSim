package org.fog.entities.microservicesBased;

import org.cloudbus.cloudsim.UtilizationModel;
import org.fog.entities.Tuple;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Samodha Pallewatta.
 * Tuple for microservices based applications.
 */
public class TupleM extends Tuple {

    private int destinationDeviceId;
    /**
     * keep track of traversed microservices by tuples of type UP
     * in microservices architecture UP -> tuple travelling towards service
     * DOWN -> tuple travelling from service to client microservice.
     */
    private Map<String, Integer> traversedMicroservices = new HashMap<>();

    public TupleM(String appId, int cloudletId, int direction, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(appId, cloudletId, direction, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        setDestinationDeviceId(-1);
    }

    public void setDestinationDeviceId(int deviceId) {
        destinationDeviceId = deviceId;
    }

    public int getDestinationDeviceId() {
        return destinationDeviceId;
    }

    public void addToTraversedMicroservices(Integer deviceID, String microserviceName) {
        traversedMicroservices.put(microserviceName, deviceID);
    }

    public int getDeviceForMicroservice(String microserviceName) {
         if(!traversedMicroservices.containsKey(microserviceName))
             return -1;
         else{
             return traversedMicroservices.get(microserviceName);
         }
    }

    public Map<String, Integer> getTraversed() {
        return traversedMicroservices;
    }

    public void setTraversedMicroservices(Map<String, Integer> traversed) {
        traversedMicroservices = traversed;
    }

}
