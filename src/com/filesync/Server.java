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
    private final String DIR_PATH = "./ServerFolder";
    private final File MAIN_DIR = new File(DIR_PATH);

    public Server(int port_number) {
        this.PORT_NUMBER = port_number;
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
            ArrayList<String> serverFilesPaths = new ArrayList<>();
            getAllFilesPaths(MAIN_DIR, serverFilesPaths);

            int amountOfFiles = serverFilesPaths.size();
            dout.write(amountOfFiles);
            for(String filePath : serverFilesPaths) {
                dout.writeUTF(filePath);
                dout.writeLong(new File(filePath).lastModified());
                System.out.println(filePath);
                System.out.println(new File(filePath).lastModified());
            }

            int pathsToSendSize = din.readInt();
            int pathsToReceiveSize = din.readInt();
            int pathsToDeleteSize = din.readInt();
            System.out.println(pathsToSendSize + " " + pathsToReceiveSize + " " + pathsToDeleteSize);
            ArrayList<String> clientFilesPathsToSend = new ArrayList<>(pathsToSendSize);
            ArrayList<String> clientFilesPathsToReceive = new ArrayList<>(pathsToReceiveSize);
            ArrayList<String> serverFilesPathsToDelete = new ArrayList<>(pathsToDeleteSize);
            System.out.println(clientFilesPathsToSend.size());
            System.out.println("Client files paths to send: ");
            for(int i = 0; i < pathsToSendSize; i++) {
                clientFilesPathsToSend.add(i, din.readUTF());
                //tmp String path converter
                String tmpPath = clientFilesPathsToSend.get(i).replaceAll("Client", "Server");
                clientFilesPathsToSend.set(i, tmpPath);
                //
                System.out.println(clientFilesPathsToSend.get(i));

            }

            System.out.println("Client files paths to receive: ");
            for(int i = 0; i < pathsToReceiveSize; i++) {
                clientFilesPathsToReceive.add(i, din.readUTF());
                //tmp String path converter
                String tmpPath = clientFilesPathsToReceive.get(i).replaceAll("Client", "Server");
                clientFilesPathsToReceive.set(i, tmpPath);
                //
                System.out.println(clientFilesPathsToReceive.get(i));
            }

            System.out.println("Server files paths to delete: ");
            for(int i = 0; i < pathsToDeleteSize; i++) {
                serverFilesPathsToDelete.add(i, din.readUTF());
                //tmp String path converter
                String tmpPath = serverFilesPathsToDelete.get(i).replaceAll("Client", "Server");
                serverFilesPathsToDelete.set(i, tmpPath);
                //
                System.out.println(serverFilesPathsToDelete.get(i));
            }

            deleteFiles(serverFilesPathsToDelete);
            for(String fileToReceive : clientFilesPathsToSend) {
                receiveFile(new File(fileToReceive));
            }
            for(String fileToSend : clientFilesPathsToReceive) {
                sendFile(new File(fileToSend));
            }

            socket.close();
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

        while((bytesRead = fileInputStream.read(buffer)) != -1) {
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
    private ArrayList<String> getAllFilesPaths(File mainFolder, ArrayList<String> filesPaths) {
        File[] folderEntries = mainFolder.listFiles();
        for(File entry : folderEntries) {
            if(entry.isDirectory()) {
                getAllFilesPaths(entry, filesPaths);
            } else {
                filesPaths.add(entry.getPath());
            }
        }
        return filesPaths;
    }
    private ArrayList<Long> getLastModified(ArrayList<String> filesPaths) {
        ArrayList<Long> filesLastModified = new ArrayList<>();
        for(String filePath : filesPaths) {
            filesLastModified.add(new File(filePath).lastModified());
        }
        return filesLastModified;
    }
    private void deleteFiles(ArrayList<String> filesPathsToDelete) {
        File fileToDelete;
        for(String filePath : filesPathsToDelete) {
            fileToDelete = new File(filePath);
            if(!fileToDelete.delete()) {
                System.out.println("Can't delete file " + filePath);
            }
        }
    }

}
