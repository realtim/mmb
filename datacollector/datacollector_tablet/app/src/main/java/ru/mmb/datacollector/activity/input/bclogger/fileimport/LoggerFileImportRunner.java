package ru.mmb.datacollector.activity.input.bclogger.fileimport;

import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.mmb.datacollector.activity.input.bclogger.dataload.LogStringParsingResult;
import ru.mmb.datacollector.activity.input.bclogger.dataload.LoggerDataProcessor;
import ru.mmb.datacollector.activity.input.bclogger.dataload.LoggerReplyParser;
import ru.mmb.datacollector.bluetooth.ThreadMessageTypes;
import ru.mmb.datacollector.model.ScanPoint;

public class LoggerFileImportRunner implements LoggerDataProcessor {
    private final ScanPoint currentScanPoint;
    private final String fileName;
    private final Handler handler;

    private boolean terminated = false;

    private LoggerReplyParser parser;

    public LoggerFileImportRunner(ScanPoint currentScanPoint, String fileName, Handler handler) {
        this.currentScanPoint = currentScanPoint;
        this.fileName = fileName;
        this.handler = handler;
    }

    @Override
    public synchronized boolean isTerminated() {
        return terminated;
    }

    public synchronized void terminate() {
        this.terminated = true;
    }

    @Override
    public void writeError(String message) {
        writeToConsole(message);
    }

    @Override
    public synchronized void writeToConsole(String message) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }

    @Override
    public boolean needRepeatLineRequest() {
        return false;
    }

    @Override
    public LogStringParsingResult repeatLineRequest(String lineNumber) {
        // no repeating request needed on file import
        return null;
    }

    private void sendFinishedSuccessNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_SUCCESS));
        }
    }

    private void sendFinishedErrorNotification() {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED_ERROR));
        }
    }

    public void importFile() {
        try {
            String fileContents = readFileToString();
            parser = new LoggerReplyParser(this, currentScanPoint, null);
            parser.parseAndSaveLogData(fileContents);
            sendFinishedSuccessNotification();
        } catch (Exception e) {
            writeToConsole("ERROR parsing data: " + e.getMessage());
            sendFinishedErrorNotification();
        }
    }

    /*
     * Code from Guava.
     */
    private String readFileToString() throws IOException {
        File file = new File(fileName);
        InputStream in = new FileInputStream(file);
        byte[] b = new byte[(int) file.length()];
        int len = b.length;
        int total = 0;
        while (total < len) {
            int result = in.read(b, total, len - total);
            if (result == -1) {
                break;
            }
            total += result;
        }
        return new String(b, "US-ASCII");
    }
}
