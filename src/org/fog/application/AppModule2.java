package org.fog.application;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.fog.application.selectivity.SelectivityModel;

import java.util.Map;


public class AppModule2 extends AppModule {

    public AppModule2(int id, String name, String appId, int userId, double mips, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler, Map<Pair<String, String>, SelectivityModel> selectivityMap) {
        super(id, name, appId, userId, mips, ram, bw, size, vmm, cloudletScheduler, selectivityMap);
    }

    public AppModule2(AppModule operator) {
        super(operator);
    }
}
