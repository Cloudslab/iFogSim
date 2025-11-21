package org.fog.mobilitydata;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class References {

    private static final String DATASET_OVERRIDE_PROPERTY = "ifogsim.dataset.dir";
    private static final String DATASET_FOLDER = "dataset";

    public static final int NOT_SET = -1;
    public static final double SETUP_TIME = -1.00;
    public static final double INIT_TIME = 0.00;

    public static final int DIRECTIONAL_MOBILITY = 1;
    public static final int RANDOM_MOBILITY = 2;

    // Reference geographical information to create random mobility pattern for mobile users
    public static final double lat_reference = -37.81349283433532;
    public static final double long_reference = 144.952370512958;

    // Reference dataset filename to store and retrieve users positions
    // ".\\dataset\\usersLocation-melbCBD_"
    // ".\\dataset\\usersLocation-melbCBD-random_
    public static final String dataset_reference;
    public static final String dataset_random;
    private static final String datasetBasePath;

    public static final int random_walk_mobility_model = 1;
    public static final int random_waypoint_mobility_model = 2;
    public static double MinMobilitySpeed = 1; //
    public static double MaxMobilitySpeed = 2; //
    public static double environmentLimit = 6371; // shows the maximum latitude and longitude of the environment. Currently it is set based on radius of the Earth (6371 KM)

    static {
        datasetBasePath = resolveDatasetBasePath();
        dataset_reference = datasetBasePath + "usersLocation-melbCBD_";
        dataset_random = datasetBasePath + "random_usersLocation-melbCBD_";
    }

    public static String getDatasetBasePath() {
        return datasetBasePath;
    }

    private static String resolveDatasetBasePath() {
        String overridePath = System.getProperty(DATASET_OVERRIDE_PROPERTY);
        if (overridePath != null && !overridePath.isEmpty()) {
            Path overridden = Paths.get(overridePath.trim());
            if (!overridden.isAbsolute()) {
                overridden = overridden.toAbsolutePath();
            }
            ensureDirectoryExists(overridden);
            return withTrailingSeparator(overridden);
        }

        URL datasetUrl = References.class.getClassLoader().getResource(DATASET_FOLDER);
        if (datasetUrl != null && "file".equalsIgnoreCase(datasetUrl.getProtocol())) {
            try {
                Path candidate = Paths.get(datasetUrl.toURI());
                ensureDirectoryExists(candidate);
                return withTrailingSeparator(candidate);
            } catch (Exception ignored) {
                // Fallback to default path below
            }
        }

        Path defaultPath = Paths.get("." + File.separator + DATASET_FOLDER).toAbsolutePath();
        ensureDirectoryExists(defaultPath);
        return withTrailingSeparator(defaultPath);
    }

    private static void ensureDirectoryExists(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException ignored) {
                // Ignore failures; caller will still get the unresolved path
            }
        }
    }

    private static String withTrailingSeparator(Path path) {
        String asString = path.toString();
        if (!asString.endsWith(File.separator)) {
            asString += File.separator;
        }
        return asString;
    }
}
