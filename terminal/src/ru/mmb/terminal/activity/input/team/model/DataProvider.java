package ru.mmb.terminal.activity.input.team.model;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.activity.input.team.SortColumn;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.registry.TeamsRegistry;

public class DataProvider
{
	public List<TeamListRecord> getTeams(int distanceId, SortColumn sortColumn)
	{
		List<TeamListRecord> result = new ArrayList<TeamListRecord>();
		for (Team team : TeamsRegistry.getInstance().getTeams(distanceId))
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
				result.add(new TeamMember(team, participant.getName()));
			}
		}
		else
		{
			result.add(new TeamMembers(team));
		}
	}
}
