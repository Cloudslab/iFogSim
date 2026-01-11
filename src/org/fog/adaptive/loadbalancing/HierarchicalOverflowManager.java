package org.fog.adaptive.loadbalancing;

import org.fog.adaptive.models.LoadMetrics;
import org.fog.entities.FogDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Hierarchical Overflow Management - Paper 1
 *
 * Implements Algorithm 3: HierarchicalOverflow
 * Coordinates request processing through master node when local capacity exceeded
 *
 * @author Narek Naltakyan
 */
public class HierarchicalOverflowManager {

    private FogDevice sourceNode;
    private FogDevice masterNode;
    private Queue<OverflowRequest> overflowQueue;
    private List<FogDevice> neighborNodes;

    public HierarchicalOverflowManager(FogDevice source, FogDevice master) {
        this.sourceNode = source;
        this.masterNode = master;
        this.overflowQueue = new LinkedBlockingQueue<>();
        this.neighborNodes = new ArrayList<>();
    }

    /**
     * Algorithm 3: HierarchicalOverflow from Paper 1, Section III
     */
    public OverflowDecision handleOverflow(OverflowRequest request) {
        // Step 1: masterNode.receiveOverflowRequest
        System.out.println(String.format(
            "[HierarchicalOverflow] Received overflow request from %s",
            sourceNode.getName()));

        // Step 2: availableNodes ← masterNode.findAvailableNeighbors
        List<FogDevice> availableNodes = findAvailableNeighbors();

        // Step 3: IF availableNodes ≠ ∅
        if (!availableNodes.isEmpty()) {
            // Step 4: targetNode ← selectOptimalNode
            FogDevice targetNode = selectOptimalNode(availableNodes, request);

            // Step 5: IF targetNode.canAccept(request)
            if (canAcceptRequest(targetNode, request)) {
                // Step 6: forwardRequest
                return new OverflowDecision(
                    OverflowAction.FORWARD_TO_NEIGHBOR,
                    targetNode,
                    "Forwarding to neighbor fog node");
            }
        }

        // Step 7: ELSE - no neighbors available
        // Step 8: IF cloudAvailable()
        if (cloudAvailable()) {
            // Step 9: offloadToCloud
            return new OverflowDecision(
                OverflowAction.OFFLOAD_TO_CLOUD,
                null,
                "No fog capacity available, offloading to cloud");
        } else {
            // Step 10: queueRequest
            overflowQueue.offer(request);
            return new OverflowDecision(
                OverflowAction.QUEUE_REQUEST,
                null,
                "Queueing request for later processing");
        }
    }

    /**
     * Find available neighbor fog nodes
     */
    private List<FogDevice> findAvailableNeighbors() {
        List<FogDevice> available = new ArrayList<>();

        for (FogDevice neighbor : neighborNodes) {
            // Check if neighbor has capacity
            // This would query actual load metrics in real implementation
            double neighborLoad = getNodeLoad(neighbor);
            if (neighborLoad < 0.85) { // THRESHOLD_HIGH
                available.add(neighbor);
            }
        }

        return available;
    }

    /**
     * Select optimal neighbor node based on load and proximity
     */
    private FogDevice selectOptimalNode(List<FogDevice> available, OverflowRequest request) {
        FogDevice bestNode = null;
        double bestScore = Double.MAX_VALUE;

        for (FogDevice node : available) {
            double load = getNodeLoad(node);
            double latency = estimateLatency(sourceNode, node);

            // Score = weighted combination of load and latency
            double score = 0.6 * load + 0.4 * (latency / 100.0); // Normalize latency

            if (score < bestScore) {
                bestScore = score;
                bestNode = node;
            }
        }

        return bestNode;
    }

    /**
     * Check if target node can accept the request
     */
    private boolean canAcceptRequest(FogDevice target, OverflowRequest request) {
        double currentLoad = getNodeLoad(target);
        // Estimate additional load from this request
        double estimatedAdditionalLoad = 0.05; // Simplified estimation

        return (currentLoad + estimatedAdditionalLoad) < 0.90;
    }

    /**
     * Check if cloud is available for offloading
     */
    private boolean cloudAvailable() {
        // In real implementation, check cloud connectivity and SLA
        return true;
    }

    /**
     * Get current load of a fog node
     */
    private double getNodeLoad(FogDevice node) {
        // This would query actual metrics from the node
        // For now, return simulated value
        return Math.random() * 0.5 + 0.3; // Random between 0.3-0.8
    }

    /**
     * Estimate network latency between two nodes
     */
    private double estimateLatency(FogDevice source, FogDevice target) {
        // In real implementation, measure actual network latency
        // For now, return simulated value in milliseconds
        return Math.random() * 50 + 10; // Random between 10-60ms
    }

    public void addNeighbor(FogDevice neighbor) {
        if (!neighborNodes.contains(neighbor)) {
            neighborNodes.add(neighbor);
        }
    }

    public void removeNeighbor(FogDevice neighbor) {
        neighborNodes.remove(neighbor);
    }

    public Queue<OverflowRequest> getOverflowQueue() {
        return overflowQueue;
    }

    /**
     * Represents an overflow request
     */
    public static class OverflowRequest {
        private String requestId;
        private long timestamp;
        private double estimatedLoad;
        private int priority;

        public OverflowRequest(String id, double load, int priority) {
            this.requestId = id;
            this.estimatedLoad = load;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }

        public String getRequestId() { return requestId; }
        public double getEstimatedLoad() { return estimatedLoad; }
        public int getPriority() { return priority; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Overflow handling decision
     */
    public static class OverflowDecision {
        private OverflowAction action;
        private FogDevice targetNode;
        private String reason;

        public OverflowDecision(OverflowAction action, FogDevice target, String reason) {
            this.action = action;
            this.targetNode = target;
            this.reason = reason;
        }

        public OverflowAction getAction() { return action; }
        public FogDevice getTargetNode() { return targetNode; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("OverflowDecision[%s: %s]", action, reason);
        }
    }

    /**
     * Possible overflow actions
     */
    public enum OverflowAction {
        FORWARD_TO_NEIGHBOR,
        OFFLOAD_TO_CLOUD,
        QUEUE_REQUEST
    }
}
