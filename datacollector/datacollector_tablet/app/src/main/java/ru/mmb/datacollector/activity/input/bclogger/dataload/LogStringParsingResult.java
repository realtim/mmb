package ru.mmb.datacollector.activity.input.bclogger.dataload;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.PointType;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.util.DateUtils;

public class LogStringParsingResult {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy/MM/dd");

    private final String source;

    private boolean regexpMatches = true;
    private boolean crcFailed = false;
    private boolean wrongLoggerId = false;
    private boolean wrongTeamNumber = false;
    private boolean wrongRecordDateTime = false;

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

    public boolean isWrongRecordDateTime() {
        return wrongRecordDateTime;
    }

    public void setWrongRecordDateTime(boolean wrongRecordDateTime) {
        this.wrongRecordDateTime = wrongRecordDateTime;
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

    public boolean isFatalError() {
        return !isRegexpMatches() || isWrongLoggerId() || isWrongTeamNumber() || isCrcFailed();
    }

    public boolean isDateError() {
        return isRegexpMatches() && isWrongRecordDateTime();
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
        if (isWrongRecordDateTime()) {
            return "ERROR [" + source + "] check RECORD_DATE_TIME failed";
        }
        return "UNKNOWN ERROR";
    }

    public void checkConsistencyErrors(String confLoggerId, ScanPoint scanPoint) {
        // check logger ID
        if (confLoggerId != null && !confLoggerId.equals(loggerId)) {
            setWrongLoggerId(true);
        }
        // check team ID
        Team team = null;
        try {
            int teamNumber = extractTeamNumber();
            team = TeamsRegistry.getInstance().getTeamByNumber(teamNumber);
        } catch (NumberFormatException e) {
        }
        if (team == null) {
            setWrongTeamNumber(true);
        }
        // check record date and time
        if (team != null) {
            try {
                LevelPoint levelPoint = scanPoint.getLevelPointByDistance(team.getDistanceId());
                Date recordDateTimeMinutes = DateUtils.trimToMinutes(sdf.parse(recordDateTime));
                Date dateFrom = levelPoint.getLevelPointMinDateTime();
                Date dateTo = levelPoint.getLevelPointMaxDateTime();
                if (levelPoint.getPointType() == PointType.START) {
                    dateFrom = shiftTimeForStart(dateFrom, -10);
                    // check start only for levelPointMinDateTime
                    // if started after max time, then start time = max
                    if (recordDateTimeMinutes.before(dateFrom)) {
                        setWrongRecordDateTime(true);
                    }
                } else {
                    // check other levelPoints for hitting min and max limits
                    if (recordDateTimeMinutes.before(dateFrom) || recordDateTimeMinutes.after(dateTo)) {
                        setWrongRecordDateTime(true);
                    }
                }
            } catch (Exception e) {
                setWrongRecordDateTime(true);
            }
        }
    }

    private Date shiftTimeForStart(Date date, int minutesShift) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutesShift);
        return calendar.getTime();
    }
}
