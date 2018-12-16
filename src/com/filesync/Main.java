package com.filesync;

import java.io.*;

public class Main {

    public static void main(String[] args) throws IOException {
        if(args[0].equals("-s")) {
            Server server = new Server(4646, args[1]);
            server.startServer();
        } else if(args[0].equals("-c")){
            Client client = new Client(args[1], 4646, args[2]);
            client.startClient();
            System.exit(0);
        }
    }
}
