package org.DSACLab.test;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.References;
import org.fog.placement.LocationHandler;
import org.fog.placement.MobilityController;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMobileEdgewards;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.io.IOException;
import java.util.*;

public class InVehicleServiceFog {
    //所有的边缘设备（端、边、云）
    static List<FogDevice> fogDevices = new ArrayList<>();
    //传感器设备
    static List<Sensor> sensors = new ArrayList<>();
    //执行器设备
    static List<Actuator> actuators = new ArrayList<>();
    static Map<Integer, Integer> mobilityPattern = new HashMap<>();
    //传感器传输时间
    static double SENSOR_TRANSMISSION_TIME = 100;
    //本次模拟的移动车辆的数量
    static int numberOfVehicle = 10000;
    //
    static LocationHandler locator;

    static boolean traceFlag = false;

    public static void main(String[] args) {

        Log.printLine("Starting In-Vehicle Service Fog...");

        try {

            Log.disable();

            int numOfCloudUser = 1;

            Calendar calendar = Calendar.getInstance();

            //cloudsim初始化
            CloudSim.init(numOfCloudUser, calendar, traceFlag);

            String applicationId = "In-Vehicle Service"; //应用id
            //创建使用cloudsim的使用者
            FogBroker broker = new FogBroker("broker");
            //创建分布式应用程序
            Application application = createApplication(applicationId, broker.getId());
            application.setUserId(broker.getId());

            //解析基站数据和移动数据
            DataParser dataObject = new DataParser();
            locator = new LocationHandler(dataObject);
            //车辆移动的数据集
            String datasetPrefixPath= new References("./dataset/geo_log/vehicleLocation-melbCBD_").dataset_reference;
            //创建车辆
            createVehicle(broker.getId(),applicationId,datasetPrefixPath);
            //创建网关、代理、云节点
            createFogDevices();

            //建立一个模组映射 Map<String, List<String>>
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping

            //将模组放置到到云上  （由于是分布式应用，除存储模块外，其余模块会随着车辆移动而产生迁移，因此这里只放置Storage Module）
            moduleMapping.addModuleToDevice("storageModule", "cloud");

            //将所有的边缘节点设备（移动手机、网管节点、代理节点、云节点）传入到controller里，由controller来控制所有的事件发生与排序
            MobilityController controller = new MobilityController("master-controller", fogDevices,sensors,actuators, locator);

            controller.submitApplication(application, 0, (new ModulePlacementMobileEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            Log.printLine("Starting In-Vehicle Service Fog...");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }


    }

    /**创建云、proxy、gateway节点
     * @author liuziyuan
     * @throws IOException 因为会使用io流所以抛出io异常
     */
    private static void createFogDevices() throws IOException {
        locator.parseResourceInfo();
        if(locator.getLevelWiseResources(locator.getLevelID("Cloud")).size()==1){
            FogDevice cloud=createFogDevice("cloud",44800,40000,100,10000,0.01,16*103,16*83.25);
            cloud.setParentId(References.NOT_SET);
            //把仿真器中的FogDevice和基站数据集中的基站绑定
            locator.linkDataWithInstance(cloud.getId(), locator.getLevelWiseResources(locator.getLevelID("Cloud")).get(0));
            fogDevices.add(cloud);

            for(int i=0;i<locator.getLevelWiseResources(locator.getLevelID("Proxy")).size();i++){
                FogDevice proxy=createFogDevice("proxy_server_"+i,2800,4000,10000,10000,0.0,107.339, 83.4333);
                locator.linkDataWithInstance(proxy.getId(),locator.getLevelWiseResources(locator.getLevelID("Proxy")).get(i));
                proxy.setParentId(cloud.getId());
                proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms
                fogDevices.add(proxy);
            }

            for (int i = 0; i < locator.getLevelWiseResources(locator.getLevelID("Gateway")).size(); i++) {

                FogDevice gateway = createFogDevice("gateway_" + i, 2800, 4000, 10000, 10000, 0.0, 107.339, 83.4333);
                locator.linkDataWithInstance(gateway.getId(), locator.getLevelWiseResources(locator.getLevelID("Gateway")).get(i));
                gateway.setParentId(locator.determineParent(gateway.getId(), References.SETUP_TIME));
                gateway.setUplinkLatency(4); //设置网关到区域代理节点的延迟为4ms
                fogDevices.add(gateway);
            }
        }

    }

    /**
     * @author                  liuziyuan
     * @param userId            brokerId
     * @param applicationId     应用id（应用名称）
     * @param datasetPrefixPath 车辆移动轨迹数据集前缀
     */
    private static void createVehicle(int userId, String applicationId, String datasetPrefixPath) throws IOException {
        //存入车辆移动轨迹模式
        for(int vehicleId=1;vehicleId<=numberOfVehicle;vehicleId++)
            mobilityPattern.put(vehicleId,References.DIRECTIONAL_MOBILITY);

        //locatorhandler 解析车辆移动轨迹
        locator.parseVehicleInfo(mobilityPattern,datasetPrefixPath);
        //获取所有车辆的数据id
        List<String> VehicleDataIds = locator.getMobileUserDataId();
        for (int i = 0; i < numberOfVehicle; i++) {
            //添加车辆设备（物理设备）
            FogDevice vehicle = addVehicle("vehicle_" + i, userId, applicationId, References.NOT_SET); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            vehicle.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 4 ms
            locator.linkDataWithInstance(vehicle.getId(), VehicleDataIds.get(i));

            fogDevices.add(vehicle);
        }
    }

    /**
     * @author              liuziyuan
     * @param deviceName    物理设备名称
     * @param userId        用户id
     * @param applicationId 应用id（应用名称）
     * @param parentId      该物理设备的上层物理网关设备id
     * @return FogDevice    返回一个雾设备（车辆）
     */
    private static FogDevice addVehicle(String deviceName, int userId, String applicationId, int parentId) {
        FogDevice vehicle=createFogDevice(deviceName,500,20,1000,270,0,87.53,82.44);
        vehicle.setParentId(parentId);
        Sensor vehicleSensor=new Sensor("sensor_"+deviceName,"M-SENSOR",userId,applicationId,new DeterministicDistribution(SENSOR_TRANSMISSION_TIME));
        sensors.add(vehicleSensor);
        Actuator vehicleActuator=new Actuator("actuator_"+deviceName,userId,applicationId,"M-DISPLAY");
        actuators.add(vehicleActuator);
        vehicleSensor.setGatewayDeviceId(vehicle.getId());
        vehicleSensor.setLatency(10.0);
        vehicleActuator.setGatewayDeviceId(vehicle.getId());
        vehicleActuator.setLatency(5.0);
        return vehicle;
    }

    /**
     * @author liuziyuan
     * @param deviceName    物理设备名称
     * @param mips          mips
     * @param ram           内存
     * @param upBw          上行带宽
     * @param downBw        下行带宽
     * @param ratePerMips   每单位mips的开销率 （未使用）
     * @param busyPower     忙时功率
     * @param idlePower     闲时功率
     * @return FogDevice    返回一个物理雾设备
     */
    private static FogDevice createFogDevice(String deviceName, int mips, int ram, int upBw, int downBw, double ratePerMips, double busyPower, double idlePower ) {
        //设置一个空的process unit 数组
        List<Pe>peList=new ArrayList<>();
        peList.add(new Pe(0,
                new PeProvisionerOverbooking(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // host storage
        int bw = 10000;
        //为每一个雾设备建立PowerHost类以用于能耗感知
        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        String arch = "x86"; // system architecture 系统架构
        String os = "Linux"; // operating system 操作系统
        String vmm = "Xen"; //虚拟化方式
        double time_zone = 10.0; // time zone this resource located 虚拟机的时区（参数作用存疑）
        double cost = 3.0; // the cost of using processing in this resource 使用资源的开销 具体单位不明
        double costPerMem = 0.05; // the cost of using memory in this resource 使用单位内存的开销
        double costPerStorage = 0.001; // the cost of using storage in this 使用单位存储的开销
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource 使用单位带宽的开销
        LinkedList<Storage> storageList = new LinkedList<>(); // we are not adding SAN  SAN指存储区域网络  在本示例中未使用
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(deviceName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fogdevice;

    }

    /**
     * @author                  liuziyuan
     * @param applicationId     应用id（应用名称）
     * @param applicationUserId 使用应用程序的用户id
     * @return Application      一个分布式应用
     */
    private static Application createApplication(String applicationId, int applicationUserId) {
        Random periodicityRandom=new Random();
        double periodicity=periodicityRandom.nextInt(10);
        System.out.println("periodicity============"+periodicity);
        //new一个 application 对象
        Application application = Application.createApplication(applicationId, applicationUserId);

        //添加分布式应用的应用模块
        application.addAppModule("clientModule",10,1000,1000,10000);
        application.addAppModule("processingModule",10,1000,1000,10000);
        application.addAppModule("storageModule",10,2000,1000,10000);

        //添加模块间的元组关系  AppEdge
        application.addAppEdge("M-SENSOR", "clientModule", 3000, 500, "M-SENSOR", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("clientModule", "processingModule",  3500, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("processingModule", "storageModule", 1000, 1000, "PROCESSED_DATA", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("processingModule", "clientModule",  100, 500, "ACTION_COMMAND", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("clientModule", "M-DISPLAY", 1000, 500, "ACTUATION_SIGNAL", Tuple.DOWN, AppEdge.ACTUATOR);

//        application.addAppEdge("M-SENSOR", "clientModule", periodicity, 2000, 500, "M-SENSOR", Tuple.UP, AppEdge.SENSOR);
//        application.addAppEdge("clientModule", "processingModule", periodicity, 2000, 500, "RAW_DATA", Tuple.UP, AppEdge.MODULE);
//        application.addAppEdge("processingModule", "storageModule", periodicity, 1000, 1000, "PROCESSED_DATA", Tuple.UP, AppEdge.MODULE);
//        application.addAppEdge("processingModule", "clientModule", periodicity, 100, 500, "ACTION_COMMAND", Tuple.DOWN, AppEdge.MODULE);
//        application.addAppEdge("clientModule", "M-DISPLAY", periodicity, 1000, 500, "ACTUATION_SIGNAL", Tuple.DOWN, AppEdge.ACTUATOR);

        //添加模块间元组映射的关系及元组发射率
        application.addTupleMapping("clientModule","M-SENSOR","RAW_DATA",new FractionalSelectivity(1.0));
        application.addTupleMapping("processingModule","RAW_DATA","PROCESSED_DATA",new FractionalSelectivity(0.5));
        application.addTupleMapping("processingModule","RAW_DATA","ACTION_COMMAND",new FractionalSelectivity(1.0));
        application.addTupleMapping("clientModule","ACTION_COMMAND","ACTUATION_SIGNAL",new FractionalSelectivity(1.0));

        //应用内的数据循环
        final AppLoop inVehicleServiceLoop = new AppLoop(new ArrayList<String>(){{
            add("M-SENSOR");
            add("clientModule");
            add("processingModule");
            add("clientModule");
            add("M-DISPLAY");
        }});

        List<AppLoop> loops=new ArrayList<AppLoop>(){{
            add(inVehicleServiceLoop);
        }};
        application.setLoops(loops);
        return application;
    }
}
