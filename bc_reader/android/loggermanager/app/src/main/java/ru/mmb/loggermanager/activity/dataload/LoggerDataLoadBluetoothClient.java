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

    public void clearDevice() {
        boolean connected = connect();
        if (connected) {
            writeToConsole("sending: DELLOG");
            sendRequestWaitForReplySilent("DELLOG\n");
            disconnectImmediately();
            sendFinishedSuccessNotification();
        } else {
            sendFinishedErrorNotification();
        }
    }

    public void loadErrorsData() {
        boolean connected = connect();
        if (connected) {
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
            writeToConsole("sending: GETL");
            String loggerReply = sendRequestWaitForReply("GETL\n");
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
                Configuration.getInstance().getSaveDir() + "/bclogger_" + logFileName + "_" +
                        currTimeFormat.format(new Date()) + ".txt";
        try {
            File outputFile = new File(fileName);
            if (!outputFile.exists()) outputFile.createNewFile();
            PrintWriter writer = new PrintWriter(new FileOutputStream(fileName, false));
            writer.write(loggerReply);
            writer.flush();
            writer.close();
            writeToConsole(logFileName + " save to file SUCCESS");
        } catch (IOException e) {
            writeToConsole(logFileName + " save to file FAILED");
            writeToConsole("error: " + e.getMessage());
        }
    }
}
