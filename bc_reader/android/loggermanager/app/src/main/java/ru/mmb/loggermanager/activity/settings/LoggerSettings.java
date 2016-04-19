package ru.mmb.loggermanager.activity.settings;

public class LoggerSettings {

    private String loggerId;
    private String scanpointId;
    private String pattern;
    private String loggerTime;
    private boolean checkLength;
    private boolean onlyDigits;

    public String getLoggerId() {
        return loggerId;
    }

    public String getScanpointId() {
        return scanpointId;
    }

    public String getPattern() {
        return pattern;
    }

    public String getLoggerTime() {
        return loggerTime;
    }

    public boolean isCheckLength() {
        return checkLength;
    }

    public boolean isOnlyDigits() {
        return onlyDigits;
    }

    public void setLoggerId(String loggerId) {
        this.loggerId = loggerId;
    }

    public void setScanpointId(String scanpointId) {
        this.scanpointId = scanpointId;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setLoggerTime(String loggerTime) {
        this.loggerTime = loggerTime;
    }

    public void setCheckLength(boolean checkLength) {
        this.checkLength = checkLength;
    }

    public void setOnlyDigits(boolean onlyDigits) {
        this.onlyDigits = onlyDigits;
    }
}
