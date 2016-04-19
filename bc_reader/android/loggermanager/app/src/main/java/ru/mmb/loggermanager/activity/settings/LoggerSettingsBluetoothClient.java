package ru.mmb.loggermanager.activity.settings;

import android.content.Context;
import android.os.Handler;

import ru.mmb.loggermanager.bluetooth.DeviceInfo;
import ru.mmb.loggermanager.bluetooth.LoggerBluetoothClient;

public class LoggerSettingsBluetoothClient extends LoggerBluetoothClient {
    private LoggerSettings loggerSettings;

    public LoggerSettingsBluetoothClient(Context context, DeviceInfo deviceInfo, Handler handler, LoggerSettings loggerSettings) {
        super(context, deviceInfo, handler);
        this.loggerSettings = loggerSettings;
    }

    public void reloadSettings() {
        boolean connected = connect();
        if (connected) {
            String loggerReply = requestSettings();
            if (loggerReply != null) {
                parseReplyToCurrentState(loggerReply);
            }
            disconnectImmediately();
            sendFinishedSuccessNotification();
        } else {
            sendFinishedErrorNotification();
        }
    }

    private void parseReplyToCurrentState(String loggerReply) {
        String[] replyStrings = loggerReply.split("\\n");
        String value = getValueFromReplyByRegexp(replyStrings, REGEXP_SCANNER_ID);
        if (value != null) loggerSettings.setLoggerId(value);
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_SCANPOINT_ORDER);
        if (value != null) loggerSettings.setScanpointId(value);
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_LENGTH_CHECKING);
        if (value != null) loggerSettings.setCheckLength("Y".equals(value));
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_NUMBERS_ONLY);
        if (value != null) loggerSettings.setOnlyDigits("Y".equals(value));
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_PATTERN);
        if (value != null) loggerSettings.setPattern(value);
        value = getValueFromReplyByRegexp(replyStrings, REGEXP_DATE_TIME);
        if (value != null) loggerSettings.setLoggerTime(value);
    }

    private String requestSettings() {
        return sendRequestWaitForReply("GETS\n");
    }

    public void sendCommand(String command) {
        boolean connected = connect();
        if (connected) {
            sendRequestWaitForReply(command);
            disconnectImmediately();
            sendFinishedSuccessNotification();
        } else {
            sendFinishedErrorNotification();
        }
    }

    public void updateLoggerTime() {
        boolean connected = connect();
        if (connected) {
            super.updateLoggerTime();
            disconnectImmediately();
            sendFinishedSuccessNotification();
        } else {
            sendFinishedErrorNotification();
        }
    }
}
