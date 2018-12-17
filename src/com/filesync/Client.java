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
    private DataOutputStream dout;
    private DataInputStream din;
    private final String DIR_PATH;
    private final File MAIN_DIR;


    public Client(String serverAddr, int port_number, String dir_path) {
        this.serverAddr = serverAddr;
        this.PORT_NUMBER = port_number;
        this.DIR_PATH = dir_path;
        this.MAIN_DIR = new File(DIR_PATH);
    }

    public void startClient() {
        try {
            socket = new Socket(serverAddr, PORT_NUMBER);
            if (socket.isConnected()) {
                System.out.println("Connected");
            }
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            dout = new DataOutputStream(socket.getOutputStream());
            din = new DataInputStream(socket.getInputStream());
            FileOperation fileOperation = new FileOperation(/*dout, din*/);

            int amountOfFiles = din.read();
//DONE
            System.out.println("Amount of files on server: " + amountOfFiles);

            ArrayList<String> serverFilesPaths = fileOperation.receivePathsArray(amountOfFiles, din);
            ArrayList<String> clientFilesPaths = new ArrayList<>();
            ArrayList<Long> serverFilesLastModified = new ArrayList<>(amountOfFiles);
            ArrayList<Long> clientFilesLastModified = new ArrayList<>();
//DONE
            System.out.println("Files paths on server: ");
            for(String filePath : serverFilesPaths) {
                System.out.println(filePath);
            }

            for (int i = 0; i < amountOfFiles; i++) {
                serverFilesLastModified.add(i, din.readLong());
                //tmp string path converter
                String tmpPath = serverFilesPaths.get(i).replaceAll("Server", "Client");
                //
                clientFilesPaths.add(tmpPath);
            }

            System.out.println("Files paths on client: ");
            for(String filePath : clientFilesPaths) {
                System.out.println(filePath);
            }


            for(String clientFilePath : clientFilesPaths) {
                clientFilesLastModified.add(new File(clientFilePath).lastModified());
            }

            System.out.println("Files paths last modified client: ");
            for(Long lastModified : clientFilesLastModified) {
                System.out.println(lastModified);
            }

            //Checking for update
            ArrayList<String> clientNotExistingFilesPaths = fileOperation.getNotExistingFilesPaths(clientFilesPaths,
                    clientFilesLastModified);

            System.out.println("Not existing files on client:");
            for(String notExistingFile : clientNotExistingFilesPaths) {
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
            for(String filePath : clientFilesPathsToReceive) {
                System.out.println(filePath);
            }


            ArrayList<String> serverFilesPathsToDelete = new ArrayList<>();
            fileOperation.checkToUpdateFromUser(clientNotExistingFilesPaths, clientFilesPathsToReceive, serverFilesPathsToDelete);

            System.out.println("Checking not existing files");
            System.out.println("Files to delete from server");
            for(String fileToDelete : serverFilesPathsToDelete) {
                System.out.println(fileToDelete);
            }
            System.out.println("Files to receive to client");
            for(String fileToReceive : clientFilesPathsToReceive) {
                System.out.println(fileToReceive);
            }

            fileOperation.createMissingFolders(clientFilesPathsToReceive);
            fileOperation.addMissingClientFiles(MAIN_DIR, clientFilesPathsToSend, clientFilesPathsToReceive);




            dout.writeInt(clientFilesPathsToSend.size());
            dout.writeInt(clientFilesPathsToReceive.size());
            dout.writeInt(serverFilesPathsToDelete.size());
            fileOperation.sendPathsArray(clientFilesPathsToSend, dout);
            fileOperation.sendPathsArray(clientFilesPathsToReceive, dout);
            fileOperation.sendPathsArray(serverFilesPathsToDelete, dout);
            System.out.println("Files to send to server");
            for (String fileToSend : clientFilesPathsToSend) {
                System.out.println(fileToSend);
            }
            System.out.println("Files paths to receive: ");
            for(String filePath : clientFilesPathsToReceive) {
                System.out.println(filePath);
            }
            System.out.println("Files paths to delete: ");
            for(String filePath : serverFilesPathsToDelete) {
                System.out.println(filePath);
            }
            for(String fileToSend : clientFilesPathsToSend) {
                if(clientFilesPathsToSend.size() > 0) {
                    sendFile(new File(fileToSend));
                }
            }
            for(String fileToReceive : clientFilesPathsToReceive) {
                if(clientFilesPathsToReceive.size() > 0) {
                    receiveFile(new File(fileToReceive));
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
                dout.close();
                din.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

                reinitConnection();
            } else {
                throw new FileNotFoundException();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

            reinitConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } /*finally {
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
        }*/
    }

    private void reinitConnection() throws IOException {
        stopClient();
        socket = new Socket(serverAddr, PORT_NUMBER);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }
}
