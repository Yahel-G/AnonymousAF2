package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.passiveObjects.DeliveryVehicle;

/**
 * Created by Yahel on 06/12/2018.
 */
public class FreeVehicleEvent implements Event {
    private DeliveryVehicle vehicle;

    public FreeVehicleEvent(DeliveryVehicle vehicle){
        this.vehicle = vehicle;
    }

    public DeliveryVehicle getVehicle() {
        return vehicle;
    }
}
