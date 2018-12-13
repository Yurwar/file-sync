package com.filesync;

import java.io.*;
import java.net.*;

public class Client {
    private String serverAddr;
    private final int PORT_NUMBER;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String DIR_PATH = "./ClientFolder";


    public Client(String serverAddr, int port_number) {
        this.serverAddr = serverAddr;
        this.PORT_NUMBER = port_number;
    }

    public void startClient() {
        try {
            socket = new Socket(serverAddr, PORT_NUMBER);
            if(socket.isConnected()) {
                System.out.println("Connected");
            }
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            recieveFile(new File("./ClientFolder/redminimalist.jpg"));
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

        while((bytesRead = fileInputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
        fileInputStream.close();
    }
    public void recieveFile(File file) throws IOException {
        byte[] buffer = new byte[socket.getReceiveBufferSize()];
        OutputStream fileOutputStream = new FileOutputStream(file);

        int bytesWrite = 0;

        while((bytesWrite = in.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesWrite);
        }
        fileOutputStream.flush();
        fileOutputStream.close();

    }
    private void reinitConnection() throws IOException {
        stopClient();
        socket = new Socket(serverAddr, PORT_NUMBER);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }
}
