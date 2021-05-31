package org.fog.test.qosAwareTests;

import java.io.*;

/**
 * Created by Samodha Pallewatta on 9/22/2020.
 */
public class ResultGenerator {

    public static String resultLocation = "src/org/fog/test/results/";
    public static String testFilePath = "";
    public static String appParamFile;
    public static int placementAlgorithms;
    public static double makespanSatisfaction;
    public static double budgetSatisfaction;
    public static double networkUsage;
    public static StringBuilder powerConsumptionData;
    public static StringBuilder placementData;
    public static long executionTime;
    public static int deviceCount;

    public static double bestLatency;
    public static double bestCost;
    public static double bestNw;
    public static double bestR;
    public static double qosViolation;
    public static double resourceUtilization;

    public static double dncpsoObjest;

    public static void generateResults() throws IOException {
        // make directory
        String directoryPath = resultLocation + System.currentTimeMillis();
        directoryPath = resultLocation + "" + "/" + System.currentTimeMillis();

        File f = new File(directoryPath);
        f.mkdir();

        // copy test file
        File testFile = new File(testFilePath);
        File copyFile = new File(directoryPath + "/test.txt");
        copyFile.createNewFile();
        copyFile(testFile, copyFile);

        //copy application file
        File appFile = new File(appParamFile);
        copyFile = new File(directoryPath + "/appParamFile.txt");
        copyFile.createNewFile();
        copyFile(appFile, copyFile);

        //generate results file and write
        File result = new File(directoryPath + "/results.txt");
        result.createNewFile();
        writeResults(directoryPath + "/results.txt");

        //copy necessary file
        copyNecessaryFiles(directoryPath);


    }

    private static void writeResults(String resultFile) throws IOException {
        FileWriter myWriter = new FileWriter(resultFile);
        myWriter.write("Results  : \n Makespan Satisfaction - " + makespanSatisfaction +
                "\n Budget Satisfaction - " + budgetSatisfaction +
                "\n NetworkUsage - " + networkUsage +
                "\n Energy Consumption : \n" + powerConsumptionData +
                "\n \n " +
                "Placement : \n " + placementData + "\n" +
                "Execution Time : " + executionTime + "\n" +
                "Config :" + "" + "\n");


//        if (placementAlgorithms == PlacementLogicFactory.QOS_AWARE_SPSO) {
//            myWriter.write("************************************ SPSO ************************************ \n" +
//                    "No of iterations : " + SpsoConfig.EPOCHS + "\n" +
//                    "No of particles : " + SpsoConfig.NUM_PARTICLE + "\n" +
//                    "Mutation rate : " + SpsoConfig.MUTATION_RATE_MIN + "-" + SpsoConfig.MUTATION_RATE_MAX + "\n" +
//                    "Mutated Particle Count : " + SpsoConfig.MUTATED_PARTICLE_COUNT + "\n" +
//                    "Makespan:Cost => " + SpsoConfig.LATENCY_WEIGHT + " : " + SpsoConfig.COST_WEIGHT + "\n" +
//                    "CompR : NwR => " + SpsoConfig.COMP_RESOURCE_WEIGHT + " : " + SpsoConfig.NW_RESOURCE_WEIGHT + "\n" +
//                    "Inertia weight func : " + SpsoConfig.INERTIA_FUNCTION + "\n" +
//                    "Best cost : " + bestCost + "\n" +
//                    "Best Latency : " + bestLatency + "\n" +
//                    "Best NW : " + bestNw + "\n" +
//                    "Best R : " + bestR + "\n" +
//                    "Best QoS : " + qosViolation + "\n" +
//                    "Best ResourceUtilization : " + resourceUtilization + "\n" +
//                    "Mutation func : " + SpsoConfig.MUTATE_FUNCTION + "\n"
//
//            );
//        } else if (placementAlgorithms == PlacementLogicFactory.SCLPSO) {
//            myWriter.write("************************************ SCLPSO ************************************ \n" +
//                    "No of iterations : " + SclpsoConfig.EPOCHS + "\n" +
//                    "No of particles : " + SclpsoConfig.NUM_PARTICLE + "\n" +
//                    "Makespan:Cost => " + SclpsoConfig.LATENCY_WEIGHT + " : " + SclpsoConfig.COST_WEIGHT + "\n" +
//                    "CompR : NwR => " + SclpsoConfig.COMP_RESOURCE_WEIGHT + " : " + SclpsoConfig.NW_RESOURCE_WEIGHT + "\n" +
//                    "Inertia weight func : " + SclpsoConfig.INERTIA_FUNCTION + "\n" +
//                    "Best cost : " + bestCost + "\n" +
//                    "Best Latency : " + bestLatency + "\n" +
//                    "Best NW : " + bestNw + "\n" +
//                    "Best R : " + bestR + "\n" +
//                    "Best QoS : " + qosViolation + "\n" +
//                    "Best ResourceUtilization : " + resourceUtilization + "\n" +
//                    "GAP : " + SclpsoConfig.GAP + "\n" +
//                    "Coeff : " + SclpsoConfig.COGNITIVE_MAX + "\n"
//
//            );
//        } else if (placementAlgorithms == PlacementLogicFactory.DNCPSO) {
//            myWriter.write("************************************ DNCPSO ************************************ \n" +
//                    "No of iterations : " + DncpsoConfig.EPOCHS + "\n" +
//                    "No of particles : " + DncpsoConfig.NUM_PARTICLE + "\n" +
//                    "Mutation rate : " + DncpsoConfig.MUTATION_RATE + "\n" +
//                    "Mutated Particle Count : " + DncpsoConfig.MUTATED_PARTICLE_COUNT + "\n" +
//                    "ETA : " + DncpsoConfig.ETA + "\n" +
//                    "Best objective : " + dncpsoObjest + "\n"+
//                    "Device Count : "+deviceCount+"\n"
//            );
//        }
//        else if (placementAlgorithms == PlacementLogicFactory.NSGAII) {
//            myWriter.write("************************************ DNCPSO ************************************ \n" +
//                    "No of iterations : " + NSGAIIConfig.EPOCHS + "\n" +
//                    "No of chromosones : " + NSGAIIConfig.NUM_CHROMOSOMES + "\n" +
//                    "Mutation rate : " + NSGAIIConfig.MUTATION_RATE + "\n" +
//                    "Device Count : "+deviceCount+"\n"
//            );
//        }
//        myWriter.close();
    }

    private static void copyNecessaryFiles(String directoryPath) throws IOException {
//        if (placementAlgorithms == PlacementLogicFactory.QOS_AWARE_SPSO || placementAlgorithms == PlacementLogicFactory.SCLPSO) {
//            File source = new File("plots/BudgetBest.txt");
//            File dest = new File(directoryPath + "/BudgetBest.txt");
//            dest.createNewFile();
//            copyFile(source, dest);
//
//            source = new File("plots/CmpResourceBest.txt");
//            dest = new File(directoryPath + "/CmpResourceBest.txt");
//            dest.createNewFile();
//            copyFile(source, dest);
//
//            source = new File("plots/LatencyBest.txt");
//            dest = new File(directoryPath + "/LatencyBest.txt");
//            dest.createNewFile();
//            copyFile(source, dest);
//
//            source = new File("plots/NwResourceBest.txt");
//            dest = new File(directoryPath + "/NwResourceBest.txt");
//            dest.createNewFile();
//            copyFile(source, dest);
//
//            source = new File("plots/QoSBest.txt");
//            dest = new File(directoryPath + "/QoSBest.txt");
//            dest.createNewFile();
//            copyFile(source, dest);
//
//            source = new File("plots/ResourceUt.txt");
//            dest = new File(directoryPath + "/ResourceUt.txt");
//            dest.createNewFile();
//            copyFile(source, dest);
//
//        } else if (placementAlgorithms == PlacementLogicFactory.QOS_AWARE_OPL) {
//            File source = new File("src/org/fog/test/Optimizer/data/singleCaseData.dat");
//            File dest = new File(directoryPath + "/singleCaseData.dat");
//            dest.createNewFile();
//            copyFile(source, dest);
//
//            source = new File("src/org/fog/test/Optimizer/data/schedule_apps.dat");
//            dest = new File(directoryPath + "/schedule_apps.dat");
//            dest.createNewFile();
//            copyFile(source, dest);
//        }
//        if (placementAlgorithms == PlacementLogicFactory.DNCPSO) {
//            File source = new File("plots/BestFitnessDncpso.txt");
//            File dest = new File(directoryPath + "/BestFitnessDncpso.txt");
//            dest.createNewFile();
//            copyFile(source, dest);
//        }
    }

    private static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static void main(String[] args) throws IOException {
        generateResults();
    }
}
