package com.company;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.HashMap;

/*
TwitterClientSender is used to send messages (accept, promise, ack) to other processes
 */

public class TwitterClientSender
{
    PrintWriter outt;
    private int id;
    private HashMap<Integer,String> ips;
    public Site site;
    private int count;

    public static void clientrun(int id, HashMap<Integer,String> ips, Site s)
    {
        try
        {
            TwitterClient client = new TwitterClient(id, ips, s);
            client.run();  
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }   
    }
    public TwitterClientSender(int id, HashMap<Integer,String> ips, Site s, int count)
    {
        this.id = id;
        this.ips = ips;
        this.site = s;
        this.count = count;
        //this.newdatastructure
    }

    public void run() throws IOException 
    {
        //handles user interface and the branching to sections of the algo

        String serverAddress = ips.get(id);
        int socketNumber = 9100 + id + count;
        Socket socket = new Socket("localhost", socketNumber);
        outt = new PrintWriter(socket.getOutputStream(), true);
        String tweet = new String();

        socket = sender(socket, outt, tweet);   //send message processed here
        closeSocket(socket, outt);
        
    }

    //send the message to the ips in the config file
    public Socket sender(Socket socket, PrintWriter out, String tweet)
    {
        //open new socket based on machine numbers
        //must do comparision of ips to blocked data structure 
        //loop through machines to send messages
        //return to 'home' machine
        //use port 910# based on machine num 

        closeSocket(socket, out);

        //tweet creation
        Tweet twt = site.tweet(tweet);

        for(int key : ips.keySet())
        {
            if (key >= site.blockTable.length){
                break;
            }
            if(key == id || site.blockTable[site.id][key] == 1) //if blocked or self, skip
            {
                continue;
            }
            else
            {
                String serverAddress = ips.get(key);
                int socketNumber = 9110 + key + this.count;
                try
                {
                    //message creation
                    Message msg = site.sendmsg(twt, key);
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonString = "";
                    try {
                        jsonString = mapper.writeValueAsString(msg);
                    }catch (MismatchedInputException e){

                    }
                    jsonString.replace("\n", "");
                    socket = new Socket(InetAddress.getByName(serverAddress), socketNumber);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    out.append(jsonString);
                    System.out.println("Connected to process " + key + " at " + serverAddress);
                    closeSocket(socket, out);
                }
                catch (IOException e) 
                {
                    System.out.println(e);
                }   
            }            
        }

        String serverAddress = ips.get(id);
        int socketNumber = 9110 + id + this.count;
        Socket newSocket = new Socket();
        try
        {
            newSocket = new Socket("localhost", socketNumber);
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }
        return newSocket;
    }

    public void closeSocket(Socket socket, PrintWriter out)
    {
        //closes all streams out and socket
        try
        {
            out.close();
            socket.close();
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }        
    } 
}