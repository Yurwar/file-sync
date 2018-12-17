package com.filesync;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {
    private Socket socket;
    private ServerSocket serverSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final int PORT_NUMBER;
    private final String DIR_PATH;
    private final File MAIN_DIR;

    public Server(int port_number, String dir_path) {
        this.PORT_NUMBER = port_number;
        this.DIR_PATH = dir_path;
        this.MAIN_DIR = new File(DIR_PATH);
    }

    public void startServer() throws IOException {
        System.out.println("Starting server...");
        serverSocket = new ServerSocket(PORT_NUMBER, 1);
        while(true) {
            socket = serverSocket.accept();
            System.out.println("Listening to the client...");
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            DataInputStream din = new DataInputStream(socket.getInputStream());
            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());

            FileOperation fileOperation = new FileOperation(/*dout, din*/);

            ArrayList<String> serverFilesPaths = fileOperation.getAllFilesPaths(MAIN_DIR);
            int amountOfFiles = serverFilesPaths.size();
            dout.write(amountOfFiles);
            fileOperation.sendPathsArray(serverFilesPaths, dout);

            for(String filePath : serverFilesPaths) {
                dout.writeLong(new File(filePath).lastModified());
            }

            int pathsToSendSize = din.readInt();
            int pathsToReceiveSize = din.readInt();
            int pathsToDeleteSize = din.readInt();
            System.out.println(pathsToSendSize + " " + pathsToReceiveSize + " " + pathsToDeleteSize);
            ArrayList<String> clientFilesPathsToSend = fileOperation.receivePathsArray(pathsToSendSize, din);
            ArrayList<String> clientFilesPathsToReceive = fileOperation.receivePathsArray(pathsToReceiveSize, din);
            ArrayList<String> serverFilesPathsToDelete = fileOperation.receivePathsArray(pathsToDeleteSize, din);

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

            System.out.println("Files paths to delete: ");
            System.out.println(serverFilesPathsToDelete);
            for(String filePath : serverFilesPathsToDelete) {
                System.out.println(filePath);
            }

            fileOperation.deleteFiles(serverFilesPathsToDelete);
            fileOperation.createMissingFolders(clientFilesPathsToSend);

            for(String fileToReceive : clientFilesPathsToSend) {
                if(clientFilesPathsToSend.size() > 0) {
                    receiveFile(new File(fileToReceive));
                }
            }
            for(String fileToSend : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    sendFile(new File(fileToSend));
                }
            }

            socket.close();
        }
    }

    public void sendFile(File file) {
        try {
            if(file.exists()) {
                byte[] buffer = new byte[socket.getSendBufferSize()];
                InputStream fileInputStream = new FileInputStream(file);

                int bytesRead = 0;

                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
                fileInputStream.close();
                System.out.println("File send successfully");

                reinitConnection();
            } else {
                throw new FileNotFoundException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveFile(File file) throws IOException {
        byte[] buffer = new byte[socket.getReceiveBufferSize()];
        OutputStream fileOutputStream = new FileOutputStream(file);

        int bytesWrite = 0;

        while((bytesWrite = in.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesWrite);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        System.out.println("File received successfully");


        reinitConnection();
    }
    public void stopServer() {
        try {
            in.close();
            out.close();
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void reinitConnection() throws IOException {
        socket.close();
        in.close();
        out.close();
        socket = serverSocket.accept();
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

}
