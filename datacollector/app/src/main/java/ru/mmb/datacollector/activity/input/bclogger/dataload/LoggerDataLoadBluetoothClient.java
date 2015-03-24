package ru.mmb.datacollector.activity.input.bclogger.dataload;

import android.content.Context;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.mmb.datacollector.activity.input.bclogger.BluetoothClient;
import ru.mmb.datacollector.db.DatacollectorDB;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.bclogger.LoggerInfo;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class LoggerDataLoadBluetoothClient extends BluetoothClient {
    private static final Pattern REGEXP_LOG_DATA = Pattern.compile("(\\d{2}), (\\d{2}), (\\d{8}), (\\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), Line#=(\\d+), CRC8=(\\d+)");
    private static final Pattern REGEXP_TO_CHECK_CRC = Pattern.compile("(\\d{2}, \\d{2}, \\d{8}, \\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2}), Line#=\\d+, CRC8=\\d+");

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy/MM/dd");

    private final ScanPoint currentScanPoint;
    private String confLoggerId;
    private PrintWriter errorLog;

    public LoggerDataLoadBluetoothClient(Context context, LoggerInfo loggerInfo, Handler handler, ScanPoint currentScanPoint) {
        super(context, loggerInfo, handler);
        this.currentScanPoint = currentScanPoint;
    }

    public void clearDevice() {
        boolean connected = connect();
        if (connected) {
            sendRequestWaitForReply("DELLOG\n");
            disconnectImmediately();
        }
        sendFinishedNotification();
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
        sendFinishedNotification();
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
        sendFinishedNotification();
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
        String fileName =
                Settings.getInstance().getExportDir() + "/" + "bclogger_" + logFileName + "_" +
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

    private void parseAndSaveLogData(String loggerReply) {
        errorLog = createErrorLog();
        if (errorLog == null) return;
        try {
            String[] replyStrings = loggerReply.split("\\n");
            boolean inDataLines = false;
            int linesProcessed = 0;
            for (String replyString : replyStrings) {
                writeToConsole("data line: " + replyString);
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

                writeToConsole("processing data line");

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
                        saveToDB(parsingResult);
                    }
                }
            }
            writeToConsole("log import finished");
        } finally {
            errorLog.flush();
            errorLog.close();
        }
    }

    private void writeError(String message) {
        errorLog.write(message + "\n");
        writeToConsole(message);
    }

    private PrintWriter createErrorLog() {
        SimpleDateFormat currTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        String fileName = Settings.getInstance().getExportDir() + "/" + "bclogger_load_errors_" +
                          currTimeFormat.format(new Date()) + ".txt";
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

    private void saveToDB(LogStringParsingResult parsingResult) {
        try {
            int loggerId = Integer.parseInt(parsingResult.getLoggerId());
            int scanpointId = currentScanPoint.getScanPointId();
            int teamId = getTeamIdByNumber(parsingResult);
            // Remove seconds from date, or existing records will be updated when no need.
            // Dates in DB are saved without seconds, and before() or after() will return
            // undesired results, if seconds are present in parsed result.
            Date recordDateTime = trimToMinutes(sdf.parse(parsingResult.getRecordDateTime()));
            RawLoggerData existingRecord = DatacollectorDB.getConnectedInstance().getExistingLoggerRecord(loggerId, scanpointId, teamId);
            if (existingRecord != null) {
                if (needUpdateExistingRecord(existingRecord, recordDateTime)) {
                    DatacollectorDB.getConnectedInstance().updateExistingLoggerRecord(loggerId, scanpointId, teamId, recordDateTime);
                    writeToConsole("existing record updated");
                } else {
                    writeToConsole("existing record NOT updated");
                }
            } else {
                DatacollectorDB.getConnectedInstance().insertNewLoggerRecord(loggerId, scanpointId, teamId, recordDateTime);
                writeToConsole("new record inserted");
            }
        } catch (Exception e) {
            writeError("ERROR before saveToDB: " + e.getMessage());
        }
    }

    private int getTeamIdByNumber(LogStringParsingResult parsingResult) throws Exception {
        int teamNumber = parsingResult.extractTeamNumber();
        Team team = TeamsRegistry.getInstance().getTeamByNumber(teamNumber);
        if (team == null) throw new Exception("team not found by number: " + teamNumber);
        return team.getTeamId();
    }

    /*
     * Code copied from stackoverflow.
     */
    private Date trimToMinutes(Date value) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTime(value);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private boolean needUpdateExistingRecord(RawLoggerData existingRecord, Date recordDateTime) {
        int distanceId = existingRecord.getTeam().getDistanceId();
        if (currentScanPoint.getLevelPointByDistance(distanceId).getPointType().isStart()) {
            // start record - use last check
            return existingRecord.getRecordDateTime().before(recordDateTime);
        } else {
            // finish record - use first check
            return existingRecord.getRecordDateTime().after(recordDateTime);
        }
    }
}
