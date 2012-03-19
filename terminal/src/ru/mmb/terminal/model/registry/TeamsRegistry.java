package ru.mmb.terminal.model.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.util.ExternalStorage;

public class TeamsRegistry extends AbstractRegistry
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
		load();
	}

	private void load()
	{
		try
		{
			List<Participant> participants = loadParticipants();
			Map<Integer, List<Participant>> teamParticipants =
			    groupChildrenByParentId(participants);
			teams = loadTeams(teamParticipants);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Team list load failed.", e);
		}
	}

	private List<Participant> loadParticipants() throws Exception
	{
		return loadStoredElements(ExternalStorage.getDir() + "/mmb/model/participants.csv", Participant.class);
	}

	private List<Team> loadTeams(Map<Integer, List<Participant>> teamParticipants) throws Exception
	{
		return loadStoredElements(ExternalStorage.getDir() + "/mmb/model/teams.csv", Team.class, teamParticipants);
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
			if (team.getId() == teamId) return team;
		}
		return null;
	}
}
