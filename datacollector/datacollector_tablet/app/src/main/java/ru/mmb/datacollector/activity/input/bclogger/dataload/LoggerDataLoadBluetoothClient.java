package ru.mmb.datacollector.activity.input.bclogger.dataload;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.mmb.datacollector.activity.input.bclogger.InputBCLoggerBluetoothClient;
import ru.mmb.datacollector.bluetooth.DeviceInfo;
import ru.mmb.datacollector.model.registry.Settings;

public class LoggerDataLoadBluetoothClient extends InputBCLoggerBluetoothClient implements LoggerDataProcessor {
    private String confLoggerId;
    private PrintWriter errorLog;

    private LoggerReplyParser parser;

    public LoggerDataLoadBluetoothClient(Context context, DeviceInfo deviceInfo, Handler handler) {
        super(context, deviceInfo, handler);
    }

    /*
    public void clearDevice() {
        boolean connected = connect();
        if (connected) {
            sendRequestWaitForReply("DELLOG\n");
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }
    */

    public void loadErrorsData() {
        boolean connected = connect();
        if (connected) {
            String loggerReply = sendRequestWaitForReplySilent("GETD\n");
            if (loggerReply != null) {
                saveLoggerReplyToLogFile(loggerReply, "errors");
            }
            updateLoggerTime();
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }

    public void loadLogData() {
        boolean connected = connect();
        if (connected) {
            confLoggerId = requestLoggerId();
            writeToConsole("loggerId loaded: " + confLoggerId);
            if (confLoggerId != null) {
                String loggerReply = sendRequestWaitForReplySilent("GETL\n");
                if (loggerReply != null) {
                    saveLoggerReplyToLogFile(loggerReply, "datalog");
                    errorLog = createErrorLog();
                    if (errorLog != null) {
                        try {
                            parser = new LoggerReplyParser(this, confLoggerId);
                            parser.parseAndSaveLogData(loggerReply);
                        } finally {
                            errorLog.flush();
                            errorLog.close();
                            parser = null;
                        }
                    }
                }
            }
            updateLoggerTime();
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }

    private String requestLoggerId() {
        String loggerReply = sendRequestWaitForReply("GETS\n");
        String[] replyStrings = loggerReply.split("\\n");
        String value = getValueFromReplyByRegexp(replyStrings, REGEXP_SCANNER_ID);
        if (value != null) return value;
        return null;
    }

    private void saveLoggerReplyToLogFile(String loggerReply, String logFileName) {
        SimpleDateFormat currTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        createDatalogDirIfNotExists();
        String fileName =
                Settings.getInstance().getDatalogDir() + "/bclogger_" + logFileName + "_" +
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

    private void createDatalogDirIfNotExists() {
        File datalogDir = new File(Settings.getInstance().getDatalogDir());
        if (!datalogDir.exists()) {
            datalogDir.mkdir();
        }
    }

    public void writeError(String message) {
        errorLog.write(message + "\n");
        writeToConsole(message);
    }

    private PrintWriter createErrorLog() {
        SimpleDateFormat currTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        createDatalogDirIfNotExists();
        String fileName = Settings.getInstance().getDatalogDir() + "/" +
                "bclogger_load_errors_" + currTimeFormat.format(new Date()) + ".txt";
        File outputFile = new File(fileName);
        try {
            if (!outputFile.exists()) outputFile.createNewFile();
            return new PrintWriter(new FileOutputStream(fileName, false));
        } catch (IOException e) {
            writeToConsole("errors log creation FAILED");
            writeToConsole("error: " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean needRepeatLineRequest() {
        return true;
    }

    @Override
    public LogStringParsingResult repeatLineRequest(String lineNumber) {
        for (int i = 0; i < 3; i++) {
            String loggerReply = sendRequestWaitForReply("GET#L" + lineNumber + "\n");
            String[] replyStrings = loggerReply.split("\\n");
            String replyString = getWholeStringFromReplyByRegexp(replyStrings, LoggerReplyParser.REGEXP_LOG_DATA);
            if (replyString != null) {
                LogStringParsingResult parsingResult = parser.parseReplyString(replyString);
                if (parsingResult.isCrcFailed()) {
                    writeError(parsingResult.getErrorMessage());
                } else {
                    return parsingResult;
                }
            }
        }
        return null;
    }
}
