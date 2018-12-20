package com.filesync;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Client {
    private Socket socket;
    private final String DIR_PATH;
    private final File MAIN_DIR;
    private Connection connection;

    public Client(String serverAddr, int port_number, String dir_path) {
        this.DIR_PATH = dir_path;
        this.MAIN_DIR = new File(DIR_PATH);
        try {
            socket = new Socket(serverAddr, port_number);
            if (socket.isConnected()) {
                System.out.println("Connected to server on address " + socket.getInetAddress() + ":" + socket.getPort());
            }
            connection = new Connection(socket, serverAddr, port_number);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sync() {
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
                //
                clientFilesPaths.add(tmpPath);
            }


            for (String clientFilePath : clientFilesPaths) {
                clientFilesLastModified.add(new File(clientFilePath).lastModified());
            }

            //Checking for update
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
            for(String fileToSendPath : clientFilesPathsToSend) {
                if (clientFilesPathsToSend.size() > 0) {
                    File file = new File(fileToSendPath);
                    long lastModified = file.lastModified();
                    connection.sendMetadata(lastModified);
                }
            }
            for(String fileToReceivePath : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    File file = new File(fileToReceivePath);
                    long lastModified = connection.receiveMetadata();
                    file.setLastModified(lastModified);
                }
            }

            System.out.println("Synchronization completed successfully");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            stopClient();
        }
    }

    public void stopClient() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
