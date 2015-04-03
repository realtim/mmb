package ru.mmb.datacollector.model.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.mmb.datacollector.model.RawTeamLevelPoints;

public class RawTeamLevelPointsStorage
{
	private final Map<Integer, ScanPointRecords> teamResults = new HashMap<Integer, ScanPointRecords>();
	private final TreeMap<RawTeamLevelPointsUID, RawTeamLevelPoints> historyAccelerator =
	    new TreeMap<RawTeamLevelPointsUID, RawTeamLevelPoints>();
	private final Map<Integer, List<RawTeamLevelPointsUID>> teamToHistory =
	    new HashMap<Integer, List<RawTeamLevelPointsUID>>();

	public void put(RawTeamLevelPoints rawTeamLevelPoints)
	{
		int teamId = rawTeamLevelPoints.getTeamId();
		ScanPointRecords teamRecords = getOrCreateTeamRecords(teamId);
		teamRecords.put(rawTeamLevelPoints);
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
		List<RawTeamLevelPointsUID> uidsToRemove = teamToHistory.get(teamId);
		if (uidsToRemove == null) return;

		for (RawTeamLevelPointsUID teamLevelPointUID : uidsToRemove)
		{
			historyAccelerator.remove(teamLevelPointUID);
		}
		teamToHistory.remove(teamId);
	}

	private void addToHistoryAccelerator(Integer teamId, ScanPointRecords teamRecords)
	{
		if (teamToHistory.containsKey(teamId)) teamToHistory.remove(teamId);

		List<RawTeamLevelPoints> recordsToAdd = teamRecords.getLastRecord().getRawTeamLevelPoints();
		List<RawTeamLevelPointsUID> uidsToAdd = new ArrayList<RawTeamLevelPointsUID>();
		for (RawTeamLevelPoints rawTeamLevelPoints : recordsToAdd)
		{
			RawTeamLevelPointsUID uid = new RawTeamLevelPointsUID(rawTeamLevelPoints);
			historyAccelerator.put(uid, rawTeamLevelPoints);
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

	public List<RawTeamLevelPoints> getHistory()
	{
		return new ArrayList<RawTeamLevelPoints>(historyAccelerator.descendingMap().values());
	}
}
