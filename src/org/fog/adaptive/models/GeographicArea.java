package org.fog.adaptive.models;

/**
 * Represents a geographic area for fog node responsibility
 * Used in Papers 1, 3, 5
 *
 * @author Narek Naltakyan
 */
public class GeographicArea {
    private double centerLatitude;
    private double centerLongitude;
    private double radiusKm;
    private double minRadiusKm;
    private double maxRadiusKm;

    public GeographicArea(double centerLat, double centerLon, double radiusKm) {
        this.centerLatitude = centerLat;
        this.centerLongitude = centerLon;
        this.radiusKm = radiusKm;
        this.minRadiusKm = radiusKm * 0.3; // Can shrink to 30% of original
        this.maxRadiusKm = radiusKm * 2.0; // Can expand to 200% of original
    }

    public GeographicArea(double centerLat, double centerLon, double radiusKm,
                         double minRadius, double maxRadius) {
        this.centerLatitude = centerLat;
        this.centerLongitude = centerLon;
        this.radiusKm = radiusKm;
        this.minRadiusKm = minRadius;
        this.maxRadiusKm = maxRadius;
    }

    /**
     * Check if a device location is within this area
     */
    public boolean contains(double latitude, double longitude) {
        double distance = calculateDistance(centerLatitude, centerLongitude,
                                           latitude, longitude);
        return distance <= radiusKm;
    }

    /**
     * Calculate distance using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Contract area by given factor (Paper 1: Area contraction)
     */
    public void contract(double factor) {
        double newRadius = radiusKm * factor;
        if (newRadius >= minRadiusKm) {
            radiusKm = newRadius;
        } else {
            radiusKm = minRadiusKm;
        }
    }

    /**
     * Expand area by given factor (Paper 1: Area expansion)
     */
    public void expand(double factor) {
        double newRadius = radiusKm * factor;
        if (newRadius <= maxRadiusKm) {
            radiusKm = newRadius;
        } else {
            radiusKm = maxRadiusKm;
        }
    }

    public boolean isMinimumSize() {
        return Math.abs(radiusKm - minRadiusKm) < 0.01;
    }

    public boolean isMaximumSize() {
        return Math.abs(radiusKm - maxRadiusKm) < 0.01;
    }

    public double getSize() {
        return Math.PI * radiusKm * radiusKm; // Area in km²
    }

    // Getters and setters
    public double getCenterLatitude() { return centerLatitude; }
    public double getCenterLongitude() { return centerLongitude; }
    public double getRadiusKm() { return radiusKm; }
    public double getMinRadiusKm() { return minRadiusKm; }
    public double getMaxRadiusKm() { return maxRadiusKm; }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    @Override
    public String toString() {
        return String.format("GeographicArea[center=(%.4f, %.4f), radius=%.2f km, size=%.2f km²]",
                           centerLatitude, centerLongitude, radiusKm, getSize());
    }
}
