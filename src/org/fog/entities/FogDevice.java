package org.fog.entities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.mobilitydata.Clustering;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.*;
import org.json.simple.JSONObject;

import java.util.*;

public class FogDevice extends PowerDatacenter {
    protected Queue<Tuple> northTupleQueue;
    protected Queue<Pair<Tuple, Integer>> southTupleQueue;

    protected List<String> activeApplications;

    protected Map<String, Application> applicationMap;
    protected Map<String, List<String>> appToModulesMap;
    protected Map<Integer, Double> childToLatencyMap;


    protected Map<Integer, Integer> cloudTrafficMap;

    protected double lockTime;

    /**
     * ID of the parent Fog Device
     */
    protected int parentId;

    /**
     * ID of the Controller
     */
    protected int controllerId;
    /**
     * IDs of the children Fog devices
     */
    protected List<Integer> childrenIds;

    protected Map<Integer, List<String>> childToOperatorsMap;

    /**
     * Flag denoting whether the link southwards from this FogDevice is busy
     */
    protected boolean isSouthLinkBusy;

    /**
     * Flag denoting whether the link northwards from this FogDevice is busy
     */
    protected boolean isNorthLinkBusy;

    protected double uplinkBandwidth;
    protected double downlinkBandwidth;
    protected double uplinkLatency;
    protected List<Pair<Integer, Double>> associatedActuatorIds;

    protected double energyConsumption;
    protected double lastUtilizationUpdateTime;
    protected double lastUtilization;
    private int level;

    protected double ratePerMips;

    protected double totalCost;

    protected Map<String, Map<String, Integer>> moduleInstanceCount;

    protected List<Integer> clusterMembers = new ArrayList<Integer>();
    protected boolean isInCluster = false;
    protected boolean selfCluster = false; // IF there is only one fog device in one cluster without any sibling
    protected Map<Integer, Double> clusterMembersToLatencyMap; // latency to other cluster members

    protected Queue<Pair<Tuple, Integer>> clusterTupleQueue;// tuple and destination cluster device ID
    protected boolean isClusterLinkBusy; //Flag denoting whether the link connecting to cluster from this FogDevice is busy
    protected double clusterLinkBandwidth;


    public FogDevice(
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);


        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());

        clusterTupleQueue = new LinkedList<>();
        setClusterLinkBusy(false);

    }

    public FogDevice(
            String name, long mips, int ram,
            double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
        super(name, null, null, new LinkedList<Storage>(), 0);

        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // host storage
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                powerModel
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));

        String arch = Config.FOG_DEVICE_ARCH;
        String os = Config.FOG_DEVICE_OS;
        String vmm = Config.FOG_DEVICE_VMM;
        double time_zone = Config.FOG_DEVICE_TIMEZONE;
        double cost = Config.FOG_DEVICE_COST;
        double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
        double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
        double costPerBw = Config.FOG_DEVICE_COST_PER_BW;

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        setCharacteristics(characteristics);

        setLastProcessTime(0.0);
        setVmList(new ArrayList<Vm>());
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host1 : getCharacteristics().getHostList()) {
            host1.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }


        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);


        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setChildToLatencyMap(new HashMap<Integer, Double>());
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());

        clusterTupleQueue = new LinkedList<>();
        setClusterLinkBusy(false);
    }

    /**
     * Overrides this method when making a new and different type of resource. <br>
     * <b>NOTE:</b> You do not need to override {@link #body()} method, if you use this method.
     *
     * @pre $none
     * @post $none
     */
    protected void registerOtherEntity() {

    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.TUPLE_ARRIVAL:
                processTupleArrival(ev);
                break;
            case FogEvents.LAUNCH_MODULE:
                processModuleArrival(ev);
                break;
            case FogEvents.RELEASE_OPERATOR:
                processOperatorRelease(ev);
                break;
            case FogEvents.SENSOR_JOINED:
                processSensorJoining(ev);
                break;
            case FogEvents.SEND_PERIODIC_TUPLE:
                sendPeriodicTuple(ev);
                break;
            case FogEvents.APP_SUBMIT:
                processAppSubmit(ev);
                break;
            case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
                updateNorthTupleQueue();
                break;
            case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
                updateSouthTupleQueue();
                break;
            case FogEvents.ACTIVE_APP_UPDATE:
                updateActiveApplications(ev);
                break;
            case FogEvents.ACTUATOR_JOINED:
                processActuatorJoined(ev);
                break;
            case FogEvents.LAUNCH_MODULE_INSTANCE:
                updateModuleInstanceCount(ev);
                break;
            case FogEvents.MODULE_SEND:
                moduleSend(ev);
                break;
            case FogEvents.MODULE_RECEIVE:
                moduleReceive(ev);
                break;
            case FogEvents.RELEASE_MODULE:
                processModuleTermination(ev);
                break;
            case FogEvents.RESOURCE_MGMT:
                manageResources(ev);
                break;
            case FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE:
                updateClusterTupleQueue();
                break;
            case FogEvents.START_DYNAMIC_CLUSTERING:
                //This message is received by the devices to start their clustering
                processClustering(this.getParentId(), this.getId(), ev);
                break;
            default:
                break;
        }
    }

    protected void moduleSend(SimEvent ev) {
        // TODO Auto-generated method stub
        JSONObject object = (JSONObject) ev.getData();
        AppModule appModule = (AppModule) object.get("module");
        System.out.println(getName() + " is sending " + appModule.getName());
        NetworkUsageMonitor.sendingModule((double) object.get("delay"), appModule.getSize());
        MigrationDelayMonitor.setMigrationDelay((double) object.get("delay"));


        sendNow(getId(), FogEvents.RELEASE_MODULE, appModule);


    }

    protected void moduleReceive(SimEvent ev) {
        // TODO Auto-generated method stub
        JSONObject object = (JSONObject) ev.getData();
        AppModule appModule = (AppModule) object.get("module");
        Application app = (Application) object.get("application");
        System.out.println(getName() + " is receiving " + appModule.getName());
        NetworkUsageMonitor.sendingModule((double) object.get("delay"), appModule.getSize());
        MigrationDelayMonitor.setMigrationDelay((double) object.get("delay"));

        sendNow(getId(), FogEvents.APP_SUBMIT, app);
        sendNow(getId(), FogEvents.LAUNCH_MODULE, appModule);
    }

    /**
     * Perform miscellaneous resource management tasks
     *
     * @param ev
     */
    protected void manageResources(SimEvent ev) {
        updateEnergyConsumption();
        send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
    }

    /**
     * Updating the number of modules of an application module on this device
     *
     * @param ev instance of SimEvent containing the module and no of instances
     */
    protected void updateModuleInstanceCount(SimEvent ev) {
        ModuleLaunchConfig config = (ModuleLaunchConfig) ev.getData();
        String appId = config.getModule().getAppId();
        if (!moduleInstanceCount.containsKey(appId))
            moduleInstanceCount.put(appId, new HashMap<String, Integer>());
        moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
        System.out.println(getName() + " Creating " + config.getInstanceCount() + " instances of module " + config.getModule().getName());
    }

    private AppModule getModuleByName(String moduleName) {
        AppModule module = null;
        for (Vm vm : getHost().getVmList()) {
            if (((AppModule) vm).getName().equals(moduleName)) {
                module = (AppModule) vm;
                break;
            }
        }
        return module;
    }

    /**
     * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, only one tuple is sent DOWN while instanceCount number of tuples are sent UP.
     *
     * @param ev SimEvent instance containing the edge to send tuple on
     */
    protected void sendPeriodicTuple(SimEvent ev) {
        AppEdge edge = (AppEdge) ev.getData();
        String srcModule = edge.getSource();
        AppModule module = getModuleByName(srcModule);

        if (module == null)
            return;

        int instanceCount = module.getNumInstances();
        /*
         * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
         */
        for (int i = 0; i < ((edge.getDirection() == Tuple.UP) ? instanceCount : 1); i++) {
            //System.out.println(CloudSim.clock()+" : Sending periodic tuple "+edge.getTupleType());
            Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId(), module.getId());
            updateTimingsOnSending(tuple);
            sendToSelf(tuple);
        }
        send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
    }

    protected void processActuatorJoined(SimEvent ev) {
        int actuatorId = ev.getSource();
        double delay = (double) ev.getData();
        getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
    }


    protected void updateActiveApplications(SimEvent ev) {
        Application app = (Application) ev.getData();
        getActiveApplications().add(app.getAppId());
    }


    public String getOperatorName(int vmId) {
        for (Vm vm : this.getHost().getVmList()) {
            if (vm.getId() == vmId)
                return ((AppModule) vm).getName();
        }
        return null;
    }

    /**
     * Update cloudet processing without scheduling future events.
     *
     * @return the double
     */
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        for (PowerHost host : this.<PowerHost>getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }

        if (timeDiff > 0) {
            Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);

            for (PowerHost host : this.<PowerHost>getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;

                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }

            Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
        }

        setPower(getPower() + timeFrameDatacenterEnergy);

        checkCloudletCompletion();

        /** Remove completed VMs **/
        /**
         * Change made by HARSHIT GUPTA
         */
		/*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/

        Log.printLine();

        setLastProcessTime(currentTime);
        return minTime;
    }


    protected void checkCloudletCompletion() {
        boolean cloudletCompleted = false;
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {

                        cloudletCompleted = true;
                        Tuple tuple = (Tuple) cl;
                        TimeKeeper.getInstance().tupleEndedExecution(tuple);
                        Application application = getApplicationMap().get(tuple.getAppId());
                        Logger.debug(getName(), "Completed execution of tuple " + tuple.getCloudletId() + "on " + tuple.getDestModuleName());
                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
                        for (Tuple resTuple : resultantTuples) {
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule) vm).getName(), vm.getId());
                            updateTimingsOnSending(resTuple);
                            sendToSelf(resTuple);
                        }
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
        }
        if (cloudletCompleted)
            updateAllocatedMips(null);
    }

    protected void updateTimingsOnSending(Tuple resTuple) {
        // TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE.
        // WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
        String srcModule = resTuple.getSrcModuleName();
        String destModule = resTuple.getDestModuleName();
        for (AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()) {
            if (loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)) {
                int tupleId = TimeKeeper.getInstance().getUniqueId();
                resTuple.setActualTupleId(tupleId);
                if (!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
                    TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
                TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
                TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());

                //Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);

            }
        }
    }

    protected int getChildIdWithRouteTo(int targetDeviceId) {
        for (Integer childId : getChildrenIds()) {
            if (targetDeviceId == childId)
                return childId;
            if (((FogDevice) CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
                return childId;
        }
        return -1;
    }

    protected int getChildIdForTuple(Tuple tuple) {
        if (tuple.getDirection() == Tuple.ACTUATOR) {
            int gatewayId = ((Actuator) CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
            return getChildIdWithRouteTo(gatewayId);
        }
        return -1;
    }

    protected void updateAllocatedMips(String incomingOperator) {
        getHost().getVmScheduler().deallocatePesForAllVms();
        for (final Vm vm : getHost().getVmList()) {
            if (vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule) vm).getName().equals(incomingOperator)) {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add((double) getHost().getTotalMips());
                    }
                });
            } else {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add(0.0);
                    }
                });
            }
        }

        updateEnergyConsumption();

    }

    private void updateEnergyConsumption() {
        double totalMipsAllocated = 0;
        for (final Vm vm : getHost().getVmList()) {
            AppModule operator = (AppModule) vm;
            operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
                    .getAllocatedMipsForVm(operator));
            totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
        }

        double timeNow = CloudSim.clock();
        double currentEnergyConsumption = getEnergyConsumption();
        double newEnergyConsumption = currentEnergyConsumption + (timeNow - lastUtilizationUpdateTime) * getHost().getPowerModel().getPower(lastUtilization);
        setEnergyConsumption(newEnergyConsumption);
	
		/*if(getName().equals("d-0")){
			System.out.println("------------------------");
			System.out.println("Utilization = "+lastUtilization);
			System.out.println("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			System.out.println(timeNow-lastUtilizationUpdateTime);
		}*/

        double currentCost = getTotalCost();
        double newcost = currentCost + (timeNow - lastUtilizationUpdateTime) * getRatePerMips() * lastUtilization * getHost().getTotalMips();
        setTotalCost(newcost);

        lastUtilization = Math.min(1, totalMipsAllocated / getHost().getTotalMips());
        lastUtilizationUpdateTime = timeNow;
    }

    protected void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        applicationMap.put(app.getAppId(), app);
    }

    public void addChild(int childId) {
        if (CloudSim.getEntityName(childId).toLowerCase().contains("sensor"))
            return;
        if (!getChildrenIds().contains(childId) && childId != getId())
            getChildrenIds().add(childId);
        if (!getChildToOperatorsMap().containsKey(childId))
            getChildToOperatorsMap().put(childId, new ArrayList<String>());
    }


    protected void updateCloudTraffic() {
        int time = (int) CloudSim.clock() / 1000;
        if (!cloudTrafficMap.containsKey(time))
            cloudTrafficMap.put(time, 0);
        cloudTrafficMap.put(time, cloudTrafficMap.get(time) + 1);
    }

    protected void sendTupleToActuator(Tuple tuple) {
		/*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
        for (Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()) {
            int actuatorId = actuatorAssociation.getFirst();
            double delay = actuatorAssociation.getSecond();
            String actuatorType = ((Actuator) CloudSim.getEntity(actuatorId)).getActuatorType();
            if (tuple.getDestModuleName().equals(actuatorType)) {
                send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
                return;
            }
        }
        for (int childId : getChildrenIds()) {
            sendDown(tuple, childId);
        }
    }

    int numClients = 0;

    protected void processTupleArrival(SimEvent ev) {
        Tuple tuple = (Tuple) ev.getData();

        if (getName().equals("cloud")) {
            updateCloudTraffic();
        }
		
		/*if(getName().equals("d-0") && tuple.getTupleType().equals("_SENSOR")){
			System.out.println(++numClients);
		}*/
        Logger.debug(getName(), "Received tuple " + tuple.getCloudletId() + "with tupleType = " + tuple.getTupleType() + "\t| Source : " +
                CloudSim.getEntityName(ev.getSource()) + "|Dest : " + CloudSim.getEntityName(ev.getDestination()));
		
		/*if(CloudSim.getEntityName(ev.getSource()).equals("drone_0")||CloudSim.getEntityName(ev.getDestination()).equals("drone_0"))
			System.out.println(CloudSim.clock()+" "+getName()+" Received tuple "+tuple.getCloudletId()+" with tupleType = "+tuple.getTupleType()+"\t| Source : "+
		CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));*/

        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);

        if (FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())) {
        }

        if (tuple.getDirection() == Tuple.ACTUATOR) {
            sendTupleToActuator(tuple);
            return;
        }

        if (getHost().getVmList().size() > 0) {
            final AppModule operator = (AppModule) getHost().getVmList().get(0);
            if (CloudSim.clock() > 0) {
                getHost().getVmScheduler().deallocatePesForVm(operator);
                getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;

                    {
                        add((double) getHost().getTotalMips());
                    }
                });
            }
        }


        if (getName().equals("cloud") && tuple.getDestModuleName() == null) {
            sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
        }

        if (appToModulesMap.containsKey(tuple.getAppId())) {
            if (appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())) {
                int vmId = -1;
                for (Vm vm : getHost().getVmList()) {
                    if (((AppModule) vm).getName().equals(tuple.getDestModuleName()))
                        vmId = vm.getId();
                }
                if (vmId < 0
                        || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) &&
                        tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId)) {
                    return;
                }
                tuple.setVmId(vmId);
                //Logger.error(getName(), "Executing tuple for operator " + moduleName);

                updateTimingsOnReceipt(tuple);

                executeTuple(ev, tuple.getDestModuleName());
            } else if (tuple.getDestModuleName() != null) {
                if (tuple.getDirection() == Tuple.UP)
                    sendUp(tuple);
                else if (tuple.getDirection() == Tuple.DOWN) {
                    for (int childId : getChildrenIds())
                        sendDown(tuple, childId);
                }
            } else {
                sendUp(tuple);
            }
        } else {
            if (tuple.getDirection() == Tuple.UP)
                sendUp(tuple);
            else if (tuple.getDirection() == Tuple.DOWN) {
                for (int childId : getChildrenIds())
                    sendDown(tuple, childId);
            }
        }
    }

    protected void updateTimingsOnReceipt(Tuple tuple) {
        Application app = getApplicationMap().get(tuple.getAppId());
        String srcModule = tuple.getSrcModuleName();
        String destModule = tuple.getDestModuleName();
        List<AppLoop> loops = app.getLoops();
        for (AppLoop loop : loops) {
            if (loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)) {
                Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                if (startTime == null)
                    break;
                if (!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())) {
                    TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
                    TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
                }
                double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
                int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
                double delay = CloudSim.clock() - TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
                double newAverage = (currentAverage * currentCount + delay) / (currentCount + 1);
                TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
                TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount + 1);
                break;
            }
        }
    }

    protected void processSensorJoining(SimEvent ev) {
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
    }

    protected void executeTuple(SimEvent ev, String moduleName) {
        Logger.debug(getName(), "Executing tuple on module " + moduleName);
        Tuple tuple = (Tuple) ev.getData();

        AppModule module = getModuleByName(moduleName);

        if (tuple.getDirection() == Tuple.UP) {
            String srcModule = tuple.getSrcModuleName();
            if (!module.getDownInstanceIdsMaps().containsKey(srcModule))
                module.getDownInstanceIdsMaps().put(srcModule, new ArrayList<Integer>());
            if (!module.getDownInstanceIdsMaps().get(srcModule).contains(tuple.getSourceModuleId()))
                module.getDownInstanceIdsMaps().get(srcModule).add(tuple.getSourceModuleId());

            int instances = -1;
            for (String _moduleName : module.getDownInstanceIdsMaps().keySet()) {
                instances = Math.max(module.getDownInstanceIdsMaps().get(_moduleName).size(), instances);
            }
            module.setNumInstances(instances);
        }

        TimeKeeper.getInstance().tupleStartedExecution(tuple);
        updateAllocatedMips(moduleName);
        processCloudletSubmit(ev, false);
        updateAllocatedMips(moduleName);
		/*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/
    }

    protected void processModuleArrival(SimEvent ev) {
        AppModule module = (AppModule) ev.getData();
        String appId = module.getAppId();
        if (!appToModulesMap.containsKey(appId)) {
            appToModulesMap.put(appId, new ArrayList<String>());
        }
        appToModulesMap.get(appId).add(module.getName());
        processVmCreate(ev, false);
        if (module.isBeingInstantiated()) {
            module.setBeingInstantiated(false);
        }

        initializePeriodicTuples(module);

        module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
                .getAllocatedMipsForVm(module));

    }

    protected void processModuleTermination(SimEvent ev) {
        processVmDestroy(ev, false);
    }

    protected void initializePeriodicTuples(AppModule module) {
        String appId = module.getAppId();
        Application app = getApplicationMap().get(appId);
        List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
        for (AppEdge edge : periodicEdges) {
            send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
        }
    }

    protected void processOperatorRelease(SimEvent ev) {
        this.processVmMigrate(ev, false);
    }


    protected void updateNorthTupleQueue() {
        if (!getNorthTupleQueue().isEmpty()) {
            Tuple tuple = getNorthTupleQueue().poll();
            sendUpFreeLink(tuple);
        } else {
            setNorthLinkBusy(false);
        }
    }

    protected void sendUpFreeLink(Tuple tuple) {
        double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
        setNorthLinkBusy(true);
        send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
        send(parentId, networkDelay + getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
    }

    protected void sendUp(Tuple tuple) {
        if (parentId > 0) {
            if (!isNorthLinkBusy()) {
                sendUpFreeLink(tuple);
            } else {
                northTupleQueue.add(tuple);
            }
        }
    }


    protected void updateSouthTupleQueue() {
        if (!getSouthTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getSouthTupleQueue().poll();
            sendDownFreeLink(pair.getFirst(), pair.getSecond());
        } else {
            setSouthLinkBusy(false);
        }
    }

    protected void sendDownFreeLink(Tuple tuple, int childId) {
        double networkDelay = tuple.getCloudletFileSize() / getDownlinkBandwidth();
        //Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
        setSouthLinkBusy(true);
        //System.out.println(getName()+" Sending tuple with tupleType = "+tuple.getTupleType()+" to "+childId);
        double latency = getChildToLatencyMap().get(childId);
        send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
        send(childId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
    }

    protected void sendDown(Tuple tuple, int childId) {
        if (getChildrenIds().contains(childId)) {
            if (!isSouthLinkBusy()) {
                sendDownFreeLink(tuple, childId);
            } else {
                southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
            }
        }
    }


    protected void sendToSelf(Tuple tuple) {
        send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
    }

    public PowerHost getHost() {
        return (PowerHost) getHostList().get(0);
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public List<Integer> getChildrenIds() {
        return childrenIds;
    }

    public void setChildrenIds(List<Integer> childrenIds) {
        this.childrenIds = childrenIds;
    }

    public double getUplinkBandwidth() {
        return uplinkBandwidth;
    }

    public void setUplinkBandwidth(double uplinkBandwidth) {
        this.uplinkBandwidth = uplinkBandwidth;
    }

    public double getUplinkLatency() {
        return uplinkLatency;
    }

    public void setUplinkLatency(double uplinkLatency) {
        this.uplinkLatency = uplinkLatency;
    }

    public boolean isSouthLinkBusy() {
        return isSouthLinkBusy;
    }

    public boolean isNorthLinkBusy() {
        return isNorthLinkBusy;
    }

    public void setSouthLinkBusy(boolean isSouthLinkBusy) {
        this.isSouthLinkBusy = isSouthLinkBusy;
    }

    public void setNorthLinkBusy(boolean isNorthLinkBusy) {
        this.isNorthLinkBusy = isNorthLinkBusy;
    }

    public int getControllerId() {
        return controllerId;
    }

    public void setControllerId(int controllerId) {
        this.controllerId = controllerId;
    }

    public List<String> getActiveApplications() {
        return activeApplications;
    }

    public void setActiveApplications(List<String> activeApplications) {
        this.activeApplications = activeApplications;
    }

    public Map<Integer, List<String>> getChildToOperatorsMap() {
        return childToOperatorsMap;
    }

    public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
        this.childToOperatorsMap = childToOperatorsMap;
    }

    public Map<String, Application> getApplicationMap() {
        return applicationMap;
    }

    public void setApplicationMap(Map<String, Application> applicationMap) {
        this.applicationMap = applicationMap;
    }

    public Queue<Tuple> getNorthTupleQueue() {
        return northTupleQueue;
    }

    public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
        this.northTupleQueue = northTupleQueue;
    }

    public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
        return southTupleQueue;
    }

    public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
        this.southTupleQueue = southTupleQueue;
    }

    public double getDownlinkBandwidth() {
        return downlinkBandwidth;
    }

    public void setDownlinkBandwidth(double downlinkBandwidth) {
        this.downlinkBandwidth = downlinkBandwidth;
    }

    public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
        return associatedActuatorIds;
    }

    public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
        this.associatedActuatorIds = associatedActuatorIds;
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public Map<Integer, Double> getChildToLatencyMap() {
        return childToLatencyMap;
    }

    public void setChildToLatencyMap(Map<Integer, Double> childToLatencyMap) {
        this.childToLatencyMap = childToLatencyMap;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getRatePerMips() {
        return ratePerMips;
    }

    public void setRatePerMips(double ratePerMips) {
        this.ratePerMips = ratePerMips;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public Map<String, Map<String, Integer>> getModuleInstanceCount() {
        return moduleInstanceCount;
    }

    public void setModuleInstanceCount(
            Map<String, Map<String, Integer>> moduleInstanceCount) {
        this.moduleInstanceCount = moduleInstanceCount;
    }

    public List<String> getPlacedAppModulesPerApplication(String appId) {
        return appToModulesMap.get(appId);
    }

    public void removeChild(int childId) {
        // TODO Auto-generated method stub
        @SuppressWarnings("deprecation")
        Integer childIDobject = new Integer(childId);
        if (getChildrenIds().contains(childId) && childId != getId())
            getChildrenIds().remove(childIDobject);
        if (getChildToOperatorsMap().containsKey(childId)) {
            List<String> operatorName = getChildToOperatorsMap().get(childId);
            getChildToOperatorsMap().remove(childId, operatorName);
        }
    }

    public void setClusterMembers(List clusterList) {
        this.clusterMembers = clusterList;
    }

    public void addClusterMember(int clusterMemberId) {
        this.clusterMembers.add(clusterMemberId);
    }

    public List<Integer> getClusterMembers() {
        return this.clusterMembers;
    }

    public void setIsInCluster(Boolean bool) {
        this.isInCluster = bool;
    }

    public void setSelfCluster(Boolean bool) {
        this.selfCluster = bool;
    }

    public Boolean getIsInCluster() {
        return this.isInCluster;
    }

    public Boolean getSelfCluster() {
        return this.selfCluster;
    }

    public void setClusterMembersToLatencyMap(Map<Integer, Double> clusterMembersToLatencyMap) {
        this.clusterMembersToLatencyMap = clusterMembersToLatencyMap;
    }

    public Map<Integer, Double> getClusterMembersToLatencyMap() {
        return this.clusterMembersToLatencyMap;
    }

    protected void processClustering(int parentId, int nodeId, SimEvent ev) {
        JSONObject objectLocator = (JSONObject) ev.getData();
        Clustering cms = new Clustering();
        cms.createClusterMembers(this.getParentId(), this.getId(), objectLocator);
    }

    public double getClusterLinkBandwidth() {
        return clusterLinkBandwidth;
    }

    protected void setClusterLinkBandwidth(double clusterLinkBandwidth) {
        this.clusterLinkBandwidth = clusterLinkBandwidth;
    }

    protected void sendToCluster(Tuple tuple, int clusterNodeID) {
        if (getClusterMembers().contains(clusterNodeID)) {
            if (!isClusterLinkBusy) {
                sendThroughFreeClusterLink(tuple, clusterNodeID);
            } else {
                clusterTupleQueue.add(new Pair<Tuple, Integer>(tuple, clusterNodeID));
            }
        }
    }

    private void updateClusterTupleQueue() {
        if (!getClusterTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getClusterTupleQueue().poll();
            sendThroughFreeClusterLink(pair.getFirst(), pair.getSecond());
        } else {
            setClusterLinkBusy(false);
        }
    }

    private void sendThroughFreeClusterLink(Tuple tuple, Integer clusterNodeID) {
        double networkDelay = tuple.getCloudletFileSize() / getClusterLinkBandwidth();
        setClusterLinkBusy(true);
        double latency = (getClusterMembersToLatencyMap()).get(clusterNodeID);
        send(getId(), networkDelay, FogEvents.UPDATE_CLUSTER_TUPLE_QUEUE);
        send(clusterNodeID, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
    }

    protected void setClusterLinkBusy(boolean busy) {
        this.isClusterLinkBusy = busy;
    }

    public Queue<Pair<Tuple, Integer>> getClusterTupleQueue() {
        return clusterTupleQueue;
    }


}