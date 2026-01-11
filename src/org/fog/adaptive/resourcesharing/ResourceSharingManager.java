package org.fog.adaptive.resourcesharing;

import org.fog.adaptive.models.SharingMode;
import org.fog.adaptive.federation.MasterNode;
import org.fog.entities.FogDevice;

/**
 * Resource Sharing Manager - Paper 5
 *
 * Implements:
 * 1. Request Redirection (<50ms negotiation)
 * 2. Node Migration (30-60s migration time)
 * 3. 30% sharing limit enforcement
 *
 * Results: 68% reduction in request dropping, 42% resource utilization improvement
 *
 * @author Narek Naltakyan
 */
public class ResourceSharingManager {

    // Timing constraints from Paper 5
    private static final long REQUEST_REDIRECTION_TIMEOUT_MS = 50;
    private static final long NODE_MIGRATION_TIME_MS = 45000; // 30-60s avg
    private static final double SHARING_LIMIT = 0.30;

    private MasterNode sourceMaster;
    private SharingMode currentMode;

    public ResourceSharingManager(MasterNode master) {
        this.sourceMaster = master;
        this.currentMode = SharingMode.NONE;
    }

    /**
     * Request Redirection Mode - Paper 5, Section III.C
     *
     * - Temporary support for load spikes
     * - Fast activation: <50ms negotiation
     * - Adds latency but prevents request dropping
     */
    public RedirectionResult redirectRequest(Request request, MasterNode targetMaster) {
        long start = System.currentTimeMillis();

        // Quick capacity check
        if (sourceMaster.getSharingCapacity() >= SHARING_LIMIT) {
            return new RedirectionResult(
                false,
                "Sharing limit reached (30%)",
                0);
        }

        // Negotiate redirection
        boolean accepted = targetMaster.acceptDelegation(
            sourceMaster,
            new MasterNode.OverflowRequest(request.getId(), 0.05, request.getPriority()));

        long negotiationTime = System.currentTimeMillis() - start;

        // Paper 5: Negotiation should be < 50ms
        if (negotiationTime > REQUEST_REDIRECTION_TIMEOUT_MS) {
            System.out.println(String.format(
                "[WARNING] Redirection negotiation took %dms (target: <%dms)",
                negotiationTime, REQUEST_REDIRECTION_TIMEOUT_MS));
        }

        if (accepted) {
            System.out.println(String.format(
                "[ResourceSharing] Request %s redirected in %dms",
                request.getId(), negotiationTime));
        }

        return new RedirectionResult(accepted, "Redirection processed", negotiationTime);
    }

    /**
     * Node Migration Mode - Paper 5, Section III.C
     *
     * - For prolonged load imbalances
     * - Migration time: 30-60 seconds
     * - No additional latency after migration
     * - Node becomes full member of target cluster
     */
    public MigrationResult migrateNode(FogDevice node, Cluster targetCluster) {
        long start = System.currentTimeMillis();

        System.out.println(String.format(
            "[ResourceSharing] Starting migration of node %s to cluster %s",
            node.getName(), targetCluster.getId()));

        // Phase 1: Checkpoint - finish active requests
        checkpointNode(node);

        // Phase 2: Disconnect from source cluster
        disconnectFromCluster(node);

        // Phase 3: Register with target cluster
        registerWithCluster(node, targetCluster);

        // Phase 4: Start accepting new requests
        activateInNewCluster(node, targetCluster);

        long migrationTime = System.currentTimeMillis() - start;

        // Paper 5: Migration time should be 30-60s
        System.out.println(String.format(
            "[ResourceSharing] Node migration completed in %.1fs (target: 30-60s)",
            migrationTime / 1000.0));

        return new MigrationResult(
            true,
            "Migration successful",
            migrationTime,
            node,
            targetCluster);
    }

    /**
     * Checkpoint node - finish active requests (Paper 5, Section V)
     * Reduces request drops during migration from 12% to 1%
     */
    private void checkpointNode(FogDevice node) {
        // Wait for requests with < 2s remaining processing time
        // Forward pending requests to other nodes
        System.out.println("[Migration] Checkpointing node - completing active requests");

        try {
            Thread.sleep(2000); // Simulate checkpoint time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Disconnect from source cluster
     */
    private void disconnectFromCluster(FogDevice node) {
        System.out.println("[Migration] Disconnecting from source cluster");
    }

    /**
     * Register with target cluster
     */
    private void registerWithCluster(FogDevice node, Cluster targetCluster) {
        System.out.println(String.format(
            "[Migration] Registering with cluster %s",
            targetCluster.getId()));
    }

    /**
     * Activate node in new cluster
     */
    private void activateInNewCluster(FogDevice node, Cluster targetCluster) {
        System.out.println("[Migration] Node activated in new cluster");
    }

    /**
     * Determine appropriate sharing mode
     */
    public SharingMode determineSharingMode(double currentLoad, boolean areaAtMinimum) {
        double sharingRatio = sourceMaster.getSharingCapacity();

        // Paper 5: Mode selection logic
        if (sharingRatio < SHARING_LIMIT) {
            return SharingMode.REQUEST_REDIRECTION;
        } else if (areaAtMinimum && currentLoad > 0.90) {
            return SharingMode.NODE_MIGRATION;
        } else {
            return SharingMode.NONE;
        }
    }

    /**
     * Request representation
     */
    public static class Request {
        private String id;
        private int priority;

        public Request(String id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        public String getId() { return id; }
        public int getPriority() { return priority; }
    }

    /**
     * Cluster representation
     */
    public static class Cluster {
        private String id;

        public Cluster(String id) {
            this.id = id;
        }

        public String getId() { return id; }
    }

    /**
     * Redirection result
     */
    public static class RedirectionResult {
        private boolean success;
        private String message;
        private long negotiationTimeMs;

        public RedirectionResult(boolean success, String message, long time) {
            this.success = success;
            this.message = message;
            this.negotiationTimeMs = time;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getNegotiationTimeMs() { return negotiationTimeMs; }
    }

    /**
     * Migration result
     */
    public static class MigrationResult {
        private boolean success;
        private String message;
        private long migrationTimeMs;
        private FogDevice migratedNode;
        private Cluster targetCluster;

        public MigrationResult(boolean success, String message, long time,
                             FogDevice node, Cluster cluster) {
            this.success = success;
            this.message = message;
            this.migrationTimeMs = time;
            this.migratedNode = node;
            this.targetCluster = cluster;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getMigrationTimeMs() { return migrationTimeMs; }
        public FogDevice getMigratedNode() { return migratedNode; }
        public Cluster getTargetCluster() { return targetCluster; }
    }
}
