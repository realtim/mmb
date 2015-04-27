package ru.mmb.datacollector.activity.report.team.result;

import java.util.Comparator;

import ru.mmb.datacollector.activity.report.team.result.model.TeamListRecord;

public class TeamMemberItemsComparator implements Comparator<TeamListRecord>
{
	private final TeamResultActivityState currentState;

	public TeamMemberItemsComparator(TeamResultActivityState currentState)
	{
		this.currentState = currentState;
	}

	@Override
	public int compare(TeamListRecord item1, TeamListRecord item2)
	{
		int result = 0;
		if (currentState.getSortColumn() == SortColumn.NUMBER)
		    result =
		        new Integer(item1.getTeamNumber()).compareTo(new Integer(item2.getTeamNumber()));
		if (currentState.getSortColumn() == SortColumn.TEAM)
		    result = item1.getTeamName().compareToIgnoreCase(item2.getTeamName());
		if (currentState.getSortColumn() == SortColumn.MEMBER)
		    result = item1.getMemberText().compareToIgnoreCase(item2.getMemberText());
		return currentState.getSortOrder() == SortOrder.ASC ? result : -1 * result;
	}
}
