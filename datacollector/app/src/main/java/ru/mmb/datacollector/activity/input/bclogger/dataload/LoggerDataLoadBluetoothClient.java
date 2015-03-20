package ru.mmb.datacollector.activity.input.bclogger.dataload;

import android.content.Context;
import android.os.Handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.mmb.datacollector.activity.input.bclogger.BluetoothClient;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.bclogger.LoggerInfo;

public class LoggerDataLoadBluetoothClient extends BluetoothClient {
    // TODO build regexp to extract each field of data
    private static final Pattern REGEXP_LOG_DATA = Pattern.compile("");

    private final ScanPoint currentScanPoint;
    private String confLoggerId;

    public LoggerDataLoadBluetoothClient(Context context, LoggerInfo loggerInfo, Handler handler, ScanPoint currentScanPoint) {
        super(context, loggerInfo, handler);
        this.currentScanPoint = currentScanPoint;
    }

    public void loadLogData() {
        boolean connected = connect();
        if (connected) {
            confLoggerId = requestLoggerId();
            writeToConsole("loggerId loaded: " + confLoggerId);
            if (confLoggerId != null) {
                String loggerReply = sendRequestWaitForReply("GETL\n");
                if (loggerReply != null) {
                    parseAndSaveLogData(loggerReply, confLoggerId);
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

    private void parseAndSaveLogData(String loggerReply, String loggerId) {
        String[] replyStrings = loggerReply.split("\\n");
        boolean inDataLines = false;
        for (String replyString : replyStrings) {
            if ("====".equals(replyString)) {
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

            Matcher matcher = REGEXP_LOG_DATA.matcher(replyString);
            if (matcher.find()) {
                String logger
            } else {
                saveToLog("ERROR line [" + replyString + "] regexp parsing failed");
                writeToConsole("ERROR line [\"+replyString+\"] regexp parsing failed");
            }

        }
    }
}
