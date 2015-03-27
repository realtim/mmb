package ru.mmb.datacollector.activity.input.bclogger.settings;

import ru.mmb.datacollector.activity.ActivityStateWithScanPointAndBTDevice;

public class LoggerSettingsActivityState extends ActivityStateWithScanPointAndBTDevice {
    private String loggerId;
    private String scanpointOrder;
    private String pattern;
    private String loggerTime = null;
    private boolean lengthCheck;
    private boolean digitsOnly;

    private boolean communicationSuccess = false;

    public LoggerSettingsActivityState(String prefix) {
        super(prefix);
    }

    public String getLoggerId() {
        return loggerId;
    }

    public void setLoggerId(String loggerId) {
        this.loggerId = loggerId;
    }

    public String getScanpointOrder() {
        return scanpointOrder;
    }

    public void setScanpointOrder(String scanpointOrder) {
        this.scanpointOrder = scanpointOrder;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getLoggerTime() {
        return loggerTime;
    }

    public void setLoggerTime(String loggerTime) {
        this.loggerTime = loggerTime;
    }

    public boolean isLengthCheck() {
        return lengthCheck;
    }

    public void setLengthCheck(boolean lengthCheck) {
        this.lengthCheck = lengthCheck;
    }

    public boolean isDigitsOnly() {
        return digitsOnly;
    }

    public void setDigitsOnly(boolean digitsOnly) {
        this.digitsOnly = digitsOnly;
    }

    public boolean isCommunicationSuccess() {
        return communicationSuccess;
    }

    public void setCommunicationSuccess(boolean communicationSuccess) {
        this.communicationSuccess = communicationSuccess;
    }
}
