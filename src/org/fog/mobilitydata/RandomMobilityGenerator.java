package org.fog.mobilitydata;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//import  java.awt.geom.
import org.fog.mobilitydata.Polygon2D;

import java.io.*;
import java.util.*;


/**
 * @author Mohammad Goudarzi
 */
public class RandomMobilityGenerator {
    protected Map<Integer, List<Double>> mobilityPositions;
    protected Map<Integer, Double> mobilityPositionsPauseTime;
    protected Map<Integer, Double> mobilityPositionsAngle;
    protected Map<Integer, Double> mobilityPositionsSpeed;
    double speed;
    double angle;
    double pauseTime;
    boolean directionFlag;
    JSONArray mobilitySpecJSON;

    public RandomMobilityGenerator() {
        mobilityPositions = new HashMap<>();  // the list of integer contatins the X and Y of one node.
        mobilityPositionsPauseTime = new HashMap<>(); // it shows the pause time of mobile user in each geographical point
        mobilityPositionsAngle = new HashMap<>(); //it shows the direction of the move for the next period of the time
        mobilityPositionsSpeed = new HashMap<>(); //it shows the speed of the move for the next period of the time
        mobilitySpecJSON = new JSONArray();
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    private static boolean positionInRangeCheck(float x, float y) {
        float topLeftXEnv = -37.813046f;
        float topLeftYEnv = 144.951380f;

        float downLeftXEnv = -37.821229f;
        float downLeftYEnv = 144.955039f;


        float topRightXEnv = -37.807397f;
        float topRightYEnv = 144.971062f;

        float downRightXEnv = -37.815136f;
        float downRightYEnv = 144.975044f;

        final Polygon2D polygon = new Polygon2D();
        polygon.addPoint(topLeftXEnv, topLeftYEnv);
        polygon.addPoint(downLeftXEnv, downLeftYEnv);
        polygon.addPoint(topRightXEnv, topRightYEnv);
        polygon.addPoint(downRightXEnv, downRightYEnv);

        if (polygon.contains(x, y)) {
            return true;
        } else {
            return false;
        }


    }

    public void createRandomData(int mobilityModel, int user_index, String datasetReference, boolean renewDataset) throws IOException, ParseException {
        // To check different mobility models, if you applied other mobility models, they can be customized here
        String fileName = References.dataset_random + user_index + ".csv";
        File tmpDir = new File(fileName);
        boolean exists = tmpDir.exists();
        if (exists && renewDataset) {
            System.out.println("The dataset: " + fileName + " is being overwritten.");
            if (mobilityModel == References.random_walk_mobility_model) {
                MobilityPositionInitiator(References.random_walk_mobility_model, 100, user_index);
            } else if (mobilityModel == References.random_waypoint_mobility_model) {
                MobilityPositionInitiator(References.random_waypoint_mobility_model, 100, user_index);
            }
        } else if (!exists) {
            System.out.println("The dataset: " + fileName + " is going to be created for the first time.");
            if (mobilityModel == References.random_walk_mobility_model) {
                MobilityPositionInitiator(References.random_walk_mobility_model, 100, user_index);
            } else if (mobilityModel == References.random_waypoint_mobility_model) {
                MobilityPositionInitiator(References.random_waypoint_mobility_model, 100, user_index);
            }
        } else {
            System.out.println("The dataset: " + fileName + " exists already.");
            // DO NOTHING
        }


    }

    public void MobilityPositionInitiator(int mobilityModel, int numberOfPositions, int user_index) throws IOException,
            ParseException, org.json.simple.parser.ParseException {
        this.mobilityPositions.clear();
        this.mobilityPositionsPauseTime.clear();
        this.mobilityPositionsAngle.clear();
        this.mobilityPositionsSpeed.clear();
        this.mobilitySpecJSON.clear();
        Random r = new Random();

        boolean file = false;


        if (file == false) {
            List<ArrayList<Double>> tempPositions = new ArrayList<ArrayList<Double>>();
            tempPositions.add(new ArrayList<Double>());
            double positionX = References.lat_reference;
            double positionY = References.long_reference;
            tempPositions.get(0).add(positionX);
            tempPositions.get(0).add(positionY);

            this.angle = getRandomNumberInRange(0, 259);
            directionFlag = true;
            int index = 1;
            int tempIndex = 0;

            this.mobilityPositionsPauseTime.put(0, 0.0);
            this.mobilityPositionsAngle.put(0, angle);
            while (tempIndex < numberOfPositions) {
                int pause_time_multiplier = 3;
                this.mobilityPositionsPauseTime.put(tempIndex, r.nextDouble() * pause_time_multiplier);
                tempIndex++;

            }
            this.mobilityPositions.put(0, tempPositions.get(0));
            JSONObject obj = new JSONObject();
            obj.put("index", 0);
            obj.put("positionX", positionX);
            obj.put("positionY", positionY);

            this.mobilitySpecJSON.add(obj);
            while (index < numberOfPositions) {
                if (this.directionFlag == false || mobilityModel == References.random_walk_mobility_model) {
                    this.angle = getRandomNumberInRange(0, 259); // Random direction.
                    this.directionFlag = true;
                }
                double mobilitySpeed = (double) (getRandomNumberInRange((int) References.MinMobilitySpeed * 100,
                        (int) References.MaxMobilitySpeed * 100)) / 100; // meter/seconds
                tempPositions.add(new ArrayList<Double>());

                //positionX = positionX + (double) (Math.cos(Math.toRadians(angle)) * speed) * (time - mobilityPositionsPauseTime.get(index - 1));
                //positionY = positionY + (double) (Math.sin(Math.toRadians(angle)) * speed) * (time - mobilityPositionsPauseTime.get(index - 1));
                double tempPositionX = positionX;
                double tempPositionY = positionY;
                positionX = positionX + (double) (Math.cos(Math.toRadians(this.angle)) * mobilitySpeed) / 1000; // divided by 1000 to change the values to KM
                positionY = positionY + (double) (Math.sin(Math.toRadians(this.angle)) * mobilitySpeed) / 1000; // divided by 1000 to change the values to KM

                if (positionX < -References.environmentLimit) {
                    positionX = -References.environmentLimit;
                    this.directionFlag = false;
                    continue;
                } else if (positionX > References.environmentLimit) {
                    positionX = References.environmentLimit;
                    this.directionFlag = false;
                    continue;
                }

                if (positionY < -References.environmentLimit) {
                    positionY = -References.environmentLimit;
                    this.directionFlag = false;
                    continue;
                } else if (positionY > References.environmentLimit) {
                    positionY = References.environmentLimit;
                    this.directionFlag = false;
                    continue;
                }

                if (!positionInRangeCheck((float) positionX, (float) positionY)) {
                    System.out.println("positionX: " + positionX + " positionY: " + positionX + " are out of environment bound....going to fix it");
                    positionX = tempPositionX;
                    positionY = tempPositionY;
                }


                tempPositions.get(index).add(positionX);
                tempPositions.get(index).add(positionY);
                this.mobilityPositions.put(index, tempPositions.get(index));
                this.mobilityPositionsAngle.put(index, this.angle);
                this.mobilityPositionsSpeed.put(index, mobilitySpeed);
                JSONObject obj1 = new JSONObject();
                obj1.put("index", index);
                obj1.put("positionX", positionX);
                obj1.put("positionY", positionY);
                this.mobilitySpecJSON.add(obj1);

                index++;
            }

            // File input path
            System.out.println("Starting Writing Mobile User Information ...");

            try (PrintWriter writer = new PrintWriter(new File(References.dataset_random + user_index + ".csv"))) {
                StringBuilder sb = new StringBuilder();
                sb.append("Latitude");
                sb.append(',');
                sb.append("Longitude");
                sb.append('\n');
                writer.write(sb.toString());
                sb.setLength(0); // clear stringbuilder
                for (int i = 0; i < this.mobilityPositions.size(); i++) {
                    sb.append(this.mobilityPositions.get(i).get(0));
                    sb.append(',');
                    sb.append(this.mobilityPositions.get(i).get(1));
                    sb.append('\n');
                    writer.write(sb.toString());
                    sb.setLength(0); // clear stringbuilder
                }

                writer.close();
                System.out.println("done!");

            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
            }

            System.out.println("Finished Writing Mobile User Information ...");

        }


    }

}
