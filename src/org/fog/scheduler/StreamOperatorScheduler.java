package org.fog.scheduler;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.VmSchedulerTimeSharedOverSubscription;
import org.cloudbus.cloudsim.sdn.overbooking.VmSchedulerTimeSharedOverbookingEnergy;

public class StreamOperatorScheduler extends VmSchedulerTimeSharedOverbookingEnergy{

	public StreamOperatorScheduler(List<? extends Pe> pelist) {
		super(pelist);
	}
}
