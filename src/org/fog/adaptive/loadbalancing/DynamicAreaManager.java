package org.fog.adaptive.loadbalancing;

import org.fog.adaptive.models.GeographicArea;
import org.fog.adaptive.models.LoadMetrics;
import org.fog.entities.FogDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Area Management - Paper 1: Load Balancing in Adaptive Fog Computing
 *
 * Implements Algorithm 1: DynamicAreaAdjustment
 * - Dynamic area resizing based on load
 * - Threshold-driven scaling
 * - Hierarchical overflow management
 *
 * @author Narek Naltakyan
 */
public class DynamicAreaManager {

    // Thresholds from Paper 1
    private static final double THRESHOLD_HIGH = 0.85;
    private static final double THRESHOLD_LOW = 0.60;
    private static final double THRESHOLD_CRITICAL = 0.95;

    // Area adjustment factors from Paper 1
    private static final double CONTRACTION_FACTOR = 0.8;
    private static final double EXPANSION_FACTOR = 1.2;

    private FogDevice fogNode;
    private GeographicArea currentArea;
    private LoadMetrics previousLoad;
    private List<AreaAdjustmentListener> listeners;

    public DynamicAreaManager(FogDevice node, GeographicArea initialArea) {
        this.fogNode = node;
        this.currentArea = initialArea;
        this.listeners = new ArrayList<>();
    }

    /**
     * Main algorithm from Paper 1, Section III
     * Algorithm 1: DynamicAreaAdjustment(node, currentLoad, requestRate)
     */
    public GeographicArea adjustArea(LoadMetrics currentLoad, int requestRate) {
        double load = currentLoad.calculateLoad();

        // Step 1: Calculate optimal area
        calculateOptimalArea(currentLoad);

        // Step 2: IF currentLoad > THRESHOLD_HIGH AND area > AREA_MIN
        if (load > THRESHOLD_HIGH && !currentArea.isMinimumSize()) {
            // Contract area
            double newRadius = currentArea.getRadiusKm() * CONTRACTION_FACTOR;
            currentArea.contract(CONTRACTION_FACTOR);
            redistributeBoundaries(currentArea);
            notifyAffectedDevices();

            System.out.println(String.format(
                "[DynamicAreaManager] Load %.2f > THRESHOLD_HIGH: Contracting area to %.2f km",
                load, currentArea.getRadiusKm()));
        }
        // Step 3: ELSE IF currentLoad < THRESHOLD_LOW AND area < AREA_MAX
        else if (load < THRESHOLD_LOW && !currentArea.isMaximumSize()) {
            currentArea.expand(EXPANSION_FACTOR);
            redistributeBoundaries(currentArea);
            notifyAffectedDevices();

            System.out.println(String.format(
                "[DynamicAreaManager] Load %.2f < THRESHOLD_LOW: Expanding area to %.2f km",
                load, currentArea.getRadiusKm()));
        }
        // Step 4: ELSE IF currentLoad > THRESHOLD_CRITICAL AND area <= AREA_MIN
        else if (load > THRESHOLD_CRITICAL && currentArea.isMinimumSize()) {
            triggerScalingProcedure();

            System.out.println(String.format(
                "[DynamicAreaManager] Load %.2f > THRESHOLD_CRITICAL at minimum area: Triggering scaling",
                load));
        }

        previousLoad = currentLoad.copy();
        return currentArea;
    }

    /**
     * Calculate optimal area based on load metrics
     */
    private void calculateOptimalArea(LoadMetrics load) {
        // Can be extended with more sophisticated calculation
        // For now, the threshold-based approach is used
    }

    /**
     * Redistribute boundaries with neighboring fog nodes
     */
    private void redistributeBoundaries(GeographicArea newArea) {
        // Notify neighbors about boundary changes
        for (AreaAdjustmentListener listener : listeners) {
            listener.onAreaChanged(fogNode, currentArea);
        }
    }

    /**
     * Notify IoT devices affected by area change
     */
    private void notifyAffectedDevices() {
        // Devices at boundary may need to switch fog nodes
        for (AreaAdjustmentListener listener : listeners) {
            listener.onDevicesAffected(fogNode, currentArea);
        }
    }

    /**
     * Trigger horizontal scaling - deploy load balancer and add fog node
     * Paper 1: Section 1.2 Research Contributions - Strategy 2
     */
    private void triggerScalingProcedure() {
        for (AreaAdjustmentListener listener : listeners) {
            listener.onScalingRequired(fogNode, currentArea);
        }
    }

    /**
     * Check if area can expand
     */
    public boolean canExpand() {
        return !currentArea.isMaximumSize();
    }

    /**
     * Check if area can contract
     */
    public boolean canContract() {
        return !currentArea.isMinimumSize();
    }

    /**
     * Get current area utilization percentage
     */
    public double getAreaUtilization() {
        double currentSize = currentArea.getSize();
        double maxSize = Math.PI * currentArea.getMaxRadiusKm() * currentArea.getMaxRadiusKm();
        return currentSize / maxSize;
    }

    public void addListener(AreaAdjustmentListener listener) {
        listeners.add(listener);
    }

    public GeographicArea getCurrentArea() {
        return currentArea;
    }

    public FogDevice getFogNode() {
        return fogNode;
    }

    /**
     * Listener interface for area adjustment events
     */
    public interface AreaAdjustmentListener {
        void onAreaChanged(FogDevice node, GeographicArea newArea);
        void onDevicesAffected(FogDevice node, GeographicArea newArea);
        void onScalingRequired(FogDevice node, GeographicArea currentArea);
    }
}
