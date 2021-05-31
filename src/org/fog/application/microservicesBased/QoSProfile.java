package org.fog.application.microservicesBased;

import org.fog.application.AppLoop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Samodha Pallewatta.
 * QoS profile for each app
 */
public class QoSProfile {
    private String appId;
    private List<AppLoop> loops = new ArrayList<>();

    /**
     * per service QoS parmeter ids
     */
    public static final int MAKE_SPAN = 1;  // defined in ms
    public static final int THORUGHPUT = 2; // service rate (per second)
    public static final int BUDGET = 3; // defined per 1M requests

    /**
     * loop id to qos map
     */
    private Map<Integer, Map<Integer, Double>> perServiceQoSParam = new HashMap<Integer, Map<Integer, Double>>();

    public QoSProfile(String appId) {
        this.appId = appId;
    }

    public void addQoSPerLoop(AppLoop loop, int qosParam, double qosValue) {
        if (perServiceQoSParam.containsKey(loop.getLoopId())) {
            perServiceQoSParam.get(loop.getLoopId()).put(qosParam, qosValue);
        } else {
            Map<Integer, Double> qosMap = new HashMap<>();
            qosMap.put(qosParam, qosValue);
            perServiceQoSParam.put(loop.getLoopId(), qosMap);

            loops.add(loop);
        }
    }

    public Map<Integer, Map<Integer, Double>> getQoSRequirementsPerService() {
        return perServiceQoSParam;
    }

}
