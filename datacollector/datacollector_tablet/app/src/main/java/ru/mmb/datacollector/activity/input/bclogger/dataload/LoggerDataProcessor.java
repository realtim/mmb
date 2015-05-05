package ru.mmb.datacollector.activity.input.bclogger.dataload;

public interface LoggerDataProcessor {
    boolean isTerminated();

    void writeError(String message);

    void writeToConsole(String message);

    boolean needRepeatLineRequest();

    LogStringParsingResult repeatLineRequest(String lineNumber);
}
