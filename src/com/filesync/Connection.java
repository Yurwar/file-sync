package com.filesync;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Connection {
    private Socket socket;
    private ServerSocket serverSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DataOutputStream dout;
    private DataInputStream din;
    private String serverAddr;
    private int PORT_NUMBER;

    public Connection(Socket socket, String serverAddr, int PORT_NUMBER) {
        this.serverAddr = serverAddr;
        this.socket = socket;
        this.PORT_NUMBER = PORT_NUMBER;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.dout = new DataOutputStream(socket.getOutputStream());
            this.din = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public Connection(ServerSocket serverSocket, Socket socket, int PORT_NUMBER) {
        this.serverSocket = serverSocket;
        this.socket = socket;
        this.PORT_NUMBER = PORT_NUMBER;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.dout = new DataOutputStream(socket.getOutputStream());
            this.din = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        if(serverSocket == null) {
            try {
                socket = new Socket(serverAddr, PORT_NUMBER);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void closeConnection() {
        try {
            socket.close();
            out.close();
            in.close();
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

                System.out.println("File " + file.toString() + " send successfully");
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

            System.out.println("File " + file.toString() + " received successfully");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reconnect();
        }
    }

    public void sendPathsArray(ArrayList<String> pathsArray) {
        if(!pathsArray.isEmpty()) {
            for (String path : pathsArray) {
                try {
                    dout.writeUTF(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ArrayList<String> receivePathsArray(int arraySize) {
        ArrayList<String> pathsArray = new ArrayList<>(arraySize);
        for (int i = 0; i < arraySize; i++) {
            try {
                pathsArray.add(i, din.readUTF());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return pathsArray;
    }

    private void reconnect() {
        closeConnection();
        connect();
    }

    public int readInt() throws IOException {
        return din.readInt();
    }
    public long readLong() throws IOException {
        return din.readLong();
    }
    public void writeInt(int value) throws IOException {
        dout.writeInt(value);
    }
    public void writeLong(long value) throws IOException {
        dout.writeLong(value);
    }
    public void sendMetadata(long value) {
        try {
            dout = new DataOutputStream(socket.getOutputStream());
            writeLong(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public long receiveMetadata() throws IOException {
        din = new DataInputStream(socket.getInputStream());
        long value = readLong();
        return value;
    }
}
