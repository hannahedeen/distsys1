package com.company;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.*;

/*
Site object handles all data structures of a process.
It contains the log, blocktable, timetable
After each event creation/addition to the log, it creates a json file called site.json which stores
all the information that allows site recovery.
 */

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;


public class Site {
    public int[][] blockTable;  //Dictionary [blocker][person being blocked]
    public int id;              //site id
    public int[][] timeTable;   //Lamport based time array
    public ArrayList<Event> log;//site Log
    public int lamportTime;     //C

    public Site(){}

    public Site(int numprocesses, int id){ //what happens when a new site initializes
        blockTable = new int[numprocesses][numprocesses];
        this.id = id;
        lamportTime = 0;
        timeTable = new int[numprocesses][numprocesses];
        log = new ArrayList<Event>();
    }

    public boolean hasRec(int id, Event e){
        return timeTable[id][e.id] >= e.lamportTime;
    }

    public void blockEvent(int blockID){ //local event
        lamportTime++;
        timeTable[this.id][this.id] = lamportTime;

        //block event
        if (blockTable[this.id][blockID] == 1){
            System.out.println("Already blocked");
        }else {
            blockTable[this.id][blockID] = 1;
            Event e = new Event(1, this.id, blockID, lamportTime);
            log.add(e);
        }

        try {
            stableStorage(this);
        }
        catch (IOException exc){
            System.out.println("failed to serialize site!");
        }

    }

    public void unblockEvent(int unblockID){ //local event
        lamportTime++;
        timeTable[this.id][this.id] = lamportTime;

        //unblock event
        if (blockTable[this.id][unblockID] == 0){
            System.out.println("That process is not blocked");
        }else {
            blockTable[this.id][unblockID] = 0;
            Event e = new Event(2, this.id, unblockID, lamportTime);
            log.add(e);
        }
        try {
            stableStorage(this);
        }
        catch (IOException exc){
            System.out.println("failed to serialize site!");
        }
    }

    public Tweet tweet(String msg){
        lamportTime++;
        timeTable[this.id][this.id] = lamportTime;
        Tweet twt = new Tweet(this.id, msg, this.lamportTime);
        Event e = new Event(0, this.id, this.lamportTime, twt);
        log.add(e);
        try {
            stableStorage(this);
        }
        catch (IOException exc){
            System.out.println("failed to serialize site!");
        }
        return twt;
    }

    public Message sendmsg(Tweet twt, int id){
        ArrayList<Event> PL = new ArrayList<Event>();
        for (Event e : log){
            if(!hasRec(id, e)){
                PL.add(e);
            }
        }

        Message msg = new Message(PL, twt, this.timeTable);
        return msg;
    }

    public ArrayList<Tweet> view(){
        ArrayList<Tweet> twtList = new ArrayList<>();
        for (Event e : log){
            if (e.type == 0){
                if (blockTable[e.twt.id][this.id] == 0)
                    twtList.add(e.twt);
            }
        }
        Collections.sort(twtList, new sortByUTC());
        return twtList;
    }

    public void recvmsg(Tweet twt, int[][] Ti, ArrayList<Event> PL){
        ArrayList<Event> partialLog = new ArrayList<Event>();
        for (int i=0; i<PL.size(); i++){
            if (!hasRec(this.id, PL.get(i))){
                partialLog.add(PL.get(i));
            }
        }

        int[][] blockTracking = new int[blockTable.length][blockTable[0].length];
        for (Event e : partialLog){
            if (e.type == 1){
                blockTracking[e.id][e.blockee]++;
            }
            if (e.type == 2){
                blockTracking[e.id][e.blockee]--;
            }
        }

        for (int i =0; i<blockTable.length; i++){
            for(int j =0; j<blockTable[0].length; j++){
                if ((blockTracking[i][j] + blockTable[i][j]) >= 1){ //sum of block events is greater than sum of unblock
                    this.blockTable[i][j] = 1;
                }else if((blockTracking[i][j] + blockTable[i][j]) <= 0){ // sum of unblock events is greater or equal to block
                    this.blockTable[i][j] = 0;
                }
            }
        }

        if (timeTable.length != Ti.length){
            System.out.println("Table length mismatch, you messed up :(");
            return;
        }
        for (int i = 0; i<timeTable.length; i++){ //direct knowledge update
            timeTable[this.id][i] = Math.max(timeTable[this.id][i], Ti[twt.id][i]);
        }
        for (int i = 0; i< timeTable.length; i++){ //indirect knowledge update
            for(int j = 0; j< timeTable[0].length; j++){
                timeTable[i][j] = Math.max(timeTable[i][j], Ti[i][j]);
            }
        }

        //log truncation
        ArrayList<Event> truncatedLog = new ArrayList<>();
        partialLog.addAll(log);
        for (Event e : partialLog){ //log truncation
            if (e.type == 1 || e.type == 2){
                boolean everyonehas = true;
                for (int i =0; i<timeTable.length; i++){
                    if (!hasRec(i, e)){
                        everyonehas = false;
                    }
                }
                if (!everyonehas){
                    truncatedLog.add(e);
                }
            }else{
                truncatedLog.add(e);
            }
        }
        log = truncatedLog;

        try {
            stableStorage(this);
        }
        catch (IOException exc){
            System.out.println("failed to serialize site!");
        }
    }

    public static void stableStorage(Site s) throws IOException{
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        File f = new File("site.json");
        mapper.writeValue(f, s);
    }

    class sortByUTC implements Comparator<Tweet>
    {
        // Used for sorting in ascending order of
        // roll number
        public int compare(Tweet a, Tweet b)
        {
            return (int) (a.time - b.time);
        }
    }

/*
    public static void main(String[] args) throws IOException {

        // create the mapper
        ObjectMapper mapper = new ObjectMapper();

        // enable pretty printing
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // serialize the object
        File f = new File("test.json");
        File g = new File("test2.json");
        mapper.writeValue(f, test1());

        Site testSite = mapper.readValue(f, Site.class);
        mapper.writeValue(g, testSite);

        Tweet t1 = new Tweet(0, "hello", 6);
        Tweet t2 = new Tweet(0, "world", 7);

        Event e1 = new Event(1, 0, t1.lamportTime, t1);
        Event e2 = new Event(1, 0, t2.lamportTime, t2);

        ArrayList<Event> l = new ArrayList<Event>();
        l.add(e1);
        l.add(e2);

        Site s = new Site(2,0);
        s.timeTable[1][1] = 1;
        s.log = l;
        s.lamportTime = 18;
        s.blockEvent(1);

    }

    private static Site test1(){
        Tweet t1 = new Tweet(1, "hello", 6);
        Tweet t2 = new Tweet(1, "world", 7);

        Event e1 = new Event(1, 1, t1.lamportTime, t1);
        Event e2 = new Event(1, 1, t2.lamportTime, t2);

        ArrayList<Event> l = new ArrayList<Event>();
        l.add(e1);
        l.add(e2);

        Site s = new Site(2,1);
        s.blockTable[0][1] = 1;
        s.timeTable[0][0] = 1;
        s.timeTable[1][1] = 1;
        s.log = l;
        s.lamportTime = 18;

        return s;

    }
*/

}
