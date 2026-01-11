package org.fog.adaptive.models;

/**
 * Resource sharing modes between clusters
 * Paper 5: Dynamic Inter-Cluster Resource Sharing
 *
 * @author Narek Naltakyan
 */
public enum SharingMode {
    /**
     * Request Redirection - for temporary load spikes
     * - Fast activation: <50ms negotiation
     * - Adds latency but prevents request dropping
     * - Used when sharing capacity < 30%
     */
    REQUEST_REDIRECTION,

    /**
     * Node Migration - for sustained load imbalances
     * - Migration time: 30-60 seconds
     * - No additional latency after migration
     * - Node becomes full member of target cluster
     * - Used when area at minimum and load still high
     */
    NODE_MIGRATION,

    /**
     * No sharing - handle locally
     */
    NONE
}
