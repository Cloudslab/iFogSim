package org.fog.application.microservicesBased;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Tuple;
import org.fog.entities.microservicesBased.TupleM;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogUtils;
import org.fog.utils.GeoCoverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Samodha Pallewatta.
 */
public class MicroservicesApplication extends Application {

    QoSProfile qoSProfile = null;

    public static MicroservicesApplication createApplication(String appId, int userId) {
        return new MicroservicesApplication(appId, userId);
    }

    public MicroservicesApplication(String appId, int userId) {
        super(appId, userId);
    }

    public MicroservicesApplication(String appId, List<AppModule> modules, List<AppEdge> edges, List<AppLoop> loops, GeoCoverage geoCoverage) {
        super(appId, modules, edges, loops, geoCoverage);
    }

    /**
     * @param moduleName
     * @param ram
     * @param mips
     * @param size
     */
    public void addAppModule(String moduleName, int ram, int mips, int size) {
        long bw = 1000;
        String vmm = "Xen";

        AppModule module = new AppModule(FogUtils.generateEntityId(), moduleName, getAppId(), getUserId(),
                mips, ram, bw, size, vmm, new TupleScheduler(mips, 1), new HashMap<Pair<String, String>, SelectivityModel>());

        getModules().add(module);
    }


    @Override
    public List<Tuple> getResultantTuples(String moduleName, Tuple inputTuple, int sourceDeviceId, int sourceModuleId) {
        List<Tuple> tuples = new ArrayList<Tuple>();
        AppModule module = getModuleByName(moduleName);
        for (AppEdge edge : getEdges()) {
            if (edge.getSource().equals(moduleName)) {
                Pair<String, String> pair = new Pair<String, String>(inputTuple.getTupleType(), edge.getTupleType());

                if (module.getSelectivityMap().get(pair) == null)
                    continue;
                SelectivityModel selectivityModel = module.getSelectivityMap().get(pair);
                if (selectivityModel.canSelect()) {
                    //TODO check if the edge is ACTUATOR, then create multiple tuples
                    if (edge.getEdgeType() == AppEdge.ACTUATOR) {
                        //for(Integer actuatorId : module.getActuatorSubscriptions().get(edge.getTupleType())){
                        TupleM tuple = new TupleM(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
                                (long) (edge.getTupleCpuLength()),
                                inputTuple.getNumberOfPes(),
                                (long) (edge.getTupleNwLength()),
                                inputTuple.getCloudletOutputSize(),
                                inputTuple.getUtilizationModelCpu(),
                                inputTuple.getUtilizationModelRam(),
                                inputTuple.getUtilizationModelBw()
                        );
                        tuple.setActualTupleId(inputTuple.getActualTupleId());
                        tuple.setUserId(inputTuple.getUserId());
                        tuple.setAppId(inputTuple.getAppId());
                        tuple.setDestModuleName(edge.getDestination());
                        tuple.setSrcModuleName(edge.getSource());
                        tuple.setDirection(Tuple.ACTUATOR);
                        tuple.setTupleType(edge.getTupleType());
                        tuple.setSourceDeviceId(sourceDeviceId);
                        tuple.setSourceModuleId(sourceModuleId);
                        //tuple.setActuatorId(actuatorId);

                        tuples.add(tuple);
                        //}
                    } else {
                        TupleM tuple = new TupleM(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
                                (long) (edge.getTupleCpuLength()),
                                inputTuple.getNumberOfPes(),
                                (long) (edge.getTupleNwLength()),
                                inputTuple.getCloudletOutputSize(),
                                inputTuple.getUtilizationModelCpu(),
                                inputTuple.getUtilizationModelRam(),
                                inputTuple.getUtilizationModelBw()
                        );
                        tuple.setActualTupleId(inputTuple.getActualTupleId());
                        tuple.setUserId(inputTuple.getUserId());
                        tuple.setAppId(inputTuple.getAppId());
                        tuple.setDestModuleName(edge.getDestination());
                        tuple.setSrcModuleName(edge.getSource());
                        tuple.setDirection(edge.getDirection());
                        tuple.setTupleType(edge.getTupleType());
                        tuple.setSourceModuleId(sourceModuleId);
                        tuple.setTraversedMicroservices(((TupleM) inputTuple).getTraversed());
                        tuples.add(tuple);
                    }
                }
            }
        }
        return tuples;
    }

    @Override
    public Tuple createTuple(AppEdge edge, int sourceDeviceId, int sourceModuleId) {
        AppModule module = getModuleByName(edge.getSource());
        if (edge.getEdgeType() == AppEdge.ACTUATOR) {
            for (Integer actuatorId : module.getActuatorSubscriptions().get(edge.getTupleType())) {
                TupleM tuple = new TupleM(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
                        (long) (edge.getTupleCpuLength()),
                        1,
                        (long) (edge.getTupleNwLength()),
                        100,
                        new UtilizationModelFull(),
                        new UtilizationModelFull(),
                        new UtilizationModelFull()
                );
                tuple.setUserId(getUserId());
                tuple.setAppId(getAppId());
                tuple.setDestModuleName(edge.getDestination());
                tuple.setSrcModuleName(edge.getSource());
                tuple.setDirection(Tuple.ACTUATOR);
                tuple.setTupleType(edge.getTupleType());
                tuple.setSourceDeviceId(sourceDeviceId);
                tuple.setActuatorId(actuatorId);
                tuple.setSourceModuleId(sourceModuleId);

                return tuple;
            }
        } else {
            TupleM tuple = new TupleM(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
                    (long) (edge.getTupleCpuLength()),
                    1,
                    (long) (edge.getTupleNwLength()),
                    100,
                    new UtilizationModelFull(),
                    new UtilizationModelFull(),
                    new UtilizationModelFull()
            );
            //tuple.setActualTupleId(inputTuple.getActualTupleId());
            tuple.setUserId(getUserId());
            tuple.setAppId(getAppId());
            tuple.setDestModuleName(edge.getDestination());
            tuple.setSrcModuleName(edge.getSource());
            tuple.setDirection(edge.getDirection());
            tuple.setTupleType(edge.getTupleType());
            tuple.setSourceModuleId(sourceModuleId);

            return tuple;
        }
        return null;
    }

    public void setQoSProfile(QoSProfile qoSProfile) {
        this.qoSProfile = qoSProfile;
    }

    public QoSProfile getQoSProfile() {
        return qoSProfile;
    }

}
