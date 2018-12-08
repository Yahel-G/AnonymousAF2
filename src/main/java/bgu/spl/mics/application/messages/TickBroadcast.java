package bgu.spl.mics.application.messages;

import bgu.spl.mics.Broadcast;

/**
 * Created by Yahel on 01/12/2018.
 */
public class TickBroadcast implements Broadcast {
    private int time;

    public TickBroadcast(int time){
        this.time = time;
    }

    public int giveMeSomeTime(){
        return time;
    }
}
