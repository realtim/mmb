package ru.mmb.datacollector.activity.input.bclogger.settings;

import android.content.Context;
import android.os.Handler;

import ru.mmb.datacollector.activity.input.bclogger.InputBCLoggerBluetoothClient;
import ru.mmb.datacollector.bluetooth.LoggerInfo;

public class LoggerSettingsBluetoothClient extends InputBCLoggerBluetoothClient {
    private final LoggerSettingsActivityState currentState;

    public LoggerSettingsBluetoothClient(Context context, LoggerInfo loggerInfo, Handler handler, LoggerSettingsActivityState currentState) {
        super(context, loggerInfo, handler);
        this.currentState = currentState;
    }

    public void reloadSettings() {
        currentState.setCommunicationSuccess(false);
        boolean connected = connect();
        if (connected) {
            String loggerReply = requestSettings();
            if (loggerReply != null) {
                parseReplyToCurrentState(loggerReply);
                currentState.setCommunicationSuccess(true);
            }
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }

    private void parseReplyToCurrentState(String loggerReply) {
        String[] replyStrings = loggerReply.split("\\n");
        String value = getValueFromReplyByRegexp(replyStrings, REGEXP_SCANNER_ID);
        if (value != null) currentState.setLoggerId(value);
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_SCANPOINT_ORDER);
        if (value != null) currentState.setScanpointOrder(value);
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_LENGTH_CHECKING);
        if (value != null) currentState.setLengthCheck("Y".equals(value));
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_NUMBERS_ONLY);
        if (value != null) currentState.setDigitsOnly("Y".equals(value));
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_PATTERN);
        if (value != null) currentState.setPattern(value);
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_DATE_TIME);
        if (value != null) currentState.setLoggerTime(value);
    }

    private String requestSettings() {
        return sendRequestWaitForReply("GETS\n");
    }

    public void sendSettings() {
        currentState.setCommunicationSuccess(false);
        boolean connected = connect();
        if (connected) {
            sendRequestWaitForReply("SETC" + currentState.getScanpointOrder() + "\n");
            String lengthCheck = currentState.isLengthCheck() ? "Y" : "N";
            sendRequestWaitForReply("SETL" + lengthCheck + "\n");
            String numbersOnly = currentState.isDigitsOnly() ? "Y" : "N";
            sendRequestWaitForReply("SETN" + numbersOnly + "\n");
            sendRequestWaitForReply("SETP" + currentState.getPattern() + "\n");
            updateLoggerTime();
            currentState.setCommunicationSuccess(true);
            disconnectImmediately();
        }
        sendFinishedSuccessNotification();
    }
}
