package ru.mmb.datacollector.model.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.datacollector.model.TeamResult;

public class ScanPointRecord
{
	private final Map<Integer, TeamResult> scanPointRecord = new TreeMap<Integer, TeamResult>();

	public TeamResult put(TeamResult teamResult)
	{
		return scanPointRecord.put(teamResult.getUserId(), teamResult);
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

	public TeamResult getByUserId(Integer userId)
	{
		return scanPointRecord.get(userId);
	}

	public TeamResult removeByUserId(Integer userId)
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

	public List<TeamResult> getTeamResults()
	{
		List<TeamResult> result = new ArrayList<TeamResult>();
		result.addAll(scanPointRecord.values());
		return result;
	}
}
