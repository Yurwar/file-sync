package com.filesync;

import java.io.*;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws IOException {
        if(args[0].equals("-s")) {
            Server server = new Server(4646, "./ServerFolder");
            server.startServer();
        } else if(args[0].equals("-c")){
            Client client = new Client("localhost", 4646, "./ClientFolder");
            client.startClient();
            System.exit(0);
        }
    }
}
