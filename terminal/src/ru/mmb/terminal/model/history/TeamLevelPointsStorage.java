package ru.mmb.terminal.model.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.mmb.terminal.model.TeamLevelPoint;

public class TeamLevelPointsStorage
{
	private final Map<Integer, LevelPointRecords> teamLevelPoints = new HashMap<Integer, LevelPointRecords>();
	private final TreeMap<TeamLevelPointUID, TeamLevelPoint> historyAccelerator =
	    new TreeMap<TeamLevelPointUID, TeamLevelPoint>();
	private final Map<Integer, List<TeamLevelPointUID>> teamToHistory =
	    new HashMap<Integer, List<TeamLevelPointUID>>();

	public void put(TeamLevelPoint teamLevelPoint)
	{
		int teamId = teamLevelPoint.getTeamId();
		LevelPointRecords teamRecords = getOrCreateTeamRecords(teamId);
		teamRecords.put(teamLevelPoint);
		removeFromHistoryAccelerator(teamId);
		addToHistoryAccelerator(teamId, teamRecords);
	}

	private LevelPointRecords getOrCreateTeamRecords(Integer teamId)
	{
		if (!teamLevelPoints.containsKey(teamId))
		{
			teamLevelPoints.put(teamId, new LevelPointRecords());
		}
		return teamLevelPoints.get(teamId);
	}

	private void removeFromHistoryAccelerator(Integer teamId)
	{
		List<TeamLevelPointUID> uidsToRemove = teamToHistory.get(teamId);
		if (uidsToRemove == null) return;

		for (TeamLevelPointUID teamLevelPointUID : uidsToRemove)
		{
			historyAccelerator.remove(teamLevelPointUID);
		}
		teamToHistory.remove(teamId);
	}

	private void addToHistoryAccelerator(Integer teamId, LevelPointRecords teamRecords)
	{
		if (teamToHistory.containsKey(teamId)) teamToHistory.remove(teamId);

		List<TeamLevelPoint> recordsToAdd = teamRecords.getLastRecord().getTeamLevelPoints();
		List<TeamLevelPointUID> uidsToAdd = new ArrayList<TeamLevelPointUID>();
		for (TeamLevelPoint teamLevelPoint : recordsToAdd)
		{
			TeamLevelPointUID uid = new TeamLevelPointUID(teamLevelPoint);
			historyAccelerator.put(uid, teamLevelPoint);
			uidsToAdd.add(uid);
		}
		teamToHistory.put(teamId, uidsToAdd);
	}

	public Object size()
	{
		return teamLevelPoints.size();
	}

	public LevelPointRecords getByTeam(Integer teamId)
	{
		return teamLevelPoints.get(teamId);
	}

	public List<TeamLevelPoint> getHistory()
	{
		return new ArrayList<TeamLevelPoint>(historyAccelerator.descendingMap().values());
	}
}
