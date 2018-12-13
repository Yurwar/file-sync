package com.filesync;


import java.io.*;
import java.net.*;

public class Server {
    private Socket socket;
    private ServerSocket serverSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final int PORT_NUMBER;
    private final String DIR_PATH = "./ServerFolder";

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

            File file = new File("./ServerFolder/redminimalist.jpg");
            sendFile(file);

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
