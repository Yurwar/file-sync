package com.filesync;

public class Main {

    public static void main(String[] args) {
        if(args[0].equals("-s")) {
            Server server = new Server(49150, args[1]);
            server.sync();
        } else if(args[0].equals("-c")){
            Client client = new Client(args[1], 49150, args[2]);
            client.sync();
        } else {
            System.err.println("Incorrect arguments, please use \"-c [SERVER_IP_ADDR] ./[FOLDER_TO_SYNC_PATH]\" to start client or \"-s ./[FOLDER_TO_SYNC_PATH]\" to start server");
            System.exit(1);
        }
    }
}
