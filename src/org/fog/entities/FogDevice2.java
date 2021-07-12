package org.fog.entities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.fog.mobilitydata.Clustering;
import org.fog.utils.FogEvents;
import org.fog.utils.NetworkUsageMonitor;
import org.json.simple.JSONObject;

import java.util.*;

public class FogDevice2 extends FogDevice {
    protected List<Integer> clusterMembers = new ArrayList<Integer>();
    protected boolean isInCluster = false;
    protected boolean selfCluster = false; // IF there is only one fog device in one cluster without any sibling
    protected Map<Integer, Double> clusterMembersToLatencyMap; // latency to other cluster members

    protected Queue<Pair<Tuple, Integer>> clusterTupleQueue;// tuple and destination cluster device ID
    protected boolean isClusterLinkBusy; //Flag denoting whether the link connecting to cluster from this FogDevice is busy
    protected double clusterLinkBandwidth;


    public FogDevice2(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
        clusterTupleQueue = new LinkedList<>();
        setClusterLinkBusy(false);
    }

    public FogDevice2(String name, long mips, int ram, double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
        super(name, mips, ram, uplinkBandwidth, downlinkBandwidth, ratePerMips, powerModel);
        clusterTupleQueue = new LinkedList<>();
        setClusterLinkBusy(false);
    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE:
                updateClusterTupleQueue();
                break;
            case FogEvents.START_DYNAMIC_CLUSTERING:
                //This message is received by the devices to start their clustering
                processClustering(this.getParentId(), this.getId(), ev);
                break;
            default:
                super.processOtherEvent(ev);
                break;
        }
    }

    public void setClusterMembers(List clusterList) {
        this.clusterMembers = clusterList;
    }

    public void addClusterMember(int clusterMemberId) {
        this.clusterMembers.add(clusterMemberId);
    }

    public List<Integer> getClusterMembers() {
        return this.clusterMembers;
    }

    public void setIsInCluster(Boolean bool) {
        this.isInCluster = bool;
    }

    public void setSelfCluster(Boolean bool) {
        this.selfCluster = bool;
    }

    public Boolean getIsInCluster() {
        return this.isInCluster;
    }

    public Boolean getSelfCluster() {
        return this.selfCluster;
    }

    public void setClusterMembersToLatencyMap(Map<Integer, Double> clusterMembersToLatencyMap) {
        this.clusterMembersToLatencyMap = clusterMembersToLatencyMap;
    }

    public Map<Integer, Double> getClusterMembersToLatencyMap() {
        return this.clusterMembersToLatencyMap;
    }

    private void processClustering(int parentId, int nodeId, SimEvent ev) {
        JSONObject objectLocator = (JSONObject) ev.getData();
        Clustering cms = new Clustering();
        cms.createClusterMembers(this.getParentId(), this.getId(), objectLocator);
    }

    public double getClusterLinkBandwidth() {
        return clusterLinkBandwidth;
    }

    protected void setClusterLinkBandwidth(double clusterLinkBandwidth) {
        this.clusterLinkBandwidth = clusterLinkBandwidth;
    }

    protected void sendToCluster(Tuple tuple, int clusterNodeID) {
        if (getClusterMembers().contains(clusterNodeID)) {
            if (!isClusterLinkBusy) {
                sendThroughFreeClusterLink(tuple, clusterNodeID);
            } else {
                clusterTupleQueue.add(new Pair<Tuple, Integer>(tuple, clusterNodeID));
            }
        }
    }

    private void updateClusterTupleQueue() {
        if (!getClusterTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getClusterTupleQueue().poll();
            sendThroughFreeClusterLink(pair.getFirst(), pair.getSecond());
        } else {
            setClusterLinkBusy(false);
        }
    }

    private void sendThroughFreeClusterLink(Tuple tuple, Integer clusterNodeID) {
        double networkDelay = tuple.getCloudletFileSize() / getClusterLinkBandwidth();
        setClusterLinkBusy(true);
        double latency = (getClusterMembersToLatencyMap()).get(clusterNodeID);
        send(getId(), networkDelay, FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE);
        send(clusterNodeID, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
    }

    protected void setClusterLinkBusy(boolean busy) {
        this.isClusterLinkBusy = busy;
    }

    public Queue<Pair<Tuple, Integer>> getClusterTupleQueue() {
        return clusterTupleQueue;
    }
}