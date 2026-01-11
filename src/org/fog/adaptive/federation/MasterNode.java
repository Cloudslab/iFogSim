package org.fog.adaptive.federation;

import org.fog.adaptive.models.LoadMetrics;
import org.fog.adaptive.models.SharingMode;
import org.fog.entities.FogDevice;

import java.util.*;

/**
 * Master Node - Papers 4, 5, 6
 *
 * Coordinates cluster-level operations and inter-cluster federation
 * Implements:
 * - Resource negotiation (Paper 5)
 * - Master node coordination (Paper 4)
 * - Regional coordination (Paper 6)
 *
 * @author Narek Naltakyan
 */
public class MasterNode {

    // Sharing constraints from Paper 5
    private static final double MAX_SHARING_RATIO = 0.30; // 30% limit

    private String clusterId;
    private List<FogDevice> localNodes;
    private List<MasterNode> neighborMasters;
    private LoadMetrics currentLoad;
    private double sharingCapacity;
    private Map<MasterNode, Double> reciprocityScores;

    // Priority calculation weights (Paper 5)
    private static final double RECIPROCITY_WEIGHT = 0.4;
    private static final double CRITICALITY_WEIGHT = 0.3;
    private static final double CAPACITY_WEIGHT = 0.2;
    private static final double PROXIMITY_WEIGHT = 0.1;

    public MasterNode(String clusterId) {
        this.clusterId = clusterId;
        this.localNodes = new ArrayList<>();
        this.neighborMasters = new ArrayList<>();
        this.reciprocityScores = new HashMap<>();
        this.sharingCapacity = 0.0;
    }

    /**
     * Handle overflow request - Paper 5, Algorithm 2
     */
    public FederationDecision handleOverflow(OverflowRequest request) {
        // Step 1: IF canAssignLocally(cluster) THEN
        if (canAssignLocally()) {
            FogDevice available = findAvailableLocalNode();
            return new FederationDecision(
                FederationAction.ASSIGN_LOCAL,
                null,
                available,
                "Assigned to local fog node");
        }

        // Step 2: FOR EACH neighbor IN SortByLatency(neighbors)
        List<MasterNode> sortedNeighbors = sortNeighborsByPriority();

        for (MasterNode neighbor : sortedNeighbors) {
            // Step 3: capacity ← QueryCapacity(neighbor)
            double capacity = queryNeighborCapacity(neighbor);

            // Step 4: IF capacity > THRESHOLD THEN
            if (capacity > 0.2) { // 20% available capacity threshold
                // Step 5: IF NegotiateDelegation(neighbor, request) THEN
                if (negotiateDelegation(neighbor, request)) {
                    return new FederationDecision(
                        FederationAction.DELEGATE_TO_NEIGHBOR,
                        neighbor,
                        null,
                        "Delegated to neighbor cluster");
                }
            }
        }

        // Step 6: RETURN QueueRequest(cluster)
        return new FederationDecision(
            FederationAction.QUEUE_REQUEST,
            null,
            null,
            "No capacity available, queuing request");
    }

    /**
     * Check if request can be assigned locally
     */
    private boolean canAssignLocally() {
        if (localNodes.isEmpty()) {
            return false;
        }

        // Check if any local node has capacity
        for (FogDevice node : localNodes) {
            double load = getNodeLoad(node);
            if (load < 0.85) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find available local fog node
     */
    private FogDevice findAvailableLocalNode() {
        FogDevice bestNode = null;
        double lowestLoad = 1.0;

        for (FogDevice node : localNodes) {
            double load = getNodeLoad(node);
            if (load < lowestLoad && load < 0.85) {
                lowestLoad = load;
                bestNode = node;
            }
        }

        return bestNode;
    }

    /**
     * Sort neighbors by priority - Paper 5
     * Priority = 0.4×Reciprocity + 0.3×Criticality + 0.2×Capacity + 0.1×Proximity
     */
    private List<MasterNode> sortNeighborsByPriority() {
        List<MasterNode> sorted = new ArrayList<>(neighborMasters);

        sorted.sort((n1, n2) -> {
            double priority1 = calculatePriority(n1);
            double priority2 = calculatePriority(n2);
            return Double.compare(priority2, priority1); // Higher priority first
        });

        return sorted;
    }

    /**
     * Calculate priority score for neighbor - Paper 5, Section IV.B
     */
    private double calculatePriority(MasterNode neighbor) {
        double reciprocity = reciprocityScores.getOrDefault(neighbor, 1.0);
        double criticality = 1.0; // Production systems
        double capacity = queryNeighborCapacity(neighbor);
        double proximity = 1.0; // Directly connected

        return RECIPROCITY_WEIGHT * reciprocity +
               CRITICALITY_WEIGHT * criticality +
               CAPACITY_WEIGHT * capacity +
               PROXIMITY_WEIGHT * proximity;
    }

    /**
     * Query neighbor's available capacity
     */
    private double queryNeighborCapacity(MasterNode neighbor) {
        // In real implementation, send network query
        // For now, simulate
        return Math.random() * 0.5; // 0-50% available
    }

    /**
     * Negotiate resource delegation with neighbor
     */
    private boolean negotiateDelegation(MasterNode neighbor, OverflowRequest request) {
        // Check if within sharing limits
        if (sharingCapacity >= MAX_SHARING_RATIO) {
            System.out.println(String.format(
                "[MasterNode %s] Cannot delegate - sharing limit reached (%.0f%%)",
                clusterId, sharingCapacity * 100));
            return false;
        }

        // Negotiate with neighbor
        boolean accepted = neighbor.acceptDelegation(this, request);

        if (accepted) {
            // Update sharing capacity
            sharingCapacity += 0.05; // Each delegation uses 5%

            // Update reciprocity score
            double currentScore = reciprocityScores.getOrDefault(neighbor, 1.0);
            reciprocityScores.put(neighbor, currentScore * 0.95); // Decrease score

            System.out.println(String.format(
                "[MasterNode %s] Delegation to %s accepted. Sharing: %.0f%%",
                clusterId, neighbor.getClusterId(), sharingCapacity * 100));
        }

        return accepted;
    }

    /**
     * Accept delegation from another cluster
     */
    public boolean acceptDelegation(MasterNode requester, OverflowRequest request) {
        // Check local capacity
        if (!canAssignLocally()) {
            return false;
        }

        // Check sharing limit
        if (sharingCapacity >= MAX_SHARING_RATIO) {
            return false;
        }

        // Accept delegation
        sharingCapacity += 0.05;

        // Update reciprocity score (increase for helping)
        double currentScore = reciprocityScores.getOrDefault(requester, 1.0);
        reciprocityScores.put(requester, Math.min(2.0, currentScore * 1.05));

        return true;
    }

    /**
     * Get node load (simplified)
     */
    private double getNodeLoad(FogDevice node) {
        // In real implementation, query actual metrics
        return Math.random() * 0.8;
    }

    public void addLocalNode(FogDevice node) {
        localNodes.add(node);
    }

    public void addNeighbor(MasterNode neighbor) {
        if (!neighborMasters.contains(neighbor)) {
            neighborMasters.add(neighbor);
            reciprocityScores.put(neighbor, 1.0); // Initialize reciprocity
        }
    }

    public String getClusterId() {
        return clusterId;
    }

    public double getSharingCapacity() {
        return sharingCapacity;
    }

    /**
     * Overflow request
     */
    public static class OverflowRequest {
        private String requestId;
        private double estimatedLoad;
        private int priority;

        public OverflowRequest(String id, double load, int priority) {
            this.requestId = id;
            this.estimatedLoad = load;
            this.priority = priority;
        }

        public String getRequestId() { return requestId; }
        public double getEstimatedLoad() { return estimatedLoad; }
        public int getPriority() { return priority; }
    }

    /**
     * Federation decision
     */
    public static class FederationDecision {
        private FederationAction action;
        private MasterNode targetCluster;
        private FogDevice targetNode;
        private String reason;

        public FederationDecision(FederationAction action, MasterNode cluster,
                                FogDevice node, String reason) {
            this.action = action;
            this.targetCluster = cluster;
            this.targetNode = node;
            this.reason = reason;
        }

        public FederationAction getAction() { return action; }
        public MasterNode getTargetCluster() { return targetCluster; }
        public FogDevice getTargetNode() { return targetNode; }
        public String getReason() { return reason; }
    }

    public enum FederationAction {
        ASSIGN_LOCAL,
        DELEGATE_TO_NEIGHBOR,
        QUEUE_REQUEST
    }
}
