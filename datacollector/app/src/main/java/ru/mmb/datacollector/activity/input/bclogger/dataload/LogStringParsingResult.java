package ru.mmb.datacollector.activity.input.bclogger.dataload;

import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class LogStringParsingResult {
    private final String source;

    private boolean regexpMatches = true;
    private boolean crcFailed = false;
    private boolean wrongLoggerId = false;
    private boolean wrongTeamId = false;

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

    public boolean isWrongTeamId() {
        return wrongTeamId;
    }

    public void setWrongTeamId(boolean wrongTeamId) {
        this.wrongTeamId = wrongTeamId;
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

    public int extractTeamId() {
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
        return !isRegexpMatches() || isWrongLoggerId() || isWrongTeamId() || isCrcFailed();
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
        if (isWrongTeamId()) {
            return "ERROR [" + source + "] check TEAM_ID failed";
        }
        return "UNKNOWN ERROR";
    }

    public void checkConsistencyErrors(String confLoggerId) {
        if (!confLoggerId.equals(loggerId)) {
            setWrongLoggerId(true);
        }
        Team team = null;
        try {
            int teamId = extractTeamId();
            team = TeamsRegistry.getInstance().getTeamById(teamId);
        } catch (NumberFormatException e) {
        }
        if (team == null) {
            setWrongTeamId(true);
        }
    }
}
