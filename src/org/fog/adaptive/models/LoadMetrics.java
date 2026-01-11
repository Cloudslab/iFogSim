package org.fog.adaptive.models;

/**
 * Load metrics for fog nodes
 * Used across all papers for load calculation
 * Load = 0.4 × CPU + 0.3 × Queue + 0.3 × Memory
 *
 * @author Narek Naltakyan
 */
public class LoadMetrics {
    private double cpuUtilization;      // 0.0 to 1.0
    private double memoryUtilization;   // 0.0 to 1.0
    private double queueDepth;          // 0.0 to 1.0 (normalized)
    private double bandwidth;           // 0.0 to 1.0
    private int requestRate;            // requests per second
    private double latency;             // milliseconds
    private long timestamp;

    // Load calculation weights (Papers 1, 5, 6)
    private static final double CPU_WEIGHT = 0.4;
    private static final double QUEUE_WEIGHT = 0.3;
    private static final double MEMORY_WEIGHT = 0.3;

    public LoadMetrics() {
        this.timestamp = System.currentTimeMillis();
    }

    public LoadMetrics(double cpu, double memory, double queue) {
        this.cpuUtilization = cpu;
        this.memoryUtilization = memory;
        this.queueDepth = queue;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Calculate composite load score
     * Formula from Papers 1, 5, 6
     */
    public double calculateLoad() {
        return CPU_WEIGHT * cpuUtilization +
               QUEUE_WEIGHT * queueDepth +
               MEMORY_WEIGHT * memoryUtilization;
    }

    /**
     * Check if load exceeds threshold
     */
    public boolean exceedsThreshold(double threshold) {
        return calculateLoad() > threshold;
    }

    /**
     * Calculate load trend (for predictive scaling - Paper 6)
     */
    public double calculateTrend(LoadMetrics previous, long timeIntervalMs) {
        if (previous == null || timeIntervalMs == 0) {
            return 0.0;
        }
        return (calculateLoad() - previous.calculateLoad()) / (timeIntervalMs / 1000.0);
    }

    // Getters and setters
    public double getCpuUtilization() { return cpuUtilization; }
    public void setCpuUtilization(double cpu) { this.cpuUtilization = Math.max(0, Math.min(1, cpu)); }

    public double getMemoryUtilization() { return memoryUtilization; }
    public void setMemoryUtilization(double memory) { this.memoryUtilization = Math.max(0, Math.min(1, memory)); }

    public double getQueueDepth() { return queueDepth; }
    public void setQueueDepth(double queue) { this.queueDepth = Math.max(0, Math.min(1, queue)); }

    public double getBandwidth() { return bandwidth; }
    public void setBandwidth(double bandwidth) { this.bandwidth = bandwidth; }

    public int getRequestRate() { return requestRate; }
    public void setRequestRate(int requestRate) { this.requestRate = requestRate; }

    public double getLatency() { return latency; }
    public void setLatency(double latency) { this.latency = latency; }

    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("LoadMetrics[CPU=%.2f, Mem=%.2f, Queue=%.2f, Load=%.2f, ReqRate=%d/s]",
                           cpuUtilization, memoryUtilization, queueDepth, calculateLoad(), requestRate);
    }

    /**
     * Create a snapshot copy
     */
    public LoadMetrics copy() {
        LoadMetrics copy = new LoadMetrics(cpuUtilization, memoryUtilization, queueDepth);
        copy.bandwidth = this.bandwidth;
        copy.requestRate = this.requestRate;
        copy.latency = this.latency;
        copy.timestamp = this.timestamp;
        return copy;
    }
}
