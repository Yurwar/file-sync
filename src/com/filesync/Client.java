package com.filesync;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class Client {
    private String serverAddr;
    private final int PORT_NUMBER;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String DIR_PATH = "./ClientFolder";
    private final File MAIN_DIR = new File(DIR_PATH);


    public Client(String serverAddr, int port_number) {
        this.serverAddr = serverAddr;
        this.PORT_NUMBER = port_number;
    }

    public void startClient() {
        try {
            socket = new Socket(serverAddr, PORT_NUMBER);
            if (socket.isConnected()) {
                System.out.println("Connected");
            }
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
            DataInputStream din = new DataInputStream(socket.getInputStream());
            int amountOfFiles = din.read();
            System.out.println(amountOfFiles);

            ArrayList<String> serverFilesPaths = new ArrayList<>(amountOfFiles);
            ArrayList<String> clientFilesPaths = new ArrayList<>();
            ArrayList<Long> serverFilesLastModified = new ArrayList<>(amountOfFiles);
            ArrayList<Long> clientFilesLastModified = new ArrayList<>();
            for (int i = 0; i < amountOfFiles; i++) {
                serverFilesPaths.add(i, din.readUTF());
                serverFilesLastModified.add(i, din.readLong());
                //tmp string path converter
                String tmpPath = serverFilesPaths.get(i).replaceAll("Server", "Client");
                //
                clientFilesPaths.add(tmpPath);
            }
            for(String clientFilePath : clientFilesPaths) {
                clientFilesLastModified.add(new File(clientFilePath).lastModified());
            }
            /* Debug
            for (int i = 0; i < serverFilesPaths.size(); i++) {
                System.out.println(serverFilesPaths.get(i));
                System.out.println(clientFilesPaths.get(i));
                System.out.println(serverFilesLastModified.get(i));
                System.out.println(clientFilesLastModified.get(i));
            }
            */
            //Checking for update
            ArrayList<String> clientNotExistingFilesPaths = new ArrayList<>();
            ArrayList<String> clientFilesPathsToReceive = new ArrayList<>();
            ArrayList<String> clientFilesPathsToSend = new ArrayList<>();
            for(int i = 0; i < serverFilesPaths.size(); i++) {
                if(clientFilesLastModified.get(i) == 0) {
                    clientNotExistingFilesPaths.add(clientFilesPaths.get(i));
                } else if (clientFilesLastModified.get(i) > serverFilesLastModified.get(i)) {
                    clientFilesPathsToSend.add(clientFilesPaths.get(i));
                } else if (clientFilesLastModified.get(i) < serverFilesLastModified.get(i)) {
                    clientFilesPathsToReceive.add(clientFilesPaths.get(i));
                }
            }

            System.out.println("Not existing files on client:");
            for(String notExistingFile : clientNotExistingFilesPaths) {
                System.out.println(notExistingFile);
            }
            System.out.println("Files to send to server");
            for (String fileToSend : clientFilesPathsToSend) {
                System.out.println(fileToSend);
            }

            ArrayList<String> serverFilesPathsToDelete = new ArrayList<>();
            checkToUpdateFromUser(clientNotExistingFilesPaths, clientFilesPathsToReceive, serverFilesPathsToDelete);

            System.out.println("Checking not existing files");
            System.out.println("Files to delete from server");
            for(String fileToDelete : serverFilesPathsToDelete) {
                System.out.println(fileToDelete);
            }
            System.out.println("Files to receive to client");
            for(String fileToReceive : clientFilesPathsToReceive) {
                System.out.println(fileToReceive);
            }

            createMissingFolders(clientFilesPathsToReceive);

            dout.writeInt(clientFilesPathsToSend.size());
            dout.writeInt(clientFilesPathsToReceive.size());
            dout.writeInt(serverFilesPathsToDelete.size());
            for(String filePath : clientFilesPathsToSend) {
                dout.writeUTF(filePath);
            }
            for(String filePath : clientFilesPathsToReceive) {
                dout.writeUTF(filePath);
            }
            for(String filePath : serverFilesPathsToDelete) {
                dout.writeUTF(filePath);
            }

            for(String fileToSend : clientFilesPathsToSend) {
                sendFile(new File(fileToSend));
            }
            for(String fileToReceive : clientFilesPathsToReceive) {
                receiveFile(new File(fileToReceive));
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

    public void sendFile(String pathToFile) throws IOException {
        File file = new File("pathToFile");
        sendFile(file);
    }

    public void sendFile(File file) throws IOException {
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
    }

    public void receiveFile(File file) throws IOException {
        byte[] buffer = new byte[socket.getReceiveBufferSize()];
        OutputStream fileOutputStream = new FileOutputStream(file);

        int bytesWrite = 0;

        while ((bytesWrite = in.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesWrite);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        System.out.println("File received successfully");

        reinitConnection();
    }

    private void reinitConnection() throws IOException {
        stopClient();
        socket = new Socket(serverAddr, PORT_NUMBER);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    private ArrayList<String> getNotExistingFiles(ArrayList<String> serverFilesPaths) {
        ArrayList<String> notExistingFiles = new ArrayList<>();
        for(String filePath : serverFilesPaths) {
            File checkFile = new File("filePath");
            if(!checkFile.exists()) {
                notExistingFiles.add(filePath);
            }
        }
        return notExistingFiles;
    }
    private ArrayList<String> getExistingFiles(ArrayList<String> serverFilesPaths) {
        ArrayList<String> existingFiles = new ArrayList<>();
        for(String filePath : serverFilesPaths) {
            File checkFile = new File("filePath");
            if(checkFile.exists()) {
                existingFiles.add(filePath);
            }
        }
        return existingFiles;
    }
    private ArrayList<Long> getLastModified(ArrayList<String> filesPaths) {
        ArrayList<Long> filesLastModified = new ArrayList<>();
        for(String filePath : filesPaths) {
            filesLastModified.add(new File(filePath).lastModified());
        }
        return filesLastModified;
    }
    private void checkToUpdateFromUser(ArrayList<String> clientNotExistingFiles,
                                         ArrayList<String> filesToReceiveFromServer,
                                         ArrayList<String> filesToDeleteFromServer) {
        if(clientNotExistingFiles.isEmpty()) {
            return;
        }
        Iterator<String> it = clientNotExistingFiles.iterator();
        String condition;
        String filePath;
        while(it.hasNext()) {
            filePath = it.next();
            System.out.println("File " + filePath + " is existing on server, but not on a client\n"
                    + "Delete it from server, receive to client or do nothing(d/r/N)");
            Scanner sc = new Scanner(System.in);
            condition = sc.nextLine();
            if(condition.equalsIgnoreCase("d")) {
                filesToDeleteFromServer.add(filePath);
            } else if(condition.equalsIgnoreCase("r")) {
                filesToReceiveFromServer.add(filePath);
            }
        }
    }
    private void createMissingFolders(ArrayList<String> filesToReceive) {
        String folderPath;
        for(String fileToRecieve : filesToReceive) {
            folderPath = fileToRecieve.substring(0, fileToRecieve.lastIndexOf("/"));
            File folder = new File(folderPath);
            if(!folder.exists()) {
                folder.mkdirs();
            }
        }
    }
}
