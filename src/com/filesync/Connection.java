package com.filesync;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

class Connection {
    private Socket socket;
    private ServerSocket serverSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DataOutputStream dout;
    private DataInputStream din;
    private String serverAddr;
    private int PORT_NUMBER;

    Connection(String serverAddr, int PORT_NUMBER) {
        this.serverAddr = serverAddr;
        this.PORT_NUMBER = PORT_NUMBER;
        try {
            this.socket = new Socket(serverAddr, PORT_NUMBER);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            this.dout = new DataOutputStream(socket.getOutputStream());
            this.din = new DataInputStream(socket.getInputStream());
            if (socket.isConnected()) {
                System.out.println("Connected to server on address " + socket.getInetAddress() + ":" + socket.getPort());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    Connection(int PORT_NUMBER) {
        this.PORT_NUMBER = PORT_NUMBER;
        try {
            this.serverSocket = new ServerSocket(PORT_NUMBER, 1);
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

    void stopConnection() {
        try {
            if(serverSocket != null) {
                serverSocket.close();
            }
            socket.close();
            out.close();
            in.close();
            dout.close();
            din.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendFile(File file) {
        try {
            if(file.exists()) {
                byte[] buffer = new byte[socket.getSendBufferSize()];
                InputStream fileInputStream = new FileInputStream(file);

                int bytesRead;

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

    void receiveFile(File file) {
        try {
            byte[] buffer = new byte[socket.getReceiveBufferSize()];
            OutputStream fileOutputStream = new FileOutputStream(file);

            int bytesWrite;

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

    void sendPathsArray(ArrayList<String> pathsArray) {
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

    ArrayList<String> receivePathsArray(int arraySize) {
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

    void sendMetadata(ArrayList<String> filesPaths) {
        for(String filePath : filesPaths) {
            if (filesPaths.size() > 0) {
                File file = new File(filePath);
                long lastModified = file.lastModified();
                sendData(lastModified);
            }
        }
    }

    void receiveMetadata(ArrayList<String> filesPaths) {
        for (String filePath : filesPaths) {
            if (filesPaths.size() > 0) {
                File file = new File(filePath);
                try {
                    long lastModified = receiveData();
                    if (!file.setLastModified(lastModified)) {
                        System.err.println("Can not set last modified time\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendData(long value) {
        try {
            dout = new DataOutputStream(socket.getOutputStream());
            writeLong(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long receiveData() throws IOException {
        din = new DataInputStream(socket.getInputStream());
        return readLong();
    }

    private void reconnect() {
        closeConnection();
        connect();
    }

    int readInt() throws IOException {
        return din.readInt();
    }

    long readLong() throws IOException {
        return din.readLong();
    }

    void writeInt(int value) throws IOException {
        dout.writeInt(value);
    }

    void writeLong(long value) throws IOException {
        dout.writeLong(value);
    }

    void acceptSocket() {
        if(serverSocket != null) {
            try {
                this.socket = serverSocket.accept();
                System.out.println("Client connected");
                System.out.println("Sync with client " + socket.getInetAddress().toString() + ":" + serverSocket.getLocalPort());
                this.out = new ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());
                this.dout = new DataOutputStream(socket.getOutputStream());
                this.din = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
