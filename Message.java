package com.company;

import java.util.ArrayList;

/*
Message object is passed between processes through the use of "Tweets"
 */

public class Message {
    public ArrayList<Event> partialLog;
    public Tweet twt;
    public int[][] table;

    public Message(){
    }

    public Message(ArrayList<Event> pl, Tweet tweet, int[][] timeTable){
        this.partialLog = pl;
        this.twt = tweet;
        this.table = timeTable;
    }
}
