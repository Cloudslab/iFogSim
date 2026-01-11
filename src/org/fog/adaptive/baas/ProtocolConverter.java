package org.fog.adaptive.baas;

import java.util.ArrayList;
import java.util.List;

/**
 * Protocol Converter - Paper 2: Batch as a Service
 *
 * UDP-to-TCP conversion with intelligent batching
 * Achieves 73% reduction in cloud traffic
 *
 * @author Narek Naltakyan
 */
public class ProtocolConverter {

    // Batch configuration from Paper 2
    private static final int OPTIMAL_BATCH_SIZE_MIN = 100;
    private static final int OPTIMAL_BATCH_SIZE_MAX = 200;
    private static final long BATCH_TIMEOUT_MS = 100; // Sub-100ms latency

    private List<UdpPacket> currentBatch;
    private long batchStartTime;
    private BatchProcessor batchProcessor;

    public ProtocolConverter() {
        this.currentBatch = new ArrayList<>();
        this.batchStartTime = System.currentTimeMillis();
        this.batchProcessor = new BatchProcessor();
    }

    /**
     * Convert UDP packet to TCP batch
     * Paper 2: 73% reduction in cloud traffic through intelligent batching
     */
    public TcpBatch convertUdpToTcp(UdpPacket packet) {
        currentBatch.add(packet);

        // Check batch completion conditions
        boolean sizeFull = currentBatch.size() >= OPTIMAL_BATCH_SIZE_MAX;
        boolean timeoutReached = (System.currentTimeMillis() - batchStartTime) >= BATCH_TIMEOUT_MS;

        if (sizeFull || timeoutReached) {
            return createTcpBatch();
        }

        return null; // Batch not ready
    }

    /**
     * Create TCP batch from accumulated UDP packets
     */
    private TcpBatch createTcpBatch() {
        if (currentBatch.isEmpty()) {
            return null;
        }

        TcpBatch batch = new TcpBatch(currentBatch);

        // Reset for next batch
        currentBatch = new ArrayList<>();
        batchStartTime = System.currentTimeMillis();

        System.out.println(String.format(
            "[ProtocolConverter] Created TCP batch with %d packets",
            batch.getPacketCount()));

        return batch;
    }

    /**
     * UDP Packet representation
     */
    public static class UdpPacket {
        private String deviceId;
        private byte[] payload;
        private long timestamp;
        private String sourceIp;
        private int sourcePort;

        public UdpPacket(String deviceId, byte[] payload) {
            this.deviceId = deviceId;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
        }

        public String getDeviceId() { return deviceId; }
        public byte[] getPayload() { return payload; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * TCP Batch representation
     */
    public static class TcpBatch {
        private List<UdpPacket> packets;
        private long creationTime;
        private String batchId;

        public TcpBatch(List<UdpPacket> packets) {
            this.packets = new ArrayList<>(packets);
            this.creationTime = System.currentTimeMillis();
            this.batchId = generateBatchId();
        }

        private String generateBatchId() {
            return "BATCH_" + System.currentTimeMillis() + "_" +
                   Integer.toHexString(hashCode());
        }

        public List<UdpPacket> getPackets() { return packets; }
        public int getPacketCount() { return packets.size(); }
        public String getBatchId() { return batchId; }
        public long getCreationTime() { return creationTime; }
    }
}
