package ru.mmb.datacollector.activity.input.bclogger.settings;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.mmb.datacollector.activity.input.bclogger.BluetoothClient;
import ru.mmb.datacollector.activity.input.bclogger.ThreadMessageTypes;
import ru.mmb.datacollector.model.bclogger.LoggerInfo;

public class LoggerSettingsBluetoothClient extends BluetoothClient {
    private static final Pattern REGEXP_SCANNER_ID = Pattern.compile("-Scanner ID: (\\d{2})");
    private static final Pattern REGEXP_SCANPOINT_ORDER = Pattern.compile("-Control Point: (\\d{2})");
    private static final Pattern REGEXP_LENGTH_CHECKING = Pattern.compile("-Barcode string length checking: ([YN])");
    private static final Pattern REGEXP_NUMBERS_ONLY = Pattern.compile("-Numbers only: ([YN])");
    private static final Pattern REGEXP_PATTERN = Pattern.compile("-Barcode pattern: (\\S+)");
    private static final Pattern REGEXP_DATE_TIME = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}, \\d{4}/\\d{2}/\\d{2})");

    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy/MM/dd");
    private static final SimpleDateFormat loggerDateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH);
    private static final SimpleDateFormat loggerTimeFormat = new SimpleDateFormat("HH:mm:ss");

    private final LoggerSettingsActivityState currentState;
    private final Handler handler;

    public LoggerSettingsBluetoothClient(Context context, LoggerInfo loggerInfo, LoggerSettingsActivityState currentState, Handler handler) {
        super(context, loggerInfo);
        this.currentState = currentState;
        this.handler = handler;
    }

    @Override
    public void writeToConsole(String message) {
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_CONSOLE, message));
        }
    }

    public void reloadSettings() {
        currentState.setCommunicationSuccess(false);
        boolean connected = connect();
        if (connected) {
            String loggerReply = requestSettings();
            String tabletTimeText = sdf.format(new Date());
            if (loggerReply != null) {
                parseReplyToCurrentState(loggerReply);
                currentState.setCommunicationSuccess(true);
            }
            disconnectImmediately();
        }
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED));
        }
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

    private String getValueFromReplyByRegexp(String[] replyStrings, Pattern pattern) {
        for (String replyString : replyStrings) {
            Matcher matcher = pattern.matcher(replyString);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
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
            resetLoggerTime();
            currentState.setCommunicationSuccess(true);
            disconnectImmediately();
        }
        if (!isTerminated()) {
            handler.sendMessage(Message.obtain(handler, ThreadMessageTypes.MSG_FINISHED));
        }
    }

    private void resetLoggerTime() {
        sendRequestWaitForReply("SETT\n");
        Date currentTime = new Date();
        String timeString =
                loggerDateFormat.format(currentTime) + " " + loggerTimeFormat.format(currentTime);
        sendRequestWaitForReply(timeString + "\n", 5000);
    }
}
