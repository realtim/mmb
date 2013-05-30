package ru.mmb.terminal.model.history;

import java.util.Date;

import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamLevelPoint;
import ru.mmb.terminal.model.registry.TeamsRegistry;

public class HistoryInfo implements Comparable<HistoryInfo>
{
	private final Team team;
	private final TeamLevelPoint teamLevelPoint;
	private final TeamDismissedState teamDismissedState;
	private final boolean compareByLevelPoint;
	private final Date comparisonDate;

	public HistoryInfo(Integer teamId, TeamLevelPoint teamLevelPoint, TeamDismissedState teamDismissedState)
	{
		this.team = TeamsRegistry.getInstance().getTeamById(teamId);
		this.teamLevelPoint = teamLevelPoint;
		this.teamDismissedState = teamDismissedState;

		if (teamLevelPoint == null && teamDismissedState == null)
		    throw new RuntimeException("HistoryInfo failed. teamLevelPoint and teamDismissState NULL for team ["
		            + teamId + "]");

		this.compareByLevelPoint = !isCompareByDismissed(teamLevelPoint, teamDismissedState);
		this.comparisonDate =
		    (this.compareByLevelPoint) ? teamLevelPoint.getRecordDateTime()
		            : teamDismissedState.getLastRecordDateTime();
	}

	private boolean isCompareByDismissed(TeamLevelPoint teamLevelPoint,
	        TeamDismissedState teamDismissedState)
	{
		if (teamLevelPoint == null) return true;
		return teamDismissedState.getLastRecordDateTime().after(teamLevelPoint.getRecordDateTime());
	}

	public String buildLevelPointInfoText()
	{
		return (teamLevelPoint == null) ? "" : teamLevelPoint.buildInfoText();
	}

	public String buildMembersInfo()
	{
		return teamDismissedState.buildMembersInfo();
	}

	public String buildDismissedInfo()
	{
		return teamDismissedState.buildDismissedInfo();
	}

	public Team getTeam()
	{
		return team;
	}

	public Integer getLevelPointUserId()
	{
		return teamLevelPoint.getUserId();
	}

	@Override
	public int compareTo(HistoryInfo another)
	{
		int result = comparisonDate.compareTo(another.comparisonDate);
		if (result == 0)
		{
			if (compareByLevelPoint && another.compareByLevelPoint)
			{
				result = teamLevelPoint.compareTo(another.teamLevelPoint);
			}
		}
		return result;
	}
}
