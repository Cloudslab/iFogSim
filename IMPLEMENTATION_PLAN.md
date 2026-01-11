# Implementation Plan for Adaptive Fog Computing Research in iFogSim

This document outlines the implementation strategy for 6 research papers by Narek Naltakyan on adaptive fog computing architectures.

## Research Papers Overview

1. **Load Balancing in Adaptive Fog Computing** - Dynamic area resizing, horizontal scaling, hierarchical congestion management
2. **Batch as a Service (BaaS)** - UDP-to-TCP conversion, differential privacy, quantum-safe cryptography
3. **Adaptive Fog Computing Architecture** - Location awareness, dynamic geographic orchestration, mobility support
4. **Federated Multi-Cluster with DANE/DANCE** - Multiplexer gateways, master node coordination, 50,000+ device support
5. **Dynamic Inter-Cluster Resource Sharing** - Request redirection, node migration, 30% sharing limit
6. **Hierarchical Federated Multi-Cluster** - Master-of-masters, sleeping nodes, 1M+ device support

## Phase 1: Core Architecture Components

### 1.1 New Package Structure

```
src/org/fog/adaptive/
├── loadbalancing/
│   ├── DynamicAreaManager.java
│   ├── ThresholdScalingController.java
│   └── HierarchicalOverflowManager.java
├── federation/
│   ├── MasterNode.java
│   ├── MasterOfMasters.java
│   ├── ClusterGateway.java
│   └── ResourceNegotiator.java
├── mobility/
│   ├── LocationService.java
│   ├── MobilityManager.java
│   └── DeviceClassifier.java (static vs dynamic)
├── baas/
│   ├── ProtocolConverter.java (UDP-TCP)
│   ├── BatchProcessor.java
│   └── SecurityManager.java (DANE/DANCE, differential privacy)
└── scenarios/
    ├── SmartCityScenario.java
    ├── TrafficManagementScenario.java
    └── IndustrialIoTScenario.java
```

### 1.2 Key Classes to Implement

**DynamicAreaManager** - Paper 1 & 3
- Manages geographic area boundaries for fog nodes
- Implements Algorithm 1: DynamicAreaAdjustment
- Calculates optimal area based on load metrics
```
OptimalArea = f(RequestRate, Capacity, Latency, Qos_target)
```

**ThresholdScalingController** - Paper 1
- Monitors load thresholds (85%, 90%, 95% CPU)
- Triggers horizontal/vertical scaling
- Deploys load balancers when area reaches minimum

**FederatedCluster** - Papers 4, 5, 6
- Manages inter-cluster communication
- Implements federation protocol
- Coordinates with neighboring clusters

**MasterOfMasters** - Paper 6
- Global coordination layer
- Master-slave fault tolerance (2.3s failover)
- Manages sleeping nodes (38% energy savings)

**ResourceSharingManager** - Paper 5
- Request redirection (35-45ms overhead)
- Node migration (30-60s migration time)
- 30% sharing limit enforcement
- Reciprocity scoring

## Phase 2: Load Balancing Implementation

### 2.1 Dynamic Area Management (Paper 1)

Create `src/org/fog/adaptive/loadbalancing/DynamicAreaManager.java`:

```java
public class DynamicAreaManager {
    private static final double THRESHOLD_HIGH = 0.85;
    private static final double THRESHOLD_LOW = 0.60;
    private static final double CONTRACTION_FACTOR = 0.8;
    private static final double EXPANSION_FACTOR = 1.2;

    // Area boundaries (latitude/longitude)
    private GeographicArea currentArea;
    private GeographicArea minArea;
    private GeographicArea maxArea;

    public GeographicArea adjustArea(double currentLoad,
                                      double requestRate) {
        if (currentLoad > THRESHOLD_HIGH && !isMinimumArea()) {
            return contractArea();
        } else if (currentLoad < THRESHOLD_LOW && !isMaximumArea()) {
            return expandArea();
        } else if (isMinimumArea() && currentLoad > THRESHOLD_CRITICAL) {
            triggerScaling();
        }
        return currentArea;
    }

    private GeographicArea contractArea() {
        // Algorithm from Paper 1, Section III
        double newSize = currentArea.getSize() * CONTRACTION_FACTOR;
        return new GeographicArea(currentArea.getCenter(), newSize);
    }
}
```

### 2.2 Load Calculation (Papers 1, 5, 6)

```java
public class LoadCalculator {
    public double calculateLoad(FogDevice device) {
        return 0.4 * device.getCpuUtilization() +
               0.3 * device.getQueueDepth() / MAX_QUEUE +
               0.3 * device.getMemoryUtilization();
    }
}
```

## Phase 3: BaaS Framework (Paper 2)

### 3.1 Protocol Conversion

```java
public class ProtocolConverter {
    // UDP to TCP conversion for IoT devices
    public TcpBatch convertUdpToTcp(List<UdpPacket> packets) {
        // Batch aggregation logic
        // 73% reduction in cloud traffic
        // 45% latency improvement
    }
}
```

### 3.2 Privacy-Preserving Batch Processing

```java
public class PrivacyPreservingProcessor {
    private double epsilon = 1.0; // Differential privacy parameter

    public ProcessedBatch processBatch(Batch batch) {
        // Implement differential privacy (98.7% data utility)
        // Apply homomorphic encryption for secure computation
        // 15% overhead for statistical operations
    }
}
```

## Phase 4: Federated Multi-Cluster (Papers 4, 5, 6)

### 4.1 Master Node Coordination

```java
public class MasterNode {
    private List<FogNode> localNodes;
    private List<MasterNode> neighborMasters;
    private double currentLoad;
    private double sharingCapacity; // Max 30%

    public void handleOverflow(Request request) {
        if (canHandleLocally()) {
            assignToLocalNode(request);
        } else if (sharingCapacity < 0.30) {
            requestRedirection(request);
        } else {
            initiateNodeMigration();
        }
    }
}
```

### 4.2 Inter-Cluster Resource Sharing (Paper 5)

```java
public class ResourceSharingManager {
    // Request Redirection Mode: <50ms negotiation
    public void redirectRequest(Request req, MasterNode target) {
        // Temporary support for load spikes
        // Added latency but fast activation
    }

    // Node Migration Mode: 30-60s migration time
    public void migrateNode(FogNode node, Cluster targetCluster) {
        // For prolonged imbalances
        // No additional latency after migration
        // 68% reduction in request dropping
    }
}
```

### 4.3 Master-of-Masters Architecture (Paper 6)

```java
public class MasterOfMasters {
    private List<MasterNode> regionalMasters;
    private MasterOfMasters slaveMaster; // Fault tolerance

    // Fault tolerance: 2.3s failover
    public void handleMasterFailure(MasterNode failed) {
        // Raft consensus for leader election
        // Automatic backup activation
    }

    // Sleeping node management: 38% energy savings
    public void manageSleepingNodes() {
        // Predictive activation based on load forecast
        // 30-60s wake-up time
    }
}
```

## Phase 5: Test Scenarios

### 5.1 Smart City Traffic Management (Paper 2)

Test scenario based on Paper 2, Section "Smart City Traffic Management Application":
- 400 intersection monitoring points
- Vehicle counts, speed measurements, traffic flow
- 78% bandwidth reduction
- 15% improvement in intersection wait times

```java
public class TrafficManagementScenario {
    public void setup() {
        // Create 400 intersection sensors
        // Each generates data every 10 seconds
        // Geographic hotspot: 80% load in one region
        // Event-based triggers for incidents
    }
}
```

### 5.2 Geographic Hotspot (Papers 1, 3, 5)

```java
public class GeographicHotspotScenario {
    // Cluster A: 90% load
    // Clusters B, C, D: 40-50% load
    // Duration: 2 hours
    // Expected: Request redirection, area contraction
}
```

### 5.3 Cascading Load (Paper 5)

```java
public class CascadingLoadScenario {
    // First spike triggers secondary spikes
    // Tests inter-cluster federation
    // 68% reduction in request dropping expected
}
```

### 5.4 Million-Device Deployment (Paper 6)

```java
public class MassiveScaleScenario {
    // 1,000,000+ IoT devices
    // 200 fog clusters across geographic regions
    // Tests: 99.95% availability, 2.3s failover
}
```

## Phase 6: Metrics Collection

### 6.1 Performance Metrics

Track metrics from all papers:

```java
public class MetricsCollector {
    // Paper 1
    - Request drop rate
    - Area size changes
    - Scaling events

    // Paper 2
    - Cloud traffic reduction (target: 73%)
    - Latency improvement (target: 45%)
    - Reliability (target: 99.8%)
    - Energy savings (target: 38%)
    - Cache hit rate (target: 85%)

    // Paper 3
    - End-to-end latency
    - Device handovers
    - Area boundary adjustments

    // Paper 4
    - Scalability (target: 50,000+ devices)
    - Cache efficiency
    - Federation latency (target: 35-45ms)

    // Paper 5
    - Request drop reduction (target: 68%)
    - Resource utilization improvement (target: 42%)
    - System-wide availability (target: 99.2%)

    // Paper 6
    - Device capacity (target: 1M+)
    - Availability (target: 99.95%)
    - Failover time (target: <2.3s)
    - Energy reduction (target: 38%)
}
```

## Implementation Priority

### Week 1-2: Foundation
1. ✅ Create package structure
2. ✅ Implement DynamicAreaManager (Paper 1)
3. ✅ Implement LoadCalculator (all papers)
4. ✅ Create base FederatedCluster class (Papers 4, 5, 6)

### Week 3-4: Load Balancing
1. Implement ThresholdScalingController (Paper 1)
2. Implement HierarchicalOverflowManager (Paper 1)
3. Create first test scenario: Geographic Hotspot
4. Collect baseline metrics

### Week 5-6: BaaS Framework
1. Implement ProtocolConverter (Paper 2)
2. Implement BatchProcessor (Paper 2)
3. Add privacy-preserving mechanisms (Paper 2)
4. Test Smart City Traffic scenario

### Week 7-8: Federation
1. Implement MasterNode (Papers 4, 5, 6)
2. Implement ResourceSharingManager (Paper 5)
3. Add request redirection logic
4. Add node migration logic

### Week 9-10: Master-of-Masters
1. Implement MasterOfMasters (Paper 6)
2. Add fault tolerance with master-slave
3. Implement sleeping node management
4. Test massive-scale scenario (1M+ devices)

### Week 11-12: Testing & Results
1. Run all scenarios
2. Collect comprehensive metrics
3. Generate comparison charts
4. Document results for publications

## Key Algorithms to Implement

### Algorithm 1: Dynamic Area Adjustment (Paper 1)
```
Input: node, currentLoad, requestRate
Output: updatedBoundaries

1: calculateOptimalArea(node)
2: IF currentLoad > THRESHOLD_HIGH AND area > AREA_MIN THEN
3:     newArea ← area × CONTRACTION_FACTOR
4:     redistributeBoundaries(newArea)
5: ELSE IF currentLoad < THRESHOLD_LOW AND area < AREA_MAX THEN
6:     newArea ← area × EXPANSION_FACTOR
7:     redistributeBoundaries(newArea)
8: ELSE IF currentLoad > THRESHOLD_CRITICAL AND area ≤ AREA_MIN THEN
9:     triggerScalingProcedure()
10: RETURN updatedBoundaries
```

### Algorithm 2: Master Node Federation (Paper 5)
```
Input: overflow_request, local_cluster, neighbors
Output: ResourceSharingDecision

1: IF CanAssignLocally(cluster) THEN
2:     RETURN AssignToFogNode(FindAvailable(cluster))
3: FOR EACH neighbor IN SortByLatency(neighbors) DO
4:     capacity ← QueryCapacity(neighbor)
5:     IF capacity > THRESHOLD THEN
6:         IF NegotiateDelegation(neighbor) THEN
7:             RETURN DelegateToNeighbor(neighbor)
8: RETURN QueueRequest(cluster)
```

### Algorithm 3: Sleeping Node Management (Paper 6)
```
Conditions for activation:
1. Instant: Load > 85% → activate in 30-60s
2. Predictive: Forecast(Load) > 70% → schedule activation
3. Deactivation: Load < 30% for >10min → sleep mode
```

## Expected Results Summary

| Paper | Key Metric | Target Value | Test Scenario |
|-------|------------|--------------|---------------|
| 1 | Request drop reduction | Significant | Geographic Hotspot |
| 2 | Cloud traffic reduction | 73% | Smart City Traffic |
| 2 | Latency improvement | 45% | Smart City Traffic |
| 2 | Reliability | 99.8% | Smart City Traffic |
| 2 | Energy savings | 38% | All scenarios |
| 3 | Scalability | Support mobility | Location-aware test |
| 4 | Device capacity | 50,000+ | Multi-cluster test |
| 4 | Cache hit rate | 85% | Multi-cluster test |
| 4 | Federation latency | 35-45ms | Federation test |
| 5 | Request drop reduction | 68% | Resource sharing test |
| 5 | Resource utilization | +42% | Resource sharing test |
| 5 | Availability | 99.2% | Resource sharing test |
| 6 | Device capacity | 1,000,000+ | Massive scale test |
| 6 | Availability | 99.95% | Fault tolerance test |
| 6 | Failover time | <2.3s | Master failure test |

## Next Steps

1. Review this implementation plan
2. Start with Phase 1: Core Architecture
3. Implement DynamicAreaManager first
4. Create initial test scenario
5. Iterate and expand based on results

## Questions to Address

1. Which paper should we implement first as proof of concept?
2. Do you have access to real IoT devices for testing, or simulation only?
3. What are the publication deadlines for results?
4. Should we integrate all papers into one comprehensive framework or separate implementations?

---

**Author**: Narek Naltakyan
**Institution**: National Polytechnic University of Armenia
**Email**: nareknaltakyan1@gmail.com
