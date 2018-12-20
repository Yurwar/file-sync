package com.filesync;

import java.io.*;
import java.util.ArrayList;

public class Client {
    private final File MAIN_DIR;
    private Connection connection;

    public Client(String serverAddr, int port_number, String dir_path) {
        this.MAIN_DIR = new File(dir_path);
        this.connection = new Connection(serverAddr, port_number);
    }

    void sync() {
        try {
            FileOperation fileOperation = new FileOperation();

            int amountOfFiles = connection.readInt();

            ArrayList<String> serverFilesPaths = connection.receivePathsArray(amountOfFiles);
            ArrayList<String> clientFilesPaths = new ArrayList<>();
            ArrayList<Long> serverFilesLastModified = new ArrayList<>(amountOfFiles);
            ArrayList<Long> clientFilesLastModified = new ArrayList<>();

            for (int i = 0; i < amountOfFiles; i++) {
                serverFilesLastModified.add(i, connection.readLong());
                //tmp string path converter
                String tmpPath = serverFilesPaths.get(i).replaceAll("Server", "Client");
                clientFilesPaths.add(tmpPath);
                //
            }


            for (String clientFilePath : clientFilesPaths) {
                clientFilesLastModified.add(new File(clientFilePath).lastModified());
            }

            ArrayList<String> clientNotExistingFilesPaths = fileOperation.getNotExistingFilesPaths(clientFilesPaths,
                    clientFilesLastModified);

            ArrayList<String> clientFilesPathsToSend = fileOperation.getFilesPathsToSend(clientFilesPaths,
                    clientFilesLastModified,
                    serverFilesLastModified);

            ArrayList<String> clientFilesPathsToReceive = fileOperation.getFilesPathsToReceive(clientFilesPaths,
                    clientFilesLastModified,
                    serverFilesLastModified);

            ArrayList<String> upToDateFilesPaths = fileOperation.getUpToDateFilesPaths(clientFilesPaths,
                    clientFilesLastModified,
                    serverFilesLastModified);

            ArrayList<String> serverFilesPathsToDelete = new ArrayList<>();
            fileOperation.checkToUpdateFromUser(clientNotExistingFilesPaths, clientFilesPathsToReceive, serverFilesPathsToDelete);

            fileOperation.createMissingFolders(clientFilesPathsToReceive);
            fileOperation.addMissingClientFiles(MAIN_DIR, clientFilesPathsToSend, clientFilesPathsToReceive, upToDateFilesPaths);

            connection.writeInt(clientFilesPathsToSend.size());
            connection.writeInt(clientFilesPathsToReceive.size());
            connection.writeInt(serverFilesPathsToDelete.size());
            connection.sendPathsArray(clientFilesPathsToSend);
            connection.sendPathsArray(clientFilesPathsToReceive);
            connection.sendPathsArray(serverFilesPathsToDelete);

            if(clientFilesPathsToSend.size() == 0 && clientFilesPathsToReceive.size() == 0) {
                System.out.println("All files is up-to-date");
            }

            for (String fileToSendPath : clientFilesPathsToSend) {
                if (clientFilesPathsToSend.size() > 0) {
                    File fileToSend = new File(fileToSendPath);
                    connection.sendFile(fileToSend);
                }
            }
            for (String fileToReceivePath : clientFilesPathsToReceive) {
                if (clientFilesPathsToReceive.size() > 0) {
                    File fileToReceive = new File(fileToReceivePath);
                    connection.receiveFile(fileToReceive);
                }
            }
            connection.sendMetadata(clientFilesPathsToSend);
            connection.receiveMetadata(clientFilesPathsToReceive);

            System.out.println("Synchronization completed successfully");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.stopConnection();
        }
    }
}
