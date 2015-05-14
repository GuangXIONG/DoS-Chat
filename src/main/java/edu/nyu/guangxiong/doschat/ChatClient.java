package edu.nyu.guangxiong.doschat;

import java.io.*;
import java.net.Socket;

/**
 * Author: GuangXIONG
 * Date: 05/05/2015
 * Time: 11:00 AM
 */

public class ChatClient {

    public static void main(String[] args){
        System.out.println("Start connecting to Chat Server, waiting for response...");
        try {
            Socket serverConnection = new Socket(ChatServer.SERVER_HOST, ChatServer.SERVER_PORT);
            Chatter chatter = new Chatter(serverConnection.getInputStream(),
                    serverConnection.getOutputStream(), System.in);
            chatter.run();
        } catch (IOException ioe) {
            System.out.println("Connection to Chat Server is over...");
        }
    }
}

class Chatter implements Runnable {

    private final InputStream chatServerInput;

    private final OutputStream chatServerOutput;

    private final InputStream userInput;

    public Chatter(InputStream chatServerInput, OutputStream chatServerOutput, InputStream userInput) {
        this.chatServerInput = chatServerInput;
        this.chatServerOutput = chatServerOutput;
        this.userInput = userInput;
    }

    @Override public void run() {
        // Read from userInput and write data to chatServerOutput
        Thread userThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader userBuffer = new BufferedReader(new InputStreamReader(userInput));
                try {
                    while(!Thread.currentThread().isInterrupted()){
                        String userText = userBuffer.readLine();
                        chatServerOutput.write(userText.getBytes());
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.out.println("Connection to Chat Server is over~! you may retry...");
                }
            }
        });
        userThread.start();

        // Read from chatServerInput and print values
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader serverBuffer = new BufferedReader(new InputStreamReader(chatServerInput));
                try {
                    while(!Thread.currentThread().isInterrupted()) {
                        String serverText = serverBuffer.readLine();
                        if (serverText.equals("null") || serverText == null) {
                            Thread.currentThread().interrupt();
                        }
                        System.out.println(serverText);
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.out.println("Connection to Chat Server is over~! you may retry...");
                }
            }
        });
        serverThread.start();
    }
}