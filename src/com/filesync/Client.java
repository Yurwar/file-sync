package com.filesync;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

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

            DataInputStream din = new DataInputStream(socket.getInputStream());
            int amountOfFiles = din.read();
            System.out.println(amountOfFiles);

            ArrayList<String> serverFilesPaths = new ArrayList<>(amountOfFiles);
            ArrayList<String> clientFilesPaths = new ArrayList<>();
            ArrayList<Long> serverFilesLastModified = new ArrayList<>(amountOfFiles);
            for (int i = 0; i < amountOfFiles; i++) {
                serverFilesPaths.add(i, din.readUTF());
                serverFilesLastModified.add(i, din.readLong());
                //tmp string path converter
                String tmpPath = serverFilesPaths.get(i).replaceAll("Server", "Client");
                //
                clientFilesPaths.add(tmpPath);
            }
            for (int i = 0; i < serverFilesPaths.size(); i++) {
                System.out.println(serverFilesPaths.get(i));
                System.out.println(clientFilesPaths.get(i));
                System.out.println(serverFilesLastModified.get(i));
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

    public void recieveFile(File file) throws IOException {
        byte[] buffer = new byte[socket.getReceiveBufferSize()];
        OutputStream fileOutputStream = new FileOutputStream(file);

        int bytesWrite = 0;

        while ((bytesWrite = in.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesWrite);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
        System.out.println("File recieved successfully");

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
}
