# Adaptive Fog Computing Framework for iFogSim

Implementation of 6 research papers by **Narek Naltakyan** on adaptive fog computing architectures.

**Author**: Narek Naltakyan
**Institution**: National Polytechnic University of Armenia
**Email**: nareknaltakyan1@gmail.com

---

## Overview

This package implements a comprehensive adaptive fog computing framework that integrates concepts from 6 research papers:

1. **Load Balancing in Adaptive Fog Computing** - Dynamic area resizing and hierarchical overflow management
2. **Batch as a Service (BaaS)** - UDP-to-TCP conversion with privacy-preserving mechanisms
3. **Adaptive Fog Computing Architecture** - Location awareness and mobility management
4. **Federated Multi-Cluster with DANE/DANCE** - Multiplexer gateways and master node coordination
5. **Dynamic Inter-Cluster Resource Sharing** - Request redirection and node migration
6. **Hierarchical Federated Multi-Cluster** - Master-of-masters with fault tolerance and sleeping nodes

---

## Package Structure

```
org.fog.adaptive/
├── models/                     # Data models and enums
│   ├── GeographicArea.java    # Geographic areas for fog nodes
│   ├── LoadMetrics.java       # Load calculation (0.4×CPU + 0.3×Queue + 0.3×Mem)
│   ├── DeviceType.java        # STATIC, DYNAMIC, PREDICTABLE devices
│   └── SharingMode.java       # REQUEST_REDIRECTION, NODE_MIGRATION, NONE
│
├── loadbalancing/             # Paper 1: Load Balancing
│   ├── DynamicAreaManager.java              # Algorithm 1: Area adjustment
│   ├── ThresholdScalingController.java      # Algorithm 2: Scaling decisions
│   └── HierarchicalOverflowManager.java     # Algorithm 3: Overflow handling
│
├── baas/                      # Paper 2: Batch as a Service
│   ├── ProtocolConverter.java              # UDP-to-TCP conversion
│   └── BatchProcessor.java                 # Differential privacy & encryption
│
├── mobility/                  # Paper 3: Mobility Management
│   └── LocationService.java               # Location-aware fog orchestration
│
├── federation/                # Papers 4, 5, 6: Federation
│   ├── MasterNode.java                    # Cluster-level coordination
│   └── MasterOfMasters.java               # Global coordination with fault tolerance
│
├── resourcesharing/           # Paper 5: Resource Sharing
│   └── ResourceSharingManager.java        # Request redirection & node migration
│
└── scenarios/                 # Test scenarios
    └── SmartCityTrafficScenario.java      # 400 intersections, 4 clusters
```

---

## Paper 1: Load Balancing in Adaptive Fog Computing

### Key Components

**DynamicAreaManager** - Implements Algorithm 1: DynamicAreaAdjustment
- Dynamically adjusts geographic coverage area based on load
- Contraction factor: 0.8 when load > 85%
- Expansion factor: 1.2 when load < 60%
- Triggers scaling when area reaches minimum and load > 95%

**ThresholdScalingController** - Implements Algorithm 2: ScalingDecision
- Monitors sustained load over 5-minute windows
- Deploys load balancer when area at minimum
- Requests neighbor assistance before cloud offload
- Prevents cascade failures through distributed coordination

**HierarchicalOverflowManager** - Implements Algorithm 3: HierarchicalOverflow
- Coordinates overflow through master node
- Selects optimal neighbor based on load (60%) and latency (40%)
- Falls back to cloud when fog capacity exhausted

### Usage Example

```java
// Create area manager
GeographicArea area = new GeographicArea(40.7128, -74.0060, 10.0); // NYC, 10km radius
DynamicAreaManager areaManager = new DynamicAreaManager(fogNode, area);

// Update based on current load
LoadMetrics load = new LoadMetrics(0.92, 0.85, 0.78); // CPU, Memory, Queue
GeographicArea newArea = areaManager.adjustArea(load, 150); // 150 req/s

// Check scaling decisions
ThresholdScalingController scaler = new ThresholdScalingController(fogNode);
ScalingAction action = scaler.makeScalingDecision(load, area.isMinimumSize());
```

### Expected Results
- Request drop reduction through dynamic area adaptation
- Threshold-driven horizontal scaling
- Hierarchical overflow prevents cascade failures

---

## Paper 2: Batch as a Service (BaaS)

### Key Components

**ProtocolConverter** - UDP-to-TCP conversion
- Optimal batch size: 100-200 packets
- Batch timeout: <100ms for low latency
- Achieves 73% reduction in cloud traffic

**BatchProcessor** - Privacy-preserving mechanisms
- Differential privacy: ε=1.0, maintains 98.7% data utility
- Homomorphic encryption: <15% overhead for statistical operations
- Quantum-safe protocols: CRYSTALS-Kyber, CRYSTALS-Dilithium

### Usage Example

```java
// Convert UDP packets to TCP batches
ProtocolConverter converter = new ProtocolConverter();
UdpPacket packet = new UdpPacket("device123", sensorData);
TcpBatch batch = converter.convertUdpToTcp(packet);

// Process with differential privacy
BatchProcessor processor = new BatchProcessor();
ProcessedBatch result = processor.processBatchWithPrivacy(batch);
System.out.println("Data utility: " + result.getDataUtility()); // ~0.987

// Or with homomorphic encryption
ProcessedBatch encrypted = processor.processBatchWithHomomorphicEncryption(batch);
System.out.println("Encryption overhead: " + encrypted.getEncryptionOverhead()); // <15%
```

### Expected Results (from Paper 2)
- 73% cloud traffic reduction
- 45% latency improvement
- 99.8% reliability
- 38% energy savings
- 85% cache hit rate

---

## Paper 3: Adaptive Fog Computing Architecture

### Key Components

**LocationService** - Location-aware fog orchestration
- Adaptive deployment: migrates cloud→fog when density increases
- Device classification: STATIC, DYNAMIC, PREDICTABLE
- Query frequency adaptation based on device speed

### Usage Example

```java
LocationService locationService = new LocationService();

// Register fog nodes with geographic areas
GeographicArea area1 = new GeographicArea(40.7128, -74.0060, 5.0);
locationService.registerFogNode(fogNode1, area1);

// Update device locations
locationService.updateDeviceLocation("sensor123", 40.7150, -74.0070, DeviceType.STATIC);
locationService.updateDeviceLocation("vehicle456", 40.7200, -74.0100, DeviceType.DYNAMIC);

// Find responsible fog node
FogDevice responsible = locationService.findFogNode(40.7150, -74.0070);

// Query frequency for dynamic devices (speed in km/h)
int queryFreq = locationService.getQueryFrequency(DeviceType.DYNAMIC, 60); // Moving at 60 km/h
```

### Adaptive Deployment Rules
- Deploy in **cloud** if: devices < 100 AND queries < 1000/min
- Deploy in **fog** if: devices ≥ 100 OR queries ≥ 1000/min

---

## Paper 4: Federated Multi-Cluster with DANE/DANCE

### Key Components

**MasterNode** - Cluster-level coordination
- Manages local fog nodes
- Coordinates with neighbor clusters
- Implements federation protocol with 30% sharing limit

### Usage Example

```java
// Create master node
MasterNode master = new MasterNode("Cluster_A");
master.addLocalNode(fogNode1);
master.addLocalNode(fogNode2);

// Add neighbor for federation
MasterNode neighborMaster = new MasterNode("Cluster_B");
master.addNeighbor(neighborMaster);

// Handle overflow
OverflowRequest request = new OverflowRequest("req123", 0.05, 1);
FederationDecision decision = master.handleOverflow(request);
System.out.println(decision.getReason());
```

### Expected Results (from Paper 4)
- Scalability: 50,000+ devices per deployment
- Cache hit rate: 85%
- Federation latency: 35-45ms

---

## Paper 5: Dynamic Inter-Cluster Resource Sharing

### Key Components

**ResourceSharingManager** - Two sharing modes
1. **Request Redirection**: <50ms negotiation, for temporary spikes
2. **Node Migration**: 30-60s migration time, for sustained imbalances

### Usage Example

```java
ResourceSharingManager sharingMgr = new ResourceSharingManager(masterNode);

// Request redirection (fast, adds latency)
ResourceSharingManager.Request request = new ResourceSharingManager.Request("req123", 1);
RedirectionResult result = sharingMgr.redirectRequest(request, neighborMaster);
System.out.println("Negotiation time: " + result.getNegotiationTimeMs() + "ms"); // <50ms

// Node migration (slow, no latency after migration)
ResourceSharingManager.Cluster targetCluster = new ResourceSharingManager.Cluster("Cluster_B");
MigrationResult migration = sharingMgr.migrateNode(fogNode, targetCluster);
System.out.println("Migration time: " + migration.getMigrationTimeMs()/1000 + "s"); // 30-60s
```

### Migration Checkpoint Protocol
- Finish requests with <2s remaining processing time
- Forward pending requests to other nodes
- Reduces request drops from 12% → 1%

### Expected Results (from Paper 5)
- 68% reduction in request dropping
- 42% resource utilization improvement
- 99.2% system-wide availability
- 30% sharing limit prevents cascade failures

---

## Paper 6: Hierarchical Federated Multi-Cluster

### Key Components

**MasterOfMasters** - Global coordination
- Master-slave fault tolerance
- Sleeping node management
- Supports 1,000,000+ devices

### Usage Example

```java
// Create global master with fault tolerance
MasterOfMasters globalMaster = new MasterOfMasters(true);
MasterOfMasters slaveMaster = new MasterOfMasters(false);
globalMaster.setSlaveMaster(slaveMaster);

// Register regional masters
MasterNode regional1 = new MasterNode("Region_A");
globalMaster.registerRegionalMaster(regional1);

// Register sleeping node pool (3-7 nodes per cluster)
List<SleepingNode> sleepingPool = new ArrayList<>();
sleepingPool.add(new SleepingNode(sleepingFogNode1));
sleepingPool.add(new SleepingNode(sleepingFogNode2));
globalMaster.registerSleepingNodePool("Region_A", sleepingPool);

// Sleeping node activation rules
LoadMetrics load = new LoadMetrics(0.92, 0.85, 0.78);
globalMaster.manageSleepingNodes("Region_A", load);
// If load > 85%: instant activation in 30-60s
// If forecast > 70%: predictive activation
// If load < 30% for >10min: deactivation for energy savings

// Handle master failure
FailoverResult failover = globalMaster.handleMasterFailure(failedMaster);
System.out.println("Failover time: " + failover.getFailoverTimeMs() + "ms"); // Target: <2.3s
```

### Sleeping Node Management Rules
1. **Instant Activation**: Load > 85% → activate in 30-60s
2. **Predictive Activation**: Forecast > 70% → schedule activation
3. **Deactivation**: Load < 30% for >10min → sleep mode

### Expected Results (from Paper 6)
- Device capacity: 1,000,000+
- Availability: 99.95%
- Failover time: <2.3 seconds
- Energy reduction: 38% (sleeping nodes)

---

## Running the Smart City Traffic Scenario

The included scenario tests concepts from all 6 papers.

### Quick Start (Recommended)

Use the provided shell scripts from the project root:

```bash
# Compile all adaptive packages
./compile_adaptive.sh

# Run the scenario
./run_scenario.sh
```

### Manual Compilation

If you prefer to compile manually:

```bash
# Compile
javac -d out -cp "jars/*:jars/commons-math3-3.5/*:src:out" src/org/fog/adaptive/**/*.java

# Run
java -cp "jars/*:jars/commons-math3-3.5/*:out" org.fog.adaptive.scenarios.SmartCityTrafficScenario
```

### Scenario Configuration
- **Intersections**: 400 monitoring points
- **Fog Clusters**: 4 (with 5 fog nodes + 3 sleeping nodes each)
- **Load Distribution**: 80% in hotspot region (Cluster 0), 20% distributed
- **Update Interval**: 10 seconds per intersection
- **Simulation Duration**: 2 hours

### Expected Output
```
========================================
Smart City Traffic Management Scenario
Paper 2: BaaS Framework Testing
========================================

[Setup] Creating fog computing infrastructure...
[Setup] Created Master-of-Masters with fault tolerance
[Setup] Created 4 fog clusters with 400 intersection sensors

[Simulation] Starting traffic monitoring simulation...
[Time 600s] Simulation progress...
[Time 1200s] Simulation progress...
...

========================================
Simulation Results
========================================

Expected Results (from Paper 2):
- Bandwidth reduction: 78%
- Intersection wait time improvement: 15%
- Reliability: 99.8%
- Energy savings: 38%
- Cache hit rate: 85%
```

---

## Key Algorithms Implemented

### Algorithm 1: DynamicAreaAdjustment (Paper 1)
```
Input: node, currentLoad, requestRate
Output: updatedBoundaries

IF currentLoad > THRESHOLD_HIGH (0.85) AND area > AREA_MIN THEN
    newArea ← area × CONTRACTION_FACTOR (0.8)
    redistributeBoundaries(newArea)
ELSE IF currentLoad < THRESHOLD_LOW (0.60) AND area < AREA_MAX THEN
    newArea ← area × EXPANSION_FACTOR (1.2)
    redistributeBoundaries(newArea)
ELSE IF currentLoad > THRESHOLD_CRITICAL (0.95) AND area ≤ AREA_MIN THEN
    triggerScalingProcedure()
```

### Algorithm 2: Master Node Federation (Paper 5)
```
Input: overflow_request, local_cluster, neighbors
Output: ResourceSharingDecision

IF canAssignLocally(cluster) THEN
    RETURN AssignToLocalNode()
FOR EACH neighbor IN SortByPriority(neighbors) DO
    capacity ← QueryCapacity(neighbor)
    IF capacity > THRESHOLD AND sharingCapacity < 30% THEN
        IF NegotiateDelegation(neighbor) THEN
            RETURN DelegateToNeighbor(neighbor)
RETURN QueueRequest(cluster)

Priority = 0.4×Reciprocity + 0.3×Criticality + 0.2×Capacity + 0.1×Proximity
```

### Load Calculation (All Papers)
```
Load = 0.4 × CPU + 0.3 × Queue + 0.3 × Memory
```

---

## Performance Metrics Collection

Create a metrics collector to track all results:

```java
public class MetricsCollector {
    // Paper 1
    private int requestDropCount;
    private int areaAdjustmentCount;

    // Paper 2
    private double bandwidthReduction; // Target: 73%
    private double latencyImprovement; // Target: 45%
    private double reliability; // Target: 99.8%
    private double energySavings; // Target: 38%
    private double cacheHitRate; // Target: 85%

    // Paper 5
    private int redirectionCount;
    private int migrationCount;
    private double resourceUtilization; // Target: +42%

    // Paper 6
    private long failoverTime; // Target: <2.3s
    private int sleepingNodeActivations;
}
```

---

## Integration with iFogSim

This framework extends iFogSim's base functionality:

1. **Extends FogDevice**: Location-aware fog nodes with dynamic areas
2. **Extends Broker**: Intelligent request routing based on location
3. **New Policies**: Adaptive placement policies for mobile IoT
4. **Custom Schedulers**: Priority-based scheduling for federated requests

### Example Integration

```java
// Create traditional iFogSim fog device
FogDevice fogDevice = new FogDevice("FogNode1", 4000, 8192, 10000, 270, 0, 0, 0, 0);

// Enhance with adaptive capabilities
GeographicArea area = new GeographicArea(40.7128, -74.0060, 10.0);
DynamicAreaManager areaManager = new DynamicAreaManager(fogDevice, area);

// Add to master node for federation
MasterNode master = new MasterNode("Cluster1");
master.addLocalNode(fogDevice);

// Now the fog device has:
// - Dynamic area management
// - Federation capabilities
// - Resource sharing abilities
```

---

## Future Enhancements

Based on the papers' future work sections:

1. **Machine Learning Integration** (Paper 6)
   - Predictive load forecasting using EWMA + pattern matching
   - 89% accuracy in predicting load spikes 5-10 minutes ahead

2. **Multi-Tier Resource Sharing** (Paper 5)
   - Extend beyond 2-cluster federation
   - Multi-hop resource delegation

3. **Enhanced Security** (Paper 2)
   - Full DANE/DANCE implementation
   - Quantum-safe key distribution

4. **Application-Aware QoS** (All papers)
   - Different SLA levels for different applications
   - Priority-based resource allocation

---

## Citation

If you use this implementation in your research, please cite the relevant papers:

```bibtex
@inproceedings{naltakyan2024loadbalancing,
  author = {Naltakyan, Narek},
  title = {Load Balancing in Adaptive Fog Computing: Research Problems and Solutions Framework},
  booktitle = {Proc. CSIT Conference},
  year = {2024}
}

@article{naltakyan2025baas,
  author = {Minasyan, H.D. and Naltakyan, N.L.},
  title = {Batch as a Service with Enhanced Security for IoT-Enabled Smart Cities},
  journal = {Computer Science and Informatics},
  year = {2025}
}
```

---

## Contact

**Narek Naltakyan**
National Polytechnic University of Armenia
Email: nareknaltakyan1@gmail.com

---

## License

MIT License - See LICENSE.txt in the main iFogSim directory
