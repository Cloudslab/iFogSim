package org.fog.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Samodha Pallewatta
 * Round Robin LoadBalancer
 */
public class RRLoadBalancer implements LoadBalancer {
    protected Map<String, Integer> loadBalancerPosition = new HashMap();

    public int getDeviceId(String microservice, ServiceDiscovery serviceDiscoveryInfo) {
        if (loadBalancerPosition.containsKey(microservice) && serviceDiscoveryInfo.getServiceDiscoveryInfo().containsKey(microservice)) {
            int pos = loadBalancerPosition.get(microservice);
            if (pos + 1 > serviceDiscoveryInfo.getServiceDiscoveryInfo().get(microservice).size() - 1)
                pos = 0;
            else
                pos = pos + 1;
            loadBalancerPosition.put(microservice, pos);
            return serviceDiscoveryInfo.getServiceDiscoveryInfo().get(microservice).get(pos);
        } else {
            if(serviceDiscoveryInfo.getServiceDiscoveryInfo().containsKey(microservice)) {
                loadBalancerPosition.put(microservice, 0);
                if (serviceDiscoveryInfo.getServiceDiscoveryInfo().get(microservice) == null)
                    System.out.println("null");
                int deviceId = serviceDiscoveryInfo.getServiceDiscoveryInfo().get(microservice).get(0);
                return deviceId;
            }
            System.out.println("Service Discovery Information Missing");
            return -1;
        }
    }
}
