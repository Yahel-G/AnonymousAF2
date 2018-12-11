package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

/**
 * Created by Yahel on 01/12/2018.
 */
public class TickBroadcast implements Broadcast {
    private int time;
    private int timeOfDeath;

    public TickBroadcast(int time, int duration, int speed){
        this.time = time;
        timeOfDeath = duration*speed;
    }



    public int giveMeSomeTime(){
        return time;
    }

    public int getTimeOfDeath() {
        return timeOfDeath;
    }
}
