package com.filesync;

import java.io.*;
import java.util.ArrayList;

public class Server {
    private final File MAIN_DIR;
    private final int PORT_NUMBER;
    private Connection connection;

    public Server(int port_number, String dir_path) {
        this.PORT_NUMBER = port_number;
        this.MAIN_DIR = new File(dir_path);
    }

    void sync() {
        System.out.println("Start server on port " + PORT_NUMBER);
        connection = new Connection(PORT_NUMBER);
        while(true) try {
            connection.acceptSocket();

            FileOperation fileOperation = new FileOperation();

            ArrayList<String> serverFilesPaths = fileOperation.getAllFilesPaths(MAIN_DIR);
            int amountOfFiles = serverFilesPaths.size();
            connection.writeInt(amountOfFiles);
            connection.sendPathsArray(serverFilesPaths);

            for (String filePath : serverFilesPaths) {
                connection.writeLong(new File(filePath).lastModified());
            }

            int pathsToSendSize = connection.readInt();
            int pathsToReceiveSize = connection.readInt();
            int pathsToDeleteSize = connection.readInt();
            ArrayList<String> clientFilesPathsToSend = connection.receivePathsArray(pathsToSendSize);
            ArrayList<String> clientFilesPathsToReceive = connection.receivePathsArray(pathsToReceiveSize);
            ArrayList<String> serverFilesPathsToDelete = connection.receivePathsArray(pathsToDeleteSize);

            System.out.println("Files to delete from server");
            fileOperation.printArray(serverFilesPathsToDelete);
            fileOperation.deleteFiles(serverFilesPathsToDelete);

            fileOperation.createMissingFolders(clientFilesPathsToSend);

            if (clientFilesPathsToSend.size() == 0 && clientFilesPathsToReceive.size() == 0) {
                System.out.println("All files is up-to-date");
            }

            for (String fileToReceivePath : clientFilesPathsToSend) {
                if (clientFilesPathsToSend.size() > 0) {
                    File fileToReceive = new File(fileToReceivePath);
                    connection.receiveFile(fileToReceive);
                }
            }

            for (String fileToSendPath : clientFilesPathsToReceive) {
                if (clientFilesPathsToReceive.size() > 0) {
                    File fileToSend = new File(fileToSendPath);
                    connection.sendFile(fileToSend);
                }
            }
            connection.receiveMetadata(clientFilesPathsToSend);
            connection.sendMetadata(clientFilesPathsToReceive);

            System.out.println("Synchronization completed successfully");
            System.out.println("Client disconnected\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
