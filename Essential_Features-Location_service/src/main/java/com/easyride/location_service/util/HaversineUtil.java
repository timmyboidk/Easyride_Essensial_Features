package com.easyride.location_service.util; // Or your common util package

public class HaversineUtil {
    private static final int EARTH_RADIUS_KM = 6371;

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c * 1000; // Distance in meters
    }

    // Method to find the closest point on a line segment to a given point
    // This is a more complex geometric calculation needed for accurate deviation from a path.
    // For simplicity, we might just check distance to the *nearest point on the planned route list*.
    // A more robust solution would involve projecting the current location onto the route segments.
    public static double perpendicularDistanceToSegment(double pointLat, double pointLon,
                                                        double segStartLat, double segStartLon,
                                                        double segEndLat, double segEndLon) {
        // Simplified: find distance to start and end of segment, take minimum.
        // This is NOT perpendicular distance, but a much simpler approximation.
        // For true perpendicular distance, vector math is needed.
        double distToStart = distance(pointLat, pointLon, segStartLat, segStartLon);
        double distToEnd = distance(pointLat, pointLon, segEndLat, segEndLon);

        // A slightly better approximation: if point is "between" start and end projection-wise,
        // calculate distance to line. This still has edge cases.
        // For a production system, use a proper geospatial library or more detailed math.

        // Let's use a very simple "distance to the closest point in the planned path" for now.
        // The SafetyService will iterate through path points.
        return Math.min(distToStart, distToEnd); // Placeholder, a better approach is needed.
    }
}