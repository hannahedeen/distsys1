package com.company;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;

/*
Tweet objects contain the message being sent between processes as well the UTC timestamp
 */

public class Tweet {
    public int id;
    public String msg;
    public int lamportTime;
    public long time;

    public Tweet(){}

    public Tweet(int id, String msg, int lamportTime){
        this.id = id;
        this.msg = msg;
        this.time = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond();
        this.lamportTime = lamportTime;
    }

    public Tweet(Tweet x) {
        this.id = x.id;
        this.msg = x.msg;
        this.time = x.time;
        this.lamportTime = x.lamportTime;
    }
}
