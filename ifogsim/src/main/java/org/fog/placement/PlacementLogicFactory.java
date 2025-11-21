package org.fog.placement;

import org.fog.utils.Logger;

/**
 * Created by Samodha Pallewatta.
 */
public class PlacementLogicFactory {

    public static final int EDGEWART_MICROSERCVICES_PLACEMENT = 1;
    public static final int CLUSTERED_MICROSERVICES_PLACEMENT = 2;
    public static final int DISTRIBUTED_MICROSERVICES_PLACEMENT =3;

    public MicroservicePlacementLogic getPlacementLogic(int logic, int fonId) {
        switch (logic) {
//            case EDGEWART_MICROSERCVICES_PLACEMENT:
//                return new EdgewardMicroservicePlacementLogic(fonId);
            case CLUSTERED_MICROSERVICES_PLACEMENT:
                return new ClusteredMicroservicePlacementLogic(fonId);
            case DISTRIBUTED_MICROSERVICES_PLACEMENT:
                return new DistributedMicroservicePlacementLogic(fonId);
        }

        Logger.error("Placement Logic Error", "Error initializing placement logic");
        return null;
    }

}
