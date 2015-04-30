package ru.mmb.datacollector.activity.input.bclogger.dataload;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.mmb.datacollector.activity.input.bclogger.InputBCLoggerBluetoothClient;
import ru.mmb.datacollector.bluetooth.DeviceInfo;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.registry.Settings;

public class LoggerDataLoadBluetoothClient extends InputBCLoggerBluetoothClient {
    private static final Pattern REGEXP_LOG_DATA = Pattern.compile("(\\d{2}), (\\d{2}), (\\d{8}), (\\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), Line#=(\\d+), CRC8=(\\d+)");
    private static final Pattern REGEXP_TO_CHECK_CRC = Pattern.compile("(\\d{2}, \\d{2}, \\d{8}, \\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), Line#=\\d+, CRC8=\\d+");

    private final ScanPoint currentScanPoint;
    private String confLoggerId;
    private PrintWriter errorLog;

    public LoggerDataLoadBluetoothClient(Context context, DeviceInfo deviceInfo, Handler handler, ScanPoint currentScanPoint) {
        super(context, deviceInfo, handler);
        this.currentScanPoint = currentScanPoint;
    }

    public void clearDevice() {
        boolean connected = connect();
        if (connected) {
            sendRequestWaitForReply("DELLOG\n");
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }

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
                    parseAndSaveLogData(loggerReply);
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
        SimpleDateFormat currTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        createDatalogDirIfNotExists();
        String fileName =
                Settings.getInstance().getMMBPathFromDBFile() + "/datalog/" + "bclogger_" +
                logFileName + "_" + currTimeFormat.format(new Date()) + ".txt";
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
        File datalogDir = new File(Settings.getInstance().getMMBPathFromDBFile() + "/datalog");
        if (!datalogDir.exists()) {
            datalogDir.mkdir();
        }
    }

    private void parseAndSaveLogData(String loggerReply) {
        errorLog = createErrorLog();
        if (errorLog == null) return;
        LoggerDataSaver dataSaver = new LoggerDataSaver(this);
        dataSaver.init();
        try {
            String[] replyStrings = loggerReply.split("\\n");
            boolean inDataLines = false;
            int linesProcessed = 0;
            for (String replyString : replyStrings) {
                // stop parsing if thread is terminated
                if (isTerminated()) {
                    break;
                }
                // Log.d("DATA_LOAD_BT", "data line: " + replyString);
                if ("====".equals(replyString.trim())) {
                    if (!inDataLines) {
                        // got start of data lines marker
                        inDataLines = true;
                        continue;
                    } else {
                        // got end of data lines marker
                        break;
                    }
                }
                if (!inDataLines) continue;

                linesProcessed++;
                if (linesProcessed % 50 == 0) {
                    writeToConsole("lines processed: " + linesProcessed);
                }

                LogStringParsingResult parsingResult = parseReplyString(replyString);
                if (parsingResult.isRegexpMatches() && parsingResult.isCrcFailed()) {
                    parsingResult = repeatLineRequest(parsingResult.getLineNumber());
                    if (parsingResult == null) {
                        writeError("ERROR repeating on bad CRC was not successful");
                        continue;
                    }
                }

                parsingResult.checkConsistencyErrors(confLoggerId);

                if (parsingResult.isError()) {
                    writeError(parsingResult.getErrorMessage());
                } else {
                    int scanpointOrder = Integer.parseInt(parsingResult.getScanpointOrder());
                    if (scanpointOrder == currentScanPoint.getScanPointOrder()) {
                        writeToConsole(replyString);
                        dataSaver.saveToDB(currentScanPoint, parsingResult);
                    }
                }
            }
            dataSaver.flushData();
            writeToConsole("log import finished");
        } finally {
            errorLog.flush();
            errorLog.close();
            dataSaver.releaseResources();
        }
    }

    public void writeError(String message) {
        errorLog.write(message + "\n");
        writeToConsole(message);
    }

    private PrintWriter createErrorLog() {
        SimpleDateFormat currTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        createDatalogDirIfNotExists();
        String fileName = Settings.getInstance().getMMBPathFromDBFile() + "/datalog/" +
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

    private LogStringParsingResult parseReplyString(String replyString) {
        LogStringParsingResult result = new LogStringParsingResult(replyString);
        Matcher matcher = REGEXP_LOG_DATA.matcher(replyString);
        if (matcher.find()) {
            result.setRegexpMatches(true);
            result.setLoggerId(matcher.group(1));
            result.setScanpointOrder(matcher.group(2));
            result.setTeamInfo(matcher.group(3));
            result.setRecordDateTime(matcher.group(4));
            result.setLineNumber(matcher.group(5));
            int crc = Integer.parseInt(matcher.group(6));
            if (!checkCRC(replyString, crc)) {
                result.setCrcFailed(true);
            }
        } else {
            result.setRegexpMatches(false);
        }
        return result;
    }

    private boolean checkCRC(String replyString, int crcToCompare) {
        Matcher matcher = REGEXP_TO_CHECK_CRC.matcher(replyString);
        matcher.find();
        int crcCalculated = crc8(matcher.group(1));
        return crcToCompare == crcCalculated;
    }

    /*
     * Tom Meyers
     * Tuesday January 14th, 2014 at 10:55 pm	 (UTC 1) Link to this comment
     * I searched all over for a Java version to interface with my Arduino messages, I developed this (below) and it seems to work. Maybe some else can use it.
     */
    private int crc8(String stringData) {
        byte crc = 0x00;
        for (int i = 0; i < stringData.length(); i++) {
            byte extract = (byte) stringData.charAt(i);
            for (byte tempI = 8; tempI > 0; tempI--) {
                byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
                sum = (byte) ((sum & 0xFF) &
                              0x01); // I had Problems writing this as one line with previous
                crc = (byte) ((crc & 0xFF) >>> 1);
                if (sum != 0) {
                    crc = (byte) ((crc & 0xFF) ^ 0x8C);
                }
                extract = (byte) ((extract & 0xFF) >>> 1);
            }
        }
        return (int) (crc & 0xFF);
    }

    private LogStringParsingResult repeatLineRequest(String lineNumber) {
        for (int i = 0; i < 3; i++) {
            String loggerReply = sendRequestWaitForReply("GET#L" + lineNumber + "\n");
            String[] replyStrings = loggerReply.split("\\n");
            String replyString = getWholeStringFromReplyByRegexp(replyStrings, REGEXP_LOG_DATA);
            if (replyString != null) {
                LogStringParsingResult parsingResult = parseReplyString(replyString);
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
