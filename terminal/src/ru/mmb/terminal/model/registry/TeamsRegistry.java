package ru.mmb.terminal.model.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.Team;

public class TeamsRegistry
{
	private static TeamsRegistry instance = null;

	private List<Team> teams = null;
	private final Map<Integer, Team> teamByIdMap = new HashMap<Integer, Team>();
	private final Map<Integer, Map<Integer, Team>> teamsByNumber =
	    new HashMap<Integer, Map<Integer, Team>>();

	public static TeamsRegistry getInstance()
	{
		if (instance == null)
		{
			instance = new TeamsRegistry();
		}
		return instance;
	}

	private TeamsRegistry()
	{
		refresh();
	}

	public void refresh()
	{
		try
		{
			teams = TerminalDB.getConnectedInstance().loadTeams();
			refreshTeamByIdMap();
			refreshTeamsByNumberMap();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Team list load failed.", e);
		}
	}

	private void refreshTeamByIdMap()
	{
		teamByIdMap.clear();
		for (Team team : teams)
		{
			teamByIdMap.put(new Integer(team.getTeamId()), team);
		}
	}

	private void refreshTeamsByNumberMap()
	{
		clearTeamsByNumber();
		for (Team team : teams)
		{
			Integer distanceId = team.getDistanceId();
			if (!teamsByNumber.containsKey(distanceId))
			    teamsByNumber.put(distanceId, new HashMap<Integer, Team>());
			Map<Integer, Team> distanceTeamsByNumber = teamsByNumber.get(distanceId);
			distanceTeamsByNumber.put(new Integer(team.getTeamNum()), team);
		}
	}

	private void clearTeamsByNumber()
	{
		for (Map<Integer, Team> teams : teamsByNumber.values())
			teams.clear();
		teamsByNumber.clear();
	}

	public List<Team> getTeams(int distanceId)
	{
		List<Team> result = new ArrayList<Team>();
		for (Team team : teams)
		{
			if (team.getDistanceId() == distanceId) result.add(team);
		}
		return result;
	}

	public Team getTeamById(int teamId)
	{
		return teamByIdMap.get(new Integer(teamId));
	}

	public Team getTeamByNumber(Integer distanceId, Integer teamNumber)
	{
		Map<Integer, Team> distanceTeams = teamsByNumber.get(distanceId);
		if (distanceTeams == null) return null;
		return distanceTeams.get(teamNumber);
	}
}
