package ru.mmb.datacollector.model.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.datacollector.model.RawTeamLevelPoints;

public class ScanPointRecord
{
	private final Map<Integer, RawTeamLevelPoints> scanPointRecord = new TreeMap<Integer, RawTeamLevelPoints>();

	public RawTeamLevelPoints put(RawTeamLevelPoints rawTeamLevelPoints)
	{
		return scanPointRecord.put(rawTeamLevelPoints.getUserId(), rawTeamLevelPoints);
	}

	public boolean containsUserId(Integer userId)
	{
		return scanPointRecord.containsKey(userId);
	}

	public Set<Integer> getUserIds()
	{
		return scanPointRecord.keySet();
	}

	public void clear()
	{
		scanPointRecord.clear();
	}

	public RawTeamLevelPoints getByUserId(Integer userId)
	{
		return scanPointRecord.get(userId);
	}

	public RawTeamLevelPoints removeByUserId(Integer userId)
	{
		return scanPointRecord.remove(userId);
	}

	public int size()
	{
		return scanPointRecord.size();
	}

	public boolean isEmpty()
	{
		return scanPointRecord.isEmpty();
	}

	public List<RawTeamLevelPoints> getRawTeamLevelPoints()
	{
		List<RawTeamLevelPoints> result = new ArrayList<RawTeamLevelPoints>();
		result.addAll(scanPointRecord.values());
		return result;
	}
}
