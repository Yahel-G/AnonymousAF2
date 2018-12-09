package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

/**
 * Created by Yahel on 01/12/2018.
 */
public class TickBroadcast implements Broadcast {
    private int time;
    private int timeOfDeath;

    public TickBroadcast(int time, int duration){
        this.time = time;
        timeOfDeath = duration;
    }

    public int giveMeSomeTime(){
        return time;
    }

    public int getTimeOfDeath() {
        return timeOfDeath; 
    }
}
