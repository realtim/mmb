package ru.mmb.datacollector.model.history;

import java.util.Date;

import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class HistoryInfo implements Comparable<HistoryInfo> {
    private final Team team;
    private final RawTeamLevelPoints rawTeamLevelPoints;
    private final TeamDismissedState teamDismissedState;
    private final boolean compareByLevelPoint;
    private final Date comparisonDate;

    public HistoryInfo(Integer teamId, RawTeamLevelPoints rawTeamLevelPoints, TeamDismissedState teamDismissedState) {
        this.team = TeamsRegistry.getInstance().getTeamById(teamId);
        this.rawTeamLevelPoints = rawTeamLevelPoints;
        this.teamDismissedState = teamDismissedState;

        if (rawTeamLevelPoints == null && teamDismissedState == null)
            throw new RuntimeException(
                    "HistoryInfo failed. rawTeamLevelPoints and teamDismissState NULL for team ["
                    + teamId + "]");

        this.compareByLevelPoint = !isCompareByDismissed(rawTeamLevelPoints, teamDismissedState);
        this.comparisonDate =
                (this.compareByLevelPoint) ? rawTeamLevelPoints.getRecordDateTime()
                        : teamDismissedState.getLastRecordDateTime();
    }

    private boolean isCompareByDismissed(RawTeamLevelPoints rawTeamLevelPoints, TeamDismissedState teamDismissedState) {
        if (rawTeamLevelPoints == null) return true;
        return teamDismissedState.getLastRecordDateTime().after(rawTeamLevelPoints.getRecordDateTime());
    }

    public String buildScanPointInfoText() {
        return (rawTeamLevelPoints == null) ? "" : rawTeamLevelPoints.buildInfoText();
    }

    public String buildMembersInfo() {
        return teamDismissedState.buildMembersInfo();
    }

    public String buildDismissedInfo() {
        return teamDismissedState.buildDismissedInfo();
    }

    public Team getTeam() {
        return team;
    }

    public Integer getUserId() {
        return rawTeamLevelPoints.getUserId();
    }

    @Override
    public int compareTo(HistoryInfo another) {
        int result = comparisonDate.compareTo(another.comparisonDate);
        if (result == 0) {
            if (compareByLevelPoint && another.compareByLevelPoint) {
                result = rawTeamLevelPoints.compareTo(another.rawTeamLevelPoints);
            }
        }
        return result;
    }
}
