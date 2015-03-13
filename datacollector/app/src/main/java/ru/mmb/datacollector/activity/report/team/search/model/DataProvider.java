package ru.mmb.datacollector.activity.report.team.search.model;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.activity.report.team.search.SortColumn;
import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class DataProvider
{
	public List<TeamListRecord> getTeams(SortColumn sortColumn)
	{
		List<TeamListRecord> result = new ArrayList<TeamListRecord>();
		for (Team team : TeamsRegistry.getInstance().getTeams())
		{
			addTeamMembers(result, team, sortColumn);
		}
		return result;
	}

	private void addTeamMembers(List<TeamListRecord> result, Team team, SortColumn sortColumn)
	{
		if (sortColumn == SortColumn.MEMBER)
		{
			for (Participant participant : team.getMembers())
			{
				result.add(new TeamMember(team, participant.getUserName()));
			}
		}
		else
		{
			result.add(new TeamMembers(team));
		}
	}
}
