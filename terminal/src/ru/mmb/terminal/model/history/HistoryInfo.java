package ru.mmb.terminal.model.history;

import java.util.Date;

import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamResult;
import ru.mmb.terminal.model.registry.TeamsRegistry;

public class HistoryInfo implements Comparable<HistoryInfo>
{
	private final Team team;
	private final TeamResult teamResult;
	private final TeamDismissedState teamDismissedState;
	private final boolean compareByLevelPoint;
	private final Date comparisonDate;

	public HistoryInfo(Integer teamId, TeamResult teamResult, TeamDismissedState teamDismissedState)
	{
		this.team = TeamsRegistry.getInstance().getTeamById(teamId);
		this.teamResult = teamResult;
		this.teamDismissedState = teamDismissedState;

		if (teamResult == null && teamDismissedState == null)
		    throw new RuntimeException("HistoryInfo failed. teamResult and teamDismissState NULL for team ["
		            + teamId + "]");

		this.compareByLevelPoint = !isCompareByDismissed(teamResult, teamDismissedState);
		this.comparisonDate =
		    (this.compareByLevelPoint) ? teamResult.getRecordDateTime()
		            : teamDismissedState.getLastRecordDateTime();
	}

	private boolean isCompareByDismissed(TeamResult teamResult,
	        TeamDismissedState teamDismissedState)
	{
		if (teamResult == null) return true;
		return teamDismissedState.getLastRecordDateTime().after(teamResult.getRecordDateTime());
	}

	public String buildScanPointInfoText()
	{
		return (teamResult == null) ? "" : teamResult.buildInfoText();
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

	public Integer getUserId()
	{
		return teamResult.getUserId();
	}

	@Override
	public int compareTo(HistoryInfo another)
	{
		int result = comparisonDate.compareTo(another.comparisonDate);
		if (result == 0)
		{
			if (compareByLevelPoint && another.compareByLevelPoint)
			{
				result = teamResult.compareTo(another.teamResult);
			}
		}
		return result;
	}
}
