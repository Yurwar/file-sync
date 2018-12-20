package com.filesync;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

class FileOperation {
    void checkToUpdateFromUser(ArrayList<String> clientNotExistingFiles,
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

    void createMissingFolders(ArrayList<String> filesToReceive) {
        if(filesToReceive.size() > 0) {
            for (String fileToReceive : filesToReceive) {
                File folder;
                if(fileToReceive.contains("/")) {
                    String folderPath = fileToReceive.substring(0, fileToReceive.lastIndexOf("/"));
                    folder = new File(folderPath);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                } else if (fileToReceive.contains("\\")) {
                    String folderPath = fileToReceive.substring(0, fileToReceive.lastIndexOf("\\"));
                    folder = new File(folderPath);
                    if (!folder.exists()) {
                        folder.mkdirs();
                    }
                }
            }
        }
    }

    private void _getAllFilesPaths(File mainFolder, ArrayList<String> filesPaths) {
        File[] folderEntries = mainFolder.listFiles();
        if(folderEntries != null) {
            for (File entry : folderEntries) {
                if (entry.isDirectory()) {
                    _getAllFilesPaths(entry, filesPaths);
                } else {
                    filesPaths.add(entry.getPath());
                }
            }
        }
    }

    ArrayList<String> getAllFilesPaths(File mainFolder) {
        ArrayList<String> filesPaths = new ArrayList<>();
        _getAllFilesPaths(mainFolder, filesPaths);
        return filesPaths;
    }

    void addMissingClientFiles(File folderPath,
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

    void deleteFiles(ArrayList<String> filesPathsToDelete) {
        File fileToDelete;
        for(String filePath : filesPathsToDelete) {
            fileToDelete = new File(filePath);
            if(!fileToDelete.delete()) {
                System.out.println("Can't delete file " + filePath);
            } else {
                System.out.println("File " + filePath + " deleted successfully");
            }
        }
    }

    ArrayList<String> getNotExistingFilesPaths(ArrayList<String> filesPaths,
                                                      ArrayList<Long> filesLastModiefied) {
        ArrayList<String> notExistingFilesPaths = new ArrayList<>();
        for(int i = 0; i < filesPaths.size(); i++) {
            if (filesLastModiefied.get(i) == 0) {
                notExistingFilesPaths.add(filesPaths.get(i));
            }
        }
        return notExistingFilesPaths;
    }

    ArrayList<String> getFilesPathsToSend(ArrayList<String> filesPaths,
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

    ArrayList<String> getFilesPathsToReceive(ArrayList<String> filesPaths,
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

    ArrayList<String> getUpToDateFilesPaths(ArrayList<String> filesPaths,
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

    void printArray(ArrayList arrayList, String msg) {
        if(arrayList.size() > 0) {
            System.out.println(msg);
            for (Object o : arrayList) {
                System.out.println(o);
            }
        }
    }
}
