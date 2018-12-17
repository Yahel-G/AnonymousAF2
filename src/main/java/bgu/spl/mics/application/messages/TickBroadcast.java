package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;


public class TickBroadcast implements Broadcast {
    private int time;
    private int timeOfDeath;

    public TickBroadcast(int time, int duration, int speed){
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
