package ru.mmb.loggermanager.activity.dataload;

public class LogStringParsingResult {

    private final String source;

    private boolean regexpMatches = true;
    private boolean crcFailed = false;

    public LogStringParsingResult(String source) {
        this.source = source;
    }

    public boolean isRegexpMatches() {
        return regexpMatches;
    }

    public void setRegexpMatches(boolean regexpMatches) {
        this.regexpMatches = regexpMatches;
    }

    public boolean isCrcFailed() {
        return crcFailed;
    }

    public void setCrcFailed(boolean crcFailed) {
        this.crcFailed = crcFailed;
    }

    public boolean isError() {
        return !isRegexpMatches() || isCrcFailed();
    }

    public String getErrorMessage() {
        if (!isRegexpMatches()) {
            return "ERROR [" + source + "] regexp parsing failed";
        }
        if (isCrcFailed()) {
            return "ERROR [" + source + "] CRC check failed";
        }
        return "UNKNOWN ERROR";
    }
}
