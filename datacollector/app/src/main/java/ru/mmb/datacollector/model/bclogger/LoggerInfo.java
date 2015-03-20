package ru.mmb.datacollector.model.bclogger;

import java.io.Serializable;

public class LoggerInfo implements Serializable, Comparable<LoggerInfo> {
    private String loggerName;
    private String loggerBTAddress;

    public LoggerInfo() {
    }

    public LoggerInfo(String loggerName, String loggerBTAddress) {
        this.loggerName = loggerName;
        this.loggerBTAddress = loggerBTAddress;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getLoggerBTAddress() {
        return loggerBTAddress;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public void setLoggerBTAddress(String loggerBTAddress) {
        this.loggerBTAddress = loggerBTAddress;
    }

    public String saveToString() {
        return loggerName + "|" + loggerBTAddress;
    }

    public void loadFromString(String source) {
        int separatorPos = source.indexOf("|");
        loggerName = source.substring(0, separatorPos);
        loggerBTAddress = source.substring(separatorPos + 1, source.length());
    }

    @Override
    public int compareTo(LoggerInfo another) {
        return loggerName.compareTo(another.loggerName);
    }

    @Override
    public String toString() {
        return "LoggerInfo{" +
               "loggerName='" + loggerName + '\'' +
               ", loggerBTAddress='" + loggerBTAddress + '\'' +
               '}';
    }
}
