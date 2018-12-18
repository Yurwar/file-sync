package com.filesync;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Client {
    private String serverAddr;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DataOutputStream dout;
    private DataInputStream din;
    private final int PORT_NUMBER;
    private final String DIR_PATH;
    private final File MAIN_DIR;


    public Client(String serverAddr, int port_number, String dir_path) {
        this.serverAddr = serverAddr;
        this.PORT_NUMBER = port_number;
        this.DIR_PATH = dir_path;
        this.MAIN_DIR = new File(DIR_PATH);
        try {
            socket = new Socket(serverAddr, PORT_NUMBER);
            if (socket.isConnected()) {
                System.out.println("Connected to server on address " + socket.getInetAddress() + ":" + socket.getPort());
            }
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            dout = new DataOutputStream(socket.getOutputStream());
            din = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sync() {
        try {
            System.out.println(convertPath("./Sadqwr/asdqw/"));
            FileOperation fileOperation = new FileOperation(dout, din);

            int amountOfFiles = din.read();

            System.out.println("Amount of files on server: " + amountOfFiles);

            ArrayList<String> serverFilesPaths = fileOperation.receivePathsArray(amountOfFiles);
            ArrayList<String> clientFilesPaths = new ArrayList<>();
            ArrayList<Long> serverFilesLastModified = new ArrayList<>(amountOfFiles);
            ArrayList<Long> clientFilesLastModified = new ArrayList<>();

            for (int i = 0; i < amountOfFiles; i++) {
                serverFilesLastModified.add(i, din.readLong());
                //tmp string path converter
                String tmpPath = serverFilesPaths.get(i).replaceAll("Server", "Client");
                //
                clientFilesPaths.add(tmpPath);
            }


            for (String clientFilePath : clientFilesPaths) {
                clientFilesLastModified.add(new File(clientFilePath).lastModified());
            }

            System.out.println("Files paths last modified client: ");
            for (Long lastModified : clientFilesLastModified) {
                System.out.println(lastModified);
            }
            System.out.println("Files paths last modified server: ");
            for (Long lastModified : serverFilesLastModified) {
                System.out.println(lastModified);
            }

            //Checking for update
            ArrayList<String> clientNotExistingFilesPaths = fileOperation.getNotExistingFilesPaths(clientFilesPaths,
                    clientFilesLastModified);

            System.out.println("Not existing files on client:");
            for (String notExistingFile : clientNotExistingFilesPaths) {
                System.out.println(notExistingFile);
            }

            ArrayList<String> clientFilesPathsToSend = fileOperation.getFilesPathsToSend(clientFilesPaths,
                    clientFilesLastModified,
                    serverFilesLastModified);

            System.out.println("Files to send to server");
            for (String fileToSend : clientFilesPathsToSend) {
                System.out.println(fileToSend);
            }

            ArrayList<String> clientFilesPathsToReceive = fileOperation.getFilesPathsToReceive(clientFilesPaths,
                    clientFilesLastModified,
                    serverFilesLastModified);

            System.out.println("Client files paths to receive: ");
            for (String filePath : clientFilesPathsToReceive) {
                System.out.println(filePath);
            }

            ArrayList<String> upToDateFilesPaths = fileOperation.getUpToDateFilesPaths(clientFilesPaths,
                    clientFilesLastModified,
                    serverFilesLastModified);

            System.out.println("Up-To-Date files: ");
            for (String filePath : upToDateFilesPaths) {
                System.out.println(filePath);
            }

            ArrayList<String> serverFilesPathsToDelete = new ArrayList<>();
            fileOperation.checkToUpdateFromUser(clientNotExistingFilesPaths, clientFilesPathsToReceive, serverFilesPathsToDelete);

            System.out.println("Checking not existing files");
            System.out.println("Files to delete from server");
            for (String fileToDelete : serverFilesPathsToDelete) {
                System.out.println(fileToDelete);
            }
            System.out.println("Files to receive to client");
            for (String fileToReceive : clientFilesPathsToReceive) {
                System.out.println(fileToReceive);
            }

            fileOperation.createMissingFolders(clientFilesPathsToReceive);
            fileOperation.addMissingClientFiles(MAIN_DIR, clientFilesPathsToSend, clientFilesPathsToReceive, upToDateFilesPaths);

            dout.writeInt(clientFilesPathsToSend.size());
            dout.writeInt(clientFilesPathsToReceive.size());
            dout.writeInt(serverFilesPathsToDelete.size());
            fileOperation.sendPathsArray(clientFilesPathsToSend);
            fileOperation.sendPathsArray(clientFilesPathsToReceive);
            fileOperation.sendPathsArray(serverFilesPathsToDelete);
            System.out.println("Files to send to server");
            for (String fileToSend : clientFilesPathsToSend) {
                System.out.println(fileToSend);
            }
            System.out.println("Files paths to receive: ");
            for (String filePath : clientFilesPathsToReceive) {
                System.out.println(filePath);
            }
            System.out.println("Files paths to delete: ");
            for (String filePath : serverFilesPathsToDelete) {
                System.out.println(filePath);
            }

            for (String fileToSendPath : clientFilesPathsToSend) {
                if (clientFilesPathsToSend.size() > 0) {
                    File fileToSend = new File(fileToSendPath);
                    sendFile(fileToSend);

                }
            }
            for (String fileToReceivePath : clientFilesPathsToReceive) {
                if (clientFilesPathsToReceive.size() > 0) {
                    File fileToReceive = new File(fileToReceivePath);
                    receiveFile(fileToReceive);
                }
            }
            for(String fileToSendPath : clientFilesPathsToSend) {
                if (clientFilesPathsToSend.size() > 0) {
                    File file = new File(fileToSendPath);
                    long lastModified = file.lastModified();
                    dout = new DataOutputStream(socket.getOutputStream());
                    dout.writeLong(lastModified);
                }
            }
            for(String fileToReceivePath : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    File file = new File(fileToReceivePath);
                    din = new DataInputStream(socket.getInputStream());
                    long lastModified = din.readLong();
                    file.setLastModified(lastModified);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopClient() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            socket = new Socket(serverAddr, PORT_NUMBER);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String convertPath(String senderPath) {
        String folderToSyncName;
        System.out.println(DIR_PATH);
        if(DIR_PATH.lastIndexOf("/") == DIR_PATH.length() - 1) {
            folderToSyncName = DIR_PATH.substring(DIR_PATH.lastIndexOf("/", DIR_PATH.length() - 2) + 1, DIR_PATH.lastIndexOf("/"));
            System.out.println(folderToSyncName);
        } else {
            folderToSyncName = DIR_PATH.substring(DIR_PATH.lastIndexOf("/") + 1, DIR_PATH.length());
            System.out.println(folderToSyncName);
        }
        if(senderPath.charAt(0) == '.') {
            if(DIR_PATH.charAt(0) == '.') {
                return senderPath;
            } else {

            }
        }
        return folderToSyncName;
    }
}
