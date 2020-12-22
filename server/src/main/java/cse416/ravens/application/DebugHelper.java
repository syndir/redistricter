package cse416.ravens.application;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class DebugHelper {
    private static final ReentrantLock lock = new ReentrantLock();
    private static Integer debugLevel = 0;
    private static String debugFile = "debug.log";

    public static void log(String debugMessage) {
        DebugHelper.log(debugMessage, false);
    }

    public static synchronized void log(String debugMessage, Boolean force) {
        if(debugLevel > 0 || force) {
            BufferedWriter bw = null;

            System.out.println("\u001b[31m*** \u001b[33m" + debugMessage + "\u001b[0m");
            try {
                if(debugFile != null) {
                    lock.lock();
                    bw = new BufferedWriter(new FileWriter(debugFile, true));
                    bw.write(new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()) + ": " + debugMessage);
                    bw.newLine();
                    bw.close();
                }
            }
            catch (Exception e) {
                System.out.println("\u001b[31m*** \u001b[33m" + e.getMessage() + "\u001b[0m");
            }
            finally {
                lock.unlock();
            }
        }
    }

    public static Integer getDebugLevel() {
        return DebugHelper.debugLevel;
    }

    public static void setDebugLevel(Integer debugLevel) {
        DebugHelper.debugLevel = debugLevel;
    }

    public static void setDebugFile(String debugFile) {
        DebugHelper.debugFile = debugFile;
    }
}