package ru.mmb.datacollector.activity.input.bclogger.dataload;

import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class LogStringParsingResult {
    private final String source;

    private boolean regexpMatches = true;
    private boolean crcFailed = false;
    private boolean wrongLoggerId = false;
    private boolean wrongTeamNumber = false;

    private String loggerId;
    private String scanpointOrder;
    private String teamInfo;
    private String recordDateTime;
    private String lineNumber;

    public LogStringParsingResult(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
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

    public boolean isWrongLoggerId() {
        return wrongLoggerId;
    }

    public void setWrongLoggerId(boolean wrongLoggerId) {
        this.wrongLoggerId = wrongLoggerId;
    }

    public boolean isWrongTeamNumber() {
        return wrongTeamNumber;
    }

    public void setWrongTeamNumber(boolean wrongTeamNumber) {
        this.wrongTeamNumber = wrongTeamNumber;
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

    public String getTeamInfo() {
        return teamInfo;
    }

    public void setTeamInfo(String teamInfo) {
        this.teamInfo = teamInfo;
    }

    public int extractTeamNumber() {
        return Integer.parseInt(teamInfo.substring(2, 6));
    }

    public String getRecordDateTime() {
        return recordDateTime;
    }

    public void setRecordDateTime(String recordDateTime) {
        this.recordDateTime = recordDateTime;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isError() {
        return !isRegexpMatches() || isWrongLoggerId() || isWrongTeamNumber() || isCrcFailed();
    }

    public String getErrorMessage() {
        if (!isRegexpMatches()) {
            return "ERROR [" + source + "] regexp parsing failed";
        }
        if (isCrcFailed()) {
            return "ERROR [" + source + "] CRC check failed";
        }
        if (isWrongLoggerId()) {
            return "ERROR [" + source + "] check LOGGER_ID failed";
        }
        if (isWrongTeamNumber()) {
            return "ERROR [" + source + "] check TEAM_NUMBER failed";
        }
        return "UNKNOWN ERROR";
    }

    public void checkConsistencyErrors(String confLoggerId) {
        if (!confLoggerId.equals(loggerId)) {
            setWrongLoggerId(true);
        }
        Team team = null;
        try {
            int teamNumber = extractTeamNumber();
            team = TeamsRegistry.getInstance().getTeamByNumber(teamNumber);
        } catch (NumberFormatException e) {
        }
        if (team == null) {
            setWrongTeamNumber(true);
        }
    }
}
