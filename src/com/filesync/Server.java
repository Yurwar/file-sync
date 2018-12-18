package com.filesync;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server {
    private Socket socket;
    private ServerSocket serverSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private DataInputStream din;
    private DataOutputStream dout;
    private final int PORT_NUMBER;
    private final String DIR_PATH;
    private final File MAIN_DIR;

    public Server(int port_number, String dir_path) {
        this.PORT_NUMBER = port_number;
        this.DIR_PATH = dir_path;
        this.MAIN_DIR = new File(DIR_PATH);
        try {
            serverSocket = new ServerSocket(PORT_NUMBER, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sync() throws IOException {
        System.out.println("Start sync server on port " + serverSocket.getLocalPort());
        while(true) {
            socket = serverSocket.accept();
            System.out.println("Client connected");
            System.out.println("Sync with client " + socket.getInetAddress().toString() + ":" + PORT_NUMBER);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            din = new DataInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());

            FileOperation fileOperation = new FileOperation(dout, din);

            ArrayList<String> serverFilesPaths = fileOperation.getAllFilesPaths(MAIN_DIR);
            int amountOfFiles = serverFilesPaths.size();
            dout.write(amountOfFiles);
            fileOperation.sendPathsArray(serverFilesPaths);

            for(String filePath : serverFilesPaths) {
                dout.writeLong(new File(filePath).lastModified());
            }

            int pathsToSendSize = din.readInt();
            int pathsToReceiveSize = din.readInt();
            int pathsToDeleteSize = din.readInt();
            System.out.println(pathsToSendSize + " " + pathsToReceiveSize + " " + pathsToDeleteSize);
            ArrayList<String> clientFilesPathsToSend = fileOperation.receivePathsArray(pathsToSendSize);
            ArrayList<String> clientFilesPathsToReceive = fileOperation.receivePathsArray(pathsToReceiveSize);
            ArrayList<String> serverFilesPathsToDelete = fileOperation.receivePathsArray(pathsToDeleteSize);

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

            for(String fileToReceivePath : clientFilesPathsToSend) {
                if(clientFilesPathsToSend.size() > 0) {
                    File fileToReceive = new File(fileToReceivePath);
                    receiveFile(fileToReceive);
                }
            }

            for(String fileToSendPath : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    File fileToSend = new File(fileToSendPath);
                    sendFile(fileToSend);
                }
            }
            for(String fileToReceive : clientFilesPathsToSend) {
                if(clientFilesPathsToSend.size() > 0) {
                    File file = new File(fileToReceive);
                    din = new DataInputStream(socket.getInputStream());
                    long lastModified = din.readLong();
                    file.setLastModified(lastModified);
                }
            }
            for(String fileToSend : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    File file = new File(fileToSend);
                    long lastModified = file.lastModified();
                    dout = new DataOutputStream(socket.getOutputStream());
                    dout.writeLong(lastModified);
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

            } else {
                throw new FileNotFoundException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reconnect();
        }
    }

    public void receiveFile(File file) {
        try {
            byte[] buffer = new byte[socket.getReceiveBufferSize()];
            OutputStream fileOutputStream = new FileOutputStream(file);

            int bytesWrite = 0;

            while ((bytesWrite = in.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesWrite);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            System.out.println("File received successfully");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reconnect();
        }
    }

    private void reconnect() {
        try {
            socket.close();
            out.close();
            in.close();
            socket = serverSocket.accept();
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        try {
            in.close();
            out.close();
            din.close();
            dout.close();
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
