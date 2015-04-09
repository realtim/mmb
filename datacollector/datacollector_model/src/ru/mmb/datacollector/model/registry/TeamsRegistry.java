package ru.mmb.datacollector.model.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.db.DatabaseAdapter;
import ru.mmb.datacollector.model.Team;

public class TeamsRegistry {
	private static TeamsRegistry instance = null;

	private List<Team> teams = null;
	private final Map<Integer, Team> teamByIdMap = new HashMap<Integer, Team>();
	private final Map<Integer, Team> teamByNumberMap = new HashMap<Integer, Team>();

	public static TeamsRegistry getInstance() {
		if (instance == null) {
			instance = new TeamsRegistry();
		}
		return instance;
	}

	private TeamsRegistry() {
		refresh();
	}

	public void refresh() {
		try {
			teams = DatabaseAdapter.getConnectedInstance().loadTeams();
			refreshTeamByIdMap();
			refreshTeamsByNumberMap();
		} catch (Exception e) {
			throw new RuntimeException("Team list load failed.", e);
		}
	}

	private void refreshTeamByIdMap() {
		teamByIdMap.clear();
		for (Team team : teams) {
			teamByIdMap.put(new Integer(team.getTeamId()), team);
		}
	}

	private void refreshTeamsByNumberMap() {
		teamByNumberMap.clear();
		for (Team team : teams) {
			teamByNumberMap.put(new Integer(team.getTeamNum()), team);
		}
	}

	public List<Team> getTeams(int distanceId) {
		List<Team> result = new ArrayList<Team>();
		for (Team team : teams) {
			if (team.getDistanceId() == distanceId)
				result.add(team);
		}
		return result;
	}

	public List<Team> getTeams() {
		return new ArrayList<Team>(teams);
	}

	public Team getTeamById(int teamId) {
		return teamByIdMap.get(new Integer(teamId));
	}

	public Team getTeamByNumber(int teamNumber) {
		return teamByNumberMap.get(teamNumber);
	}
}
