package org.fog.mobilitydata;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.FogDevice;
import org.fog.placement.LocationHandler;
import org.fog.utils.Config;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Mohammad Goudarzi
 */
public class Clustering {
    public void createClusterMembers(int parentId, int nodeId, JSONObject locatorObject) {
        List<Integer> SiblingListIDs = new ArrayList<>();
        List<FogDevice> SiblingsList = new ArrayList<FogDevice>();
        List<Integer> clusterMemberList = new ArrayList<>();
        int fogId = nodeId;
        LocationHandler locatorTemp = new LocationHandler();
        locatorTemp = (LocationHandler) locatorObject.get("locationsInfo");
        FogDevice parentDevice = (FogDevice) CloudSim.getEntity(parentId);
        SiblingListIDs = parentDevice.getChildrenIds();

        if (SiblingListIDs.size() < 1 || SiblingListIDs.isEmpty()) {
            //System.out.println("The node: " + nodeId + " with parent Id: " + parentId + " does not have any cluster members " + parentDevice.getChildrenIds());
            System.out.println("ERROR in clustering --> Pranet Node does not habe any children");
            //Log.printLine("ERROR in clustering --> Pranet Node cannot be Found");
            return;
        }

        for (int i = 0; i < SiblingListIDs.size(); i++) {
            int tempId = SiblingListIDs.get(i);
            FogDevice tempNode = (FogDevice) CloudSim.getEntity(tempId);
            SiblingsList.add(tempNode);
        }

        double fogNodePositionX = locatorTemp.dataObject.resourceLocationData.get(locatorTemp.instanceToDataId.get(fogId)).latitude;
        double fogNodePositionY = locatorTemp.dataObject.resourceLocationData.get(locatorTemp.instanceToDataId.get(fogId)).longitude;
        Location L1 = new Location(fogNodePositionX, fogNodePositionY, 0);
        for (FogDevice fogdevice : SiblingsList) {

            if (fogId == fogdevice.getId()) {
                continue;
            }
            // To check all siblings except itself
            double tempX = locatorTemp.dataObject.resourceLocationData.get(locatorTemp.instanceToDataId.get(fogdevice.getId())).latitude;
            double tempY = locatorTemp.dataObject.resourceLocationData.get(locatorTemp.instanceToDataId.get(fogdevice.getId())).longitude;

            Location L2 = new Location(tempX, tempY, 0);

            boolean clusterCheck = calculateInRange(L1, L2, Config.Node_Communication_RANGE);

            //Clustering Policy
            //double x = Math.pow((fogNodePositionX - tempX), 2) + Math.pow((fogNodePositionY - tempY), 2);

            /*if (Math.sqrt(x) <= Config.Node_Communication_RANGE / 1000) {
                clusterMemberList.add(fogdevice.getId());
            */
            if (clusterCheck == true) {
                clusterMemberList.add(fogdevice.getId());
            }
        }
        // Clustering Policy


        if (clusterMemberList.isEmpty() || clusterMemberList.size() < 1) {
            ((FogDevice) CloudSim.getEntity(fogId)).setSelfCluster(true);
            ((FogDevice) CloudSim.getEntity(fogId)).setIsInCluster(true);
        } else {
            ((FogDevice) CloudSim.getEntity(fogId)).setIsInCluster(true);
            ((FogDevice) CloudSim.getEntity(fogId)).setSelfCluster(false);
            ((FogDevice) CloudSim.getEntity(fogId)).setClusterMembers(clusterMemberList);
            Map<Integer, Double> latencyMapL2 = new HashMap<>();
            for (int id : clusterMemberList) {
                latencyMapL2.put(id, Config.clusteringLatency);
            }
            ((FogDevice) CloudSim.getEntity(fogId)).setClusterMembersToLatencyMap(latencyMapL2);

        }
        System.out.println("The Fog Device: " + locatorTemp.instanceToDataId.get(fogId) + " with id: " + fogId + " and parent id: " + parentId +
                " has these cluster members: " + ((FogDevice) CloudSim.getEntity(fogId)).getClusterMembers());
        return;
    }

    private static boolean calculateInRange(Location loc1, Location loc2, double fogRange) {

        final int R = 6371; // Radius of the earth in Kilometers

        double latDistance = Math.toRadians(loc1.latitude - loc2.latitude);
        double lonDistance = Math.toRadians(loc1.longitude - loc2.longitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(loc1.latitude)) * Math.cos(Math.toRadians(loc2.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // kms


        distance = Math.pow(distance, 2);

        if (Math.sqrt(distance) <= fogRange / 1000) {
            return true;
        } else {
            return false;
        }

    }
}
