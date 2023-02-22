package org.DSACLab.test;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.mobilitydata.DataParser;
import org.fog.placement.LocationHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class InVehicleServiceFog {
    //所有的边缘设备（端、边、云）
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    //传感器设备
    static List<Sensor> sensors = new ArrayList<Sensor>();
    //执行器设备
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static boolean CLOUD = false;
    //传感器传输时间
    static double SENSOR_TRANSMISSION_TIME = 10;
    //本次模拟的移动车辆的数量
    static int numberOfVehicle = 100;
    //
    static LocationHandler locator;

    static boolean traceFlag = false;

    public static void main(String[] args) {

        Log.printLine("Starting In-Vehicle Service Fog...");

        try {

            Log.disable();

            int numOfCloudUser = 1;

            Calendar calendar = Calendar.getInstance();

            //cloudsim不需要追踪事件
            CloudSim.init(numOfCloudUser, calendar, traceFlag);

            String applicationId = "In-Vehicle Service"; //应用id

            FogBroker broker = new FogBroker("broker");
            //创建分布式应用程序
            Application application = createApplication(applicationId, broker.getId());
            application.setUserId(broker.getId());

            //解析基站数据和移动数据
            DataParser dataObject = new DataParser();
            locator = new LocationHandler(dataObject);

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }


    }

    /**
     * @author liuziyuan
     * @param applicationId  应用id（应用名称）
     * @param applicationUserId  使用应用程序的用户id
     * @return Application  一个分布式应用
     */
    private static Application createApplication(String applicationId, int applicationUserId) {
        Random periodicityRandom=new Random();
        double periodicity=periodicityRandom.nextDouble()*10;
        //new一个 application 对象
        Application application = Application.createApplication(applicationId, applicationUserId);

        //添加分布式应用的应用模块
        application.addAppModule("Vehicle Client Module",2000,1000,1000,10000);
        application.addAppModule("Processing Module",500,1000,1000,10000);
        application.addAppModule("Storage Module",500,2000,1000,10000);

        //添加模块间的元组关系  AppEdge
        application.addAppEdge("Vehicle Sensor", "Vehicle Client Module", periodicity, 2000, 500, "Vehicle Sensor Data", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("Vehicle Client Module", "Processing Module", periodicity, 2000, 500, "Raw Data", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Processing Module", "Storage Module", periodicity, 1000, 1000, "Processed Data", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Processing Module", "Vehicle Client Module", periodicity, 100, 500, "Action Command", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("Vehicle Client Module", "Vehicle Actuator", periodicity, 1000, 500, "Action Signal", Tuple.UP, AppEdge.ACTUATOR);

        //添加模块间元组映射的关系及元组发射率
        application.addTupleMapping("Vehicle Client Module","Vehicle Sensor Data","Raw Data",new FractionalSelectivity(1.0));
        application.addTupleMapping("Processing Module","Raw Data","Processed Data",new FractionalSelectivity(0.5));
        application.addTupleMapping("Processing Module","Raw Data","Action Command",new FractionalSelectivity(1.0));
        application.addTupleMapping("Vehicle Client Module","Action Command","Action Signal",new FractionalSelectivity(1.0));

        //应用内的数据循环
        final AppLoop inVehicleServiceLoop = new AppLoop(new ArrayList<String>(){{
            add("Vehicle Sensor");
            add("Vehicle Client Module");
            add("Processing Module");
            add("Vehicle Client Module");
            add("Vehicle Actuator");
        }});

        List<AppLoop> loops=new ArrayList<AppLoop>(){{
            add(inVehicleServiceLoop);
        }};
        application.setLoops(loops);
        return application;
    }
}
