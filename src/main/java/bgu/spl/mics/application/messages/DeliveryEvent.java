package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;

/**
 * Created by Yahel on 01/12/2018.
 */
public class DeliveryEvent implements Event {
    private Integer distance;
    private String address;
    public DeliveryEvent(Integer distance, String address){
        this.distance = distance;
        this.address = address;
    }

    public Integer getDistance() {
        return distance;
    }

    public String getAddress() {
        return address;
    }
}
