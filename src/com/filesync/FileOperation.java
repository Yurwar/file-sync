package com.filesync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class FileOperation {
    private final DataOutputStream dataOutputStream;
    private final DataInputStream dataInputStream;

    public FileOperation(DataOutputStream dataOutputStream, DataInputStream dataInputStream) {
        this.dataOutputStream = dataOutputStream;
        this.dataInputStream = dataInputStream;
    }

    public void checkToUpdateFromUser(ArrayList<String> clientNotExistingFiles,
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

    public void createMissingFolders(ArrayList<String> filesToReceive) {
        String folderPath;
        for(String fileToRecieve : filesToReceive) {
            folderPath = fileToRecieve.substring(0, fileToRecieve.lastIndexOf("/"));
            File folder = new File(folderPath);
            if(!folder.exists()) {
                folder.mkdirs();
            }
        }
    }

    private void _getAllFilesPaths(File mainFolder, ArrayList<String> filesPaths) {
        File[] folderEntries = mainFolder.listFiles();
        for(File entry : folderEntries) {
            if(entry.isDirectory()) {
                _getAllFilesPaths(entry, filesPaths);
            } else {
                filesPaths.add(entry.getPath());
            }
        }
    }

    public ArrayList<String> getAllFilesPaths(File mainFolder) {
        ArrayList<String> filesPaths = new ArrayList<>();
        _getAllFilesPaths(mainFolder, filesPaths);
        return filesPaths;
    }

    public void addMissingClientFiles(File folderPath,
                                      ArrayList<String> clientFilesPathsToSend,
                                      ArrayList<String> clientFilesPathsToReceive,
                                      ArrayList<String> upToDateFilesPaths) {
        ArrayList<String> allFilesPaths = getAllFilesPaths(folderPath);
        for (String filePath : allFilesPaths) {
            if (!clientFilesPathsToSend.contains(filePath)
                    && !clientFilesPathsToReceive.contains(filePath)
                    && !upToDateFilesPaths.contains(filePath)) {
                clientFilesPathsToSend.add(filePath);
            }
        }
    }

    public void sendPathsArray(ArrayList<String> pathsArray) {
        if(!pathsArray.isEmpty()) {
            for (String path : pathsArray) {
                try {
                    dataOutputStream.writeUTF(path);
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
                    pathsArray.add(i, dataInputStream.readUTF());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return pathsArray;
    }
    public void deleteFiles(ArrayList<String> filesPathsToDelete) {
        File fileToDelete;
        for(String filePath : filesPathsToDelete) {
            fileToDelete = new File(filePath);
            if(!fileToDelete.delete()) {
                System.out.println("Can't delete file " + filePath);
            }
        }
    }

    public ArrayList<String> getNotExistingFilesPaths(ArrayList<String> filesPaths,
                                                      ArrayList<Long> filesLastModiefied) {
        ArrayList<String> notExistingFilesPaths = new ArrayList<>();
        for(int i = 0; i < filesPaths.size(); i++) {
            if (filesLastModiefied.get(i) == 0) {
                notExistingFilesPaths.add(filesPaths.get(i));
            }
        }
        return notExistingFilesPaths;
    }

    public ArrayList<String> getFilesPathsToSend(ArrayList<String> filesPaths,
                                                 ArrayList<Long> senderFilesLastModified,
                                                 ArrayList<Long> receiverFilesLastModified) {
        ArrayList<String> filesPathsToSend = new ArrayList<>();
        for(int i = 0; i < filesPaths.size(); i++) {
            if(senderFilesLastModified.get(i) > receiverFilesLastModified.get(i) && senderFilesLastModified.get(i) != 0) {
                filesPathsToSend.add(filesPaths.get(i));
            }
        }
        return filesPathsToSend;
    }

    public ArrayList<String> getFilesPathsToReceive(ArrayList<String> filesPaths,
                                                    ArrayList<Long> senderFilesLastModified,
                                                    ArrayList<Long> receiverFilesLastModified) {
        ArrayList<String> filesPathsToReceive = new ArrayList<>();
        for(int i = 0; i < filesPaths.size(); i++) {
            if(senderFilesLastModified.get(i) < receiverFilesLastModified.get(i) && senderFilesLastModified.get(i) != 0) {
                filesPathsToReceive.add(filesPaths.get(i));
            }
        }
        return filesPathsToReceive;
    }

    public ArrayList<String> getUpToDateFilesPaths(ArrayList<String> filesPaths,
                                                   ArrayList<Long> senderFilesLastModified,
                                                   ArrayList<Long> receiverFilesLastModified) {
        ArrayList<String> upToDateFilesPaths = new ArrayList<>();
        for(int i = 0; i < filesPaths.size(); i++) {
            if(senderFilesLastModified.get(i).equals(receiverFilesLastModified.get(i))) {
                upToDateFilesPaths.add(filesPaths.get(i));
            }
        }
        return upToDateFilesPaths;
    }
}
