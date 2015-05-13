package ru.mmb.datacollector.activity.report.team.search.search;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.Team;

public class TeamListRecord {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Date arrival;
    private final Team team;

    public TeamListRecord(RawLoggerData rawLoggerData) {
        this.team = rawLoggerData.getTeam();
        this.arrival = rawLoggerData.getRecordDateTime();
    }

    public int getTeamNumber() {
        return team.getTeamNum();
    }

    public String getTeamName() {
        return team.getTeamName();
    }

    public String getMembersText() {
        return team.getMembersText();
    }

    public Team getTeam() {
        return team;
    }

    public String getArrivalText() {
        return sdf.format(arrival);
    }

    public int getTeamId() {
        return team.getTeamId();
    }
}
