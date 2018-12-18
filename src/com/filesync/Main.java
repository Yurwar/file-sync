package com.filesync;

import java.io.*;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws IOException {
        if(args[0].equals("-s")) {
            Server server = new Server(49150, args[1]);
            server.sync();
        } else if(args[0].equals("-c")){
            Client client = new Client(args[1], 49150, args[2]);
            client.sync();
        }
    }
}
