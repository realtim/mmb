package ru.mmb.terminal.model.registry;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.Team;

public class TeamsRegistry
{
	private static TeamsRegistry instance = null;

	private List<Team> teams = null;

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
			teams = TerminalDB.getInstance().loadTeams();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Team list load failed.", e);
		}
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
		for (Team team : teams)
		{
			if (team.getTeamId() == teamId) return team;
		}
		return null;
	}
}
