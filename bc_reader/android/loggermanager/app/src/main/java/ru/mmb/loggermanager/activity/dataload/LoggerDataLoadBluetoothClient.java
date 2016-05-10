package ru.mmb.loggermanager.activity.dataload;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.LoggerBluetoothClient;
import ru.mmb.loggermanager.conf.Configuration;

public class LoggerDataLoadBluetoothClient extends LoggerBluetoothClient {

    public LoggerDataLoadBluetoothClient(Context context, DeviceInfo deviceInfo, Handler handler) {
        super(context, deviceInfo, handler);
    }

    public void sendCommand(String command) {
        boolean connected = connect();
        if (connected) {
            receiveData(200, false);
            sendRequestWaitForReply(command);
            disconnectImmediately();
            sendFinishedSuccessNotification();
        } else {
            sendFinishedErrorNotification();
        }
    }

    public void loadErrorsData() {
        boolean connected = connect();
        if (connected) {
            receiveData(200, false);
            writeToConsole("sending: GETD");
            String loggerReply = sendRequestWaitForReplySilent("GETD\n");
            if (loggerReply != null) {
                saveLoggerReplyToLogFile(loggerReply, "errors");
            }
            disconnectImmediately();
            sendFinishedSuccessNotification();
        } else {
            sendFinishedErrorNotification();
        }
    }

    public void loadLogData() {
        boolean connected = connect();
        if (connected) {
            receiveData(200, false);
            writeToConsole("sending: GETL");
            String loggerReply = sendRequestWaitForReplySilent("GETL\n");
            if (loggerReply != null) {
                saveLoggerReplyToLogFile(loggerReply, "datalog");
                LoggerReplyParser parser = new LoggerReplyParser(this);
                parser.parseLogData(loggerReply);
            }
            disconnectImmediately();
            sendFinishedSuccessNotification();
        } else {
            sendFinishedErrorNotification();
        }
    }

    private void saveLoggerReplyToLogFile(String loggerReply, String logFileName) {
        SimpleDateFormat currTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fileName =
                Configuration.getInstance().getSaveDir() + "/" + logFileName + "_" +
                        getDeviceInfo().getDeviceName() + "_" +
                        currTimeFormat.format(new Date()) + ".txt";
        try {
            PureDataExtractor extractor = new PureDataExtractor();
            extractor.processLogData(loggerReply);
            String pureData = extractor.getProcessingResult().toString();
            File outputFile = new File(fileName);
            if (!outputFile.exists()) outputFile.createNewFile();
            PrintWriter writer = new PrintWriter(new FileOutputStream(fileName, false));
            writer.write(pureData);
            writer.flush();
            writer.close();
            writeToConsole(logFileName + " save to file SUCCESS");
        } catch (IOException e) {
            writeToConsole(logFileName + " save to file FAILED");
            writeToConsole("error: " + e.getMessage());
        }
    }

    private class PureDataExtractor extends LoggerReplyProcessor {
        private StringBuilder processingResult = new StringBuilder();

        public StringBuilder getProcessingResult() {
            return processingResult;
        }

        @Override
        protected void processLogLine(String logLine) {
            processingResult.append(logLine).append("\n");
        }
    }
}
