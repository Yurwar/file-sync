package com.filesync;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {
    private Socket socket;
    private ServerSocket serverSocket;
    private final File MAIN_DIR;
    private final String DIR_PATH;
    private Connection connection;

    public Server(int port_number, String dir_path) {
        try {
            serverSocket = new ServerSocket(port_number, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.DIR_PATH = dir_path;
        this.MAIN_DIR = new File(DIR_PATH);
    }

    public void sync() throws IOException {
        System.out.println("Start server on port " + serverSocket.getLocalPort());
        while(true) {
            socket = serverSocket.accept();
            connection = new Connection(serverSocket, socket, serverSocket.getLocalPort());
            System.out.println("Client connected");
            System.out.println("Sync with client " + socket.getInetAddress().toString() + ":" + serverSocket.getLocalPort());

            FileOperation fileOperation = new FileOperation();

            ArrayList<String> serverFilesPaths = fileOperation.getAllFilesPaths(MAIN_DIR);
            int amountOfFiles = serverFilesPaths.size();
            connection.writeInt(amountOfFiles);
            connection.sendPathsArray(serverFilesPaths);

            for(String filePath : serverFilesPaths) {
                connection.writeLong(new File(filePath).lastModified());
            }

            int pathsToSendSize = connection.readInt();
            int pathsToReceiveSize = connection.readInt();
            int pathsToDeleteSize = connection.readInt();
            ArrayList<String> clientFilesPathsToSend = connection.receivePathsArray(pathsToSendSize);
            ArrayList<String> clientFilesPathsToReceive = connection.receivePathsArray(pathsToReceiveSize);
            ArrayList<String> serverFilesPathsToDelete = connection.receivePathsArray(pathsToDeleteSize);

            for(int i = 0; i < pathsToSendSize; i++) {
                //tmp String path converter
                String tmpPath = clientFilesPathsToSend.get(i).replaceAll("Client", "Server");
                clientFilesPathsToSend.set(i, tmpPath);
                //
            }

            for(int i = 0; i < pathsToReceiveSize; i++) {
                //tmp String path converter
                String tmpPath = clientFilesPathsToReceive.get(i).replaceAll("Client", "Server");
                clientFilesPathsToReceive.set(i, tmpPath);
                //
            }

            for(int i = 0; i < pathsToDeleteSize; i++) {
                //tmp String path converter
                String tmpPath = serverFilesPathsToDelete.get(i).replaceAll("Client", "Server");
                serverFilesPathsToDelete.set(i, tmpPath);
                //
            }

            fileOperation.printArray(serverFilesPathsToDelete, "Files to delete from server");
            fileOperation.deleteFiles(serverFilesPathsToDelete);

            fileOperation.createMissingFolders(clientFilesPathsToSend);

            if(clientFilesPathsToSend.size() == 0 && clientFilesPathsToReceive.size() == 0) {
                System.out.println("All files is up-to-date");
            }

            for(String fileToReceivePath : clientFilesPathsToSend) {
                if(clientFilesPathsToSend.size() > 0) {
                    File fileToReceive = new File(fileToReceivePath);
                    connection.receiveFile(fileToReceive);
                }
            }

            for(String fileToSendPath : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    File fileToSend = new File(fileToSendPath);
                    connection.sendFile(fileToSend);
                }
            }
            for(String fileToReceivePath : clientFilesPathsToSend) {
                if(clientFilesPathsToSend.size() > 0) {
                    File file = new File(fileToReceivePath);
                    long lastModified = connection.receiveMetadata();
                    file.setLastModified(lastModified);
                }
            }
            for(String fileToSendPath : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    File file = new File(fileToSendPath);
                    long lastModified = file.lastModified();
                    connection.sendMetadata(lastModified);
                }
            }

            socket.close();
            System.out.println("Synchronization completed successfully");
            System.out.println("Client disconnected\n");
        }
    }

    public void stopServer() {
        try {
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
