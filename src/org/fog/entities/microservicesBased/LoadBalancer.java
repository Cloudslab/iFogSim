package org.fog.entities.microservicesBased;

/**
 * Created by Samodha Pallewatta
 */
public interface LoadBalancer {
    int getDeviceId(String microservice, ServiceDiscoveryInfo serviceDiscoveryInfo);
}
