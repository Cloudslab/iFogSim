package org.fog.utils;

import org.apache.commons.math3.util.Pair;
import org.fog.entities.FogDevice;
import org.fog.entities.MicroserviceFogDevice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Samodha Pallewatta on 6/18/2021.
 * Creates a routing table considering shortest path between devices.
 */
public class ShortestPathRoutingGenerator {

    public static Map<Integer, Map<Integer, Integer>> generateRoutingTable(List<FogDevice> fogDevices) {
        // <source device id>  ->  <dest device id,next device to route to>
        Map<Integer, Map<Integer, Integer>> routing = new HashMap<>();
        Map<String, Map<String, String>> routingString = new HashMap<>();
        int size = fogDevices.size();

        int[][] routingMatrix = new int[size][size];
        double[][] distanceMatrix = new double[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                routingMatrix[row][column] = -1;
                distanceMatrix[row][column] = -1;
            }
        }

        boolean change = true;
        boolean firstIteration = true;
        while (change || firstIteration) {
            change = false;
            for (int row = 0; row < size; row++) {
                for (int column = 0; column < size; column++) {
                    double dist = distanceMatrix[row][column];
                    FogDevice rFog = fogDevices.get(row);
                    FogDevice cFog = fogDevices.get(column);
                    if (firstIteration && dist < 0) {
                        if (row == column) {
                            dist = 0;
                        } else {
                            dist = directlyConnectedDist(rFog, cFog);
                        }
                        if (dist >= 0) {
                            change = true;
                            distanceMatrix[row][column] = dist;
                            distanceMatrix[column][row] = dist;

                            // directly connected
                            routingMatrix[row][column] = cFog.getId();
                            routingMatrix[column][row] = rFog.getId();
                        }
                    }
                    if (dist < 0) {
                        Pair<Double, Integer> result = indirectDist(row, column, size, distanceMatrix);
                        dist = result.getFirst();
                        int mid = result.getSecond();
                        if (dist >= 0) {
                            change = true;
                            distanceMatrix[row][column] = dist;
                            routingMatrix[row][column] = routingMatrix[row][mid];
                        }
                    }
                    if (dist > 0) {
                        Pair<Double, Integer> result = indirectDist(row, column, size, distanceMatrix);
                        double distNew = result.getFirst();
                        int mid = result.getSecond();
                        if (distNew < dist) {
                            change = true;
                            distanceMatrix[row][column] = distNew;
                            routingMatrix[row][column] = routingMatrix[row][mid];
                        }
                    }
                }
            }
            firstIteration = false;
        }

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int sourceId = fogDevices.get(row).getId();
                int destId = fogDevices.get(column).getId();
                if (routing.containsKey(sourceId)) {
                    routing.get(sourceId).put(destId, routingMatrix[row][column]);
                    routingString.get(fogDevices.get(row).getName()).put(fogDevices.get(column).getName(), getFogDeviceById(routingMatrix[row][column], fogDevices).getName());
                } else {
                    Map<Integer, Integer> route = new HashMap<>();
                    route.put(destId, routingMatrix[row][column]);
                    routing.put(sourceId, route);

                    Map<String, String> routeS = new HashMap<>();
                    routeS.put(fogDevices.get(column).getName(), getFogDeviceById(routingMatrix[row][column], fogDevices).getName());
                    routingString.put(fogDevices.get(row).getName(), routeS);
                }
            }
        }

        System.out.println("Routing Table : ");
        for (String deviceName : routingString.keySet()) {
            System.out.println(deviceName + " : " + routingString.get(deviceName).toString());
        }
        System.out.println("\n");

        return routing;
    }


    private static Pair<Double, Integer> indirectDist(int row, int dest, int size, double[][] distanceMatrix) {
        double minDistFromDirectConn = distanceMatrix[row][dest];
        int midPoint = -1;
        for (int column = 0; column < size; column++) {
            if (distanceMatrix[row][column] >= 0 && distanceMatrix[column][dest] >= 0) {
                double totalDist = distanceMatrix[row][column] + distanceMatrix[column][dest];
                if (minDistFromDirectConn >= 0 && totalDist < minDistFromDirectConn) {
                    minDistFromDirectConn = totalDist;
                    midPoint = column;
                } else if (minDistFromDirectConn < 0) {
                    minDistFromDirectConn = totalDist;
                    midPoint = column;
                }
            }
        }
        return new Pair<>(minDistFromDirectConn, midPoint);
    }

    private static double directlyConnectedDist(FogDevice rFog, FogDevice cFog) {
        int parent = rFog.getParentId();
        List<Integer> children = rFog.getChildrenIds();
        List<Integer> cluster = (rFog).getClusterMembers();
        if (cFog.getId() == parent) {
            return rFog.getUplinkLatency();
        } else if (children != null && children.contains(cFog.getId())) {
            return rFog.getChildToLatencyMap().get(cFog.getId());
        } else if (cluster != null && cluster.contains(cFog.getId())) {
            return ((MicroserviceFogDevice) rFog).getClusterMembersToLatencyMap().get(cFog.getId());
        }
        return -1;
    }

    private static FogDevice getFogDeviceById(int id, List<FogDevice> fogDevices) {
        for (FogDevice f : fogDevices) {
            if (f.getId() == id)
                return f;
        }
        return null;
    }
}
