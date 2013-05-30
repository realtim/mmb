package ru.mmb.terminal.model.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.mmb.terminal.model.TeamLevelPoint;

public class LevelPointRecord
{
	private final Map<Integer, TeamLevelPoint> levelPointRecord = new TreeMap<Integer, TeamLevelPoint>();

	public TeamLevelPoint put(TeamLevelPoint teamLevelPoint)
	{
		return levelPointRecord.put(teamLevelPoint.getUserId(), teamLevelPoint);
	}

	public boolean containsUserId(Integer userId)
	{
		return levelPointRecord.containsKey(userId);
	}

	public Set<Integer> getUserIds()
	{
		return levelPointRecord.keySet();
	}

	public void clear()
	{
		levelPointRecord.clear();
	}

	public TeamLevelPoint getByUserId(Integer userId)
	{
		return levelPointRecord.get(userId);
	}

	public TeamLevelPoint removeByUserId(Integer userId)
	{
		return levelPointRecord.remove(userId);
	}

	public int size()
	{
		return levelPointRecord.size();
	}

	public boolean isEmpty()
	{
		return levelPointRecord.isEmpty();
	}

	public List<TeamLevelPoint> getTeamLevelPoints()
	{
		List<TeamLevelPoint> result = new ArrayList<TeamLevelPoint>();
		result.addAll(levelPointRecord.values());
		return result;
	}
}
