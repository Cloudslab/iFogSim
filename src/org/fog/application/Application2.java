package org.fog.application;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.fog.application.microservicesBased.DAG;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Tuple;
import org.fog.entities.Tuple2;
import org.fog.utils.FogUtils;
import org.fog.utils.GeoCoverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Application2 extends Application {

    protected Map<String, List<String>> specialPlacementInfo = new HashMap<>(); // module name to placement device staring with...

    protected DAG dag;

    public static Application2 createApplication(String appId, int userId) {
        return new Application2(appId, userId);
    }

    public Application2(String appId, int userId) {
        super(appId, userId);
    }

    public Application2(String appId, List<AppModule> modules, List<AppEdge> edges, List<AppLoop> loops, GeoCoverage geoCoverage) {
        super(appId, modules, edges, loops, geoCoverage);
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
                        Tuple2 tuple = new Tuple2(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
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
                        Tuple2 tuple = new Tuple2(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
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
                        tuple.setTraversedMicroservices(((Tuple2) inputTuple).getTraversed());
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
                Tuple2 tuple = new Tuple2(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
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
            Tuple2 tuple = new Tuple2(getAppId(), FogUtils.generateTupleId(), edge.getDirection(),
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

    public void setSpecialPlacementInfo(String moduleName, String device) {
        if (specialPlacementInfo.containsKey(moduleName))
            specialPlacementInfo.get(moduleName).add(device);
        else {
            List<String> devices = new ArrayList<>();
            devices.add(device);
            specialPlacementInfo.put(moduleName, devices);
        }
    }

    public Map<String, List<String>> getSpecialPlacementInfo() {
        return specialPlacementInfo;
    }

    public void createDAG() {
        List<String> moduleNames = new ArrayList<>();
        for (AppModule module : getModules()) {
            moduleNames.add(module.getName());
        }
        dag = new DAG(moduleNames);
        for (AppEdge edge : getEdges()) {
            if (edge.getDirection() == Tuple.UP)
                dag.addEdge(edge.getSource(), edge.getDestination());
        }
    }

    public DAG getDAG() {
        return dag;
    }


}
