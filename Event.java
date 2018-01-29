package com.company;

/*
Event Object are log entries
 */

public class Event {
    public int type; //should be an enum later, 0 = tweet, 1 = block, 2 = unblock
    public int id;
    public int blockee;
    public int lamportTime;
    public Tweet twt;

    public Event(){}

    public Event(int type, int id, int blockee, int lamportTime){
        this.type = type;
        this.id = id;
        this.blockee = blockee;
        this.lamportTime = lamportTime;
        twt = null;
    }

    public Event(int type, int id, int lamportTime, Tweet x){
        this.type = type;
        this.id = id;
        this.lamportTime = lamportTime;
        twt = x;
    }
}
