package com.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/*
TwitterServer is used to receive messages from over processes
 */

public class TwitterServer {

    private int id;
    private HashMap<Integer,String> ips;
    public Site site;
    public int count;

    public TwitterServer(int id, HashMap<Integer,String> ips, Site s)
    {
        this.id = id;
        this.ips = ips;
        this.site = s;
        this.count = 0;
    }

    public void run()
    {
        try
        {
            int socketNumber = 9100 + id;
            ServerSocket listener = new ServerSocket(socketNumber); 
            new TwitterClientGUI(this.id, this.ips, this.site).run();
           
            while (true) 
            {
                this.count++;
                try
                {
                    new Handler(listener.accept(), this.count, this.id, this.ips, this.site).start();

                }
                finally 
                {
                    listener.close();
                }
                new EchoThread(this.id, this.count, this.site, this.ips).start();
            }        
        }
        catch (IOException e) 
        {
            System.out.println(e);
        }
    }

    public class EchoThread extends Thread 
    {
        private int id;
        private HashMap<Integer,String> ips;
        private Site site;
        private int count;

        public EchoThread(int id, int count, Site site, HashMap<Integer,String> ips) {
            this.id = id;
            this.count = count;
            this.site = site;
            this.ips = ips;
        }

        public void run() 
        {
            int socketNum = 9110 + this.id + this.count;
            ServerSocket listener = new ServerSocket(socketNum); 
           
            try
            {
                try 
                {
                    while (true) 
                    {
                        new Handler(listener.accept(), this.count, this.id, this.ips, this.site).start();
                    }
                } 
                finally 
                {
                    listener.close();
                }
            }
            catch (IOException e) 
            {
                System.out.println(e);
            }
        }
    }
    public class Handler extends Thread
    {
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;
        private int count;
        private int id;
        private HashMap<Integer,String> ips;
        private Site site;
        
        public Handler(Socket socket, int count, int id, HashMap<Integer,String> ips, Site site) 
        {
            this.socket = socket;
            this.count = count;
            this.id = id;
            this.site = site;
            this.ips = ips;
        }

        public void run() 
        {
            try 
            {
                String clientMsg;
                String json = "";
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));
                while((clientMsg = in.readLine()) != null)
                {
                    writer.append(clientMsg);
                    json += clientMsg;
                }
                writer.close();
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                Message msg = mapper.readValue(json, Message.class);
                site.recvmsg(msg.twt, msg.table, msg.partialLog);
                //do message processing
                //send to sites
                new TwitterClientSender(this.id, this.ips, this.site, this.count).run();

            }
            catch (IOException e) 
            {
                System.out.println(e);
            }
        }
    }
}

