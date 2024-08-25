package net.cakemine.playerservers.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

public class ServerLogReader {
    private BufferedReader logReader;
    private File logFile;
    private FileTime lastModifiedTime;

    public ServerLogReader(File logFile) {
        this.logFile = logFile;
        openLogFile();
    }

    private void openLogFile() {
        try {
            if (logReader != null) {
                logReader.close(); // Close the previous reader if it's open
            }
            logReader = new BufferedReader(new FileReader(logFile));
            lastModifiedTime = Files.getLastModifiedTime(logFile.toPath());
        } catch (IOException e) {
            logReader = null;
            e.printStackTrace();
        }
    }

    public BufferedReader getLogReader() {
        if (logReader == null || logFileModified()) {
            openLogFile(); // Reopen the log file if it's been modified or if the reader is null
        }
        return logReader; // This will only return null if the file doesn't exist or there was an error
    }

    private boolean logFileModified() {
        try {
            FileTime currentModifiedTime = Files.getLastModifiedTime(logFile.toPath());
            return !currentModifiedTime.equals(lastModifiedTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
