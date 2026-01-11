package org.fog.adaptive.loadbalancing;

import org.fog.adaptive.models.LoadMetrics;
import org.fog.entities.FogDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Threshold-Driven Scaling Controller - Paper 1
 *
 * Implements intelligent scaling decision algorithm (Algorithm 2)
 * Monitors load and triggers appropriate scaling actions
 *
 * @author Narek Naltakyan
 */
public class ThresholdScalingController {

    // Load thresholds for different actions
    private static final double THRESHOLD_NORMAL = 0.60;
    private static final double THRESHOLD_HIGH = 0.85;
    private static final double THRESHOLD_CRITICAL = 0.95;

    // Time window for sustained load detection (milliseconds)
    private static final long SUSTAINED_LOAD_WINDOW = 300000; // 5 minutes

    private FogDevice fogNode;
    private List<LoadMetrics> loadHistory;
    private long lastScalingTime;
    private boolean loadBalancerDeployed;
    private int additionalNodesCount;

    public ThresholdScalingController(FogDevice node) {
        this.fogNode = node;
        this.loadHistory = new ArrayList<>();
        this.lastScalingTime = 0;
        this.loadBalancerDeployed = false;
        this.additionalNodesCount = 0;
    }

    /**
     * Algorithm 2: ScalingDecision from Paper 1
     */
    public ScalingAction makeScalingDecision(LoadMetrics currentLoad, boolean areaAtMinimum) {
        // Record load in history
        loadHistory.add(currentLoad.copy());
        cleanOldHistory();

        double load = currentLoad.calculateLoad();

        // Check if load is sustained
        boolean sustainedHighLoad = isSustainedHighLoad();

        // Step 1: IF sustainedHighLoad
        if (sustainedHighLoad) {
            // Step 2: IF area == AREA_MIN AND loadBalancerAvailable()
            if (areaAtMinimum && loadBalancerAvailable()) {
                return new ScalingAction(ScalingType.DEPLOY_LOAD_BALANCER,
                    "Deploying load balancer and adding fog node");
            }
            // Step 3: ELSE IF neighborsAvailable
            else if (neighborsAvailable()) {
                return new ScalingAction(ScalingType.REQUEST_NEIGHBOR_ASSISTANCE,
                    "Requesting assistance from neighbor clusters");
            }
            // Step 4: ELSE initiate cloud offload
            else {
                return new ScalingAction(ScalingType.CLOUD_OFFLOAD,
                    "Offloading to cloud due to insufficient local capacity");
            }
        }

        // No scaling needed
        return new ScalingAction(ScalingType.NONE, "Load within normal range");
    }

    /**
     * Check if high load is sustained over time window
     */
    private boolean isSustainedHighLoad() {
        if (loadHistory.size() < 2) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - SUSTAINED_LOAD_WINDOW;

        int highLoadCount = 0;
        int totalCount = 0;

        for (LoadMetrics metrics : loadHistory) {
            if (metrics.getTimestamp() >= windowStart) {
                totalCount++;
                if (metrics.calculateLoad() > THRESHOLD_HIGH) {
                    highLoadCount++;
                }
            }
        }

        // Consider load sustained if > 80% of recent measurements are high
        return totalCount > 0 && ((double) highLoadCount / totalCount) > 0.8;
    }

    /**
     * Check if load balancer can be deployed
     */
    private boolean loadBalancerAvailable() {
        // Check if enough time has passed since last scaling
        long timeSinceLastScaling = System.currentTimeMillis() - lastScalingTime;
        return !loadBalancerDeployed && timeSinceLastScaling > 60000; // 1 minute cooldown
    }

    /**
     * Check if neighbor clusters are available for assistance
     */
    private boolean neighborsAvailable() {
        // This would check with federation manager
        // For now, assume available if not already using neighbors
        return true;
    }

    /**
     * Deploy horizontal scaling
     */
    public void deployLoadBalancer() {
        loadBalancerDeployed = true;
        additionalNodesCount++;
        lastScalingTime = System.currentTimeMillis();

        System.out.println(String.format(
            "[ThresholdScalingController] Deployed load balancer. Total additional nodes: %d",
            additionalNodesCount));
    }

    /**
     * Scale down if load returns to normal
     */
    public boolean canScaleDown() {
        if (!loadBalancerDeployed || loadHistory.isEmpty()) {
            return false;
        }

        // Check if load has been low for sustained period
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - SUSTAINED_LOAD_WINDOW;

        int lowLoadCount = 0;
        int totalCount = 0;

        for (LoadMetrics metrics : loadHistory) {
            if (metrics.getTimestamp() >= windowStart) {
                totalCount++;
                if (metrics.calculateLoad() < THRESHOLD_NORMAL) {
                    lowLoadCount++;
                }
            }
        }

        return totalCount > 0 && ((double) lowLoadCount / totalCount) > 0.9;
    }

    /**
     * Remove old history entries
     */
    private void cleanOldHistory() {
        long cutoff = System.currentTimeMillis() - (SUSTAINED_LOAD_WINDOW * 2);
        loadHistory.removeIf(metrics -> metrics.getTimestamp() < cutoff);
    }

    public boolean isLoadBalancerDeployed() {
        return loadBalancerDeployed;
    }

    public int getAdditionalNodesCount() {
        return additionalNodesCount;
    }

    /**
     * Scaling action result
     */
    public static class ScalingAction {
        private ScalingType type;
        private String reason;

        public ScalingAction(ScalingType type, String reason) {
            this.type = type;
            this.reason = reason;
        }

        public ScalingType getType() { return type; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("ScalingAction[%s: %s]", type, reason);
        }
    }

    /**
     * Types of scaling actions
     */
    public enum ScalingType {
        NONE,
        DEPLOY_LOAD_BALANCER,
        REQUEST_NEIGHBOR_ASSISTANCE,
        CLOUD_OFFLOAD
    }
}
