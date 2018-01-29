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
TwitterClient is used to send messages from over processes
 */

public class TwitterClient
{
    PrintWriter outt;
    private int id;
    private HashMap<Integer,String> ips;
    public Site site;

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
    public TwitterClient(int id, HashMap<Integer,String> ips, Site s)
    {
        this.id = id;
        this.ips = ips;
        this.site = s;
    }

    public void run() throws IOException 
    {
        //handles user interface and the branching to sections of the algo

        Scanner scanner = new Scanner(System.in);

        System.out.print("Welcome to Twitter!\n");
        System.out.print("Enter One of the follow options: \n");
        System.out.print("1 tweet \n");
        System.out.print("2 view timeline\n");
        System.out.print("3 block \n");
        System.out.print("4 unblock user\n");
        System.out.print("Crtl C to Exit \n\n");
        int entry = scanner.nextInt();

        String serverAddress = ips.get(id);
        int socketNumber = 9100 + id;
        Socket socket = new Socket("localhost", socketNumber);
        outt = new PrintWriter(socket.getOutputStream(), true);
        while(true) 
        {
            if (entry == 1) //tweet
            {
                System.out.print("My Tweet: \n");
                scanner.nextLine();
                String tweet = scanner.nextLine();
                socket = sender(socket, outt, tweet);   //send message processed here
                outt = new PrintWriter(socket.getOutputStream(), true);

            } 
            else if (entry == 2) //view
            {
                ArrayList<Tweet> twtList = site.view();
                for (Tweet t : twtList){
                    Date formattedTime = new Date(t.time*1000);
                    System.out.println("Tweet by " + t.id + ": " + t.msg + ",   Time: " + formattedTime.toString());
                }

            }
            else if (entry == 3) //block
            {
                System.out.print("Block: \n");
                int block = scanner.nextInt();
                if(block >= site.blockTable.length || block < 0){ //number too big
                    System.out.println("invalid option!");
                }else{
                    site.blockEvent(block);
                }
            }
            else if (entry == 4) //unblock
            {
                System.out.print("Unblock: \n");
                int unblock = scanner.nextInt();
                if(unblock >= site.blockTable.length || unblock < 0){ //number too big
                    System.out.println("invalid option!");
                }else{
                    site.unblockEvent(unblock);
                }
            }
            else
            {
                //if they input and invalid choice
                System.out.print("Invalid Entry! Try again! \n");
            }

            System.out.print("Enter One of the follow options: \n");
            System.out.print("1 tweet \n");
            System.out.print("2 view timeline\n");
            System.out.print("3 block user\n");
            System.out.print("4 unblock user\n");
            System.out.print("Crtl C to Exit \n");
            entry = scanner.nextInt();
        }
    }

    //send the message to the ips in the config file
    public Socket sender(Socket socket, PrintWriter out, String tweet)
    {
        //open new socket based on machine numbers
        //must do comparision of ips to blocked data structure 
        //loop through machines to send messages
        //return to 'home' machine
        //use port 900# based on machine num 

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
                int socketNumber = 9100 + key;
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
        int socketNumber = 9100 + id;
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