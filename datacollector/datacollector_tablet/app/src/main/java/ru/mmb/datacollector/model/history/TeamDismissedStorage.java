package ru.mmb.datacollector.model.history;

import java.util.HashMap;
import java.util.Map;

import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class TeamDismissedStorage
{
	private final Map<Integer, TeamDismissedState> teamDismissedStorage =
	    new HashMap<Integer, TeamDismissedState>();

	public void put(RawTeamLevelDismiss rawTeamLevelDismiss)
	{
		Integer teamId = rawTeamLevelDismiss.getTeamId();
		if (!teamDismissedStorage.containsKey(teamId))
		{
			teamDismissedStorage.put(teamId, new TeamDismissedState(rawTeamLevelDismiss.getTeam()));
		}
		TeamDismissedState teamDismissedState = teamDismissedStorage.get(teamId);
		teamDismissedState.add(rawTeamLevelDismiss);
	}

	public TeamDismissedState getTeamDismissedState(Integer teamId)
	{
		if (!teamDismissedStorage.containsKey(teamId))
		{
			teamDismissedStorage.put(teamId, new TeamDismissedState(TeamsRegistry.getInstance().getTeamById(teamId)));
		}
		return teamDismissedStorage.get(teamId);
	}

	public boolean containsTeamId(Integer teamId)
	{
		return teamDismissedStorage.containsKey(teamId);
	}
}
