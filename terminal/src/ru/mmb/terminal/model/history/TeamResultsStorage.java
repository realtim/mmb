package ru.mmb.terminal.model.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.mmb.terminal.model.TeamResult;

public class TeamResultsStorage
{
	private final Map<Integer, ScanPointRecords> teamResults = new HashMap<Integer, ScanPointRecords>();
	private final TreeMap<TeamResultUID, TeamResult> historyAccelerator =
	    new TreeMap<TeamResultUID, TeamResult>();
	private final Map<Integer, List<TeamResultUID>> teamToHistory =
	    new HashMap<Integer, List<TeamResultUID>>();

	public void put(TeamResult teamResult)
	{
		int teamId = teamResult.getTeamId();
		ScanPointRecords teamRecords = getOrCreateTeamRecords(teamId);
		teamRecords.put(teamResult);
		removeFromHistoryAccelerator(teamId);
		addToHistoryAccelerator(teamId, teamRecords);
	}

	private ScanPointRecords getOrCreateTeamRecords(Integer teamId)
	{
		if (!teamResults.containsKey(teamId))
		{
			teamResults.put(teamId, new ScanPointRecords());
		}
		return teamResults.get(teamId);
	}

	private void removeFromHistoryAccelerator(Integer teamId)
	{
		List<TeamResultUID> uidsToRemove = teamToHistory.get(teamId);
		if (uidsToRemove == null) return;

		for (TeamResultUID teamLevelPointUID : uidsToRemove)
		{
			historyAccelerator.remove(teamLevelPointUID);
		}
		teamToHistory.remove(teamId);
	}

	private void addToHistoryAccelerator(Integer teamId, ScanPointRecords teamRecords)
	{
		if (teamToHistory.containsKey(teamId)) teamToHistory.remove(teamId);

		List<TeamResult> recordsToAdd = teamRecords.getLastRecord().getTeamResults();
		List<TeamResultUID> uidsToAdd = new ArrayList<TeamResultUID>();
		for (TeamResult teamResult : recordsToAdd)
		{
			TeamResultUID uid = new TeamResultUID(teamResult);
			historyAccelerator.put(uid, teamResult);
			uidsToAdd.add(uid);
		}
		teamToHistory.put(teamId, uidsToAdd);
	}

	public Object size()
	{
		return teamResults.size();
	}

	public ScanPointRecords getByTeam(Integer teamId)
	{
		return teamResults.get(teamId);
	}

	public List<TeamResult> getHistory()
	{
		return new ArrayList<TeamResult>(historyAccelerator.descendingMap().values());
	}
}
