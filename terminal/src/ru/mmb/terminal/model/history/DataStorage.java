package ru.mmb.terminal.model.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.TeamLevelDismiss;
import ru.mmb.terminal.model.TeamLevelPoint;

public class DataStorage
{
	private static DataStorage instance = null;

	/**
	 * DataStorage is recreated for level point.<br>
	 * Initialization load must be performed only once on history activity
	 * creation.<br>
	 * Special static methods will make data storage usable from
	 * InputDataActivity and WithdrawMemberActivity.
	 * 
	 * @param levelPointId
	 * @return
	 */
	public static DataStorage getInstance(LevelPoint levelPoint)
	{
		if (instance == null
		        || instance.getLevelPoint().getLevelPointId() != levelPoint.getLevelPointId())
		{
			instance = new DataStorage(levelPoint);
		}
		return instance;
	}

	private final LevelPoint levelPoint;

	private final Set<Integer> levelPointTeams = new HashSet<Integer>();
	private final TeamLevelPointsStorage teamLevelPoints = new TeamLevelPointsStorage();
	private final TeamDismissedStorage teamDismissed = new TeamDismissedStorage();

	private DataStorage(LevelPoint levelPoint)
	{
		this.levelPoint = levelPoint;
		initLevelPointTeams();
		initTeamLevelPoints();
		initTeamDismissed();
	}

	private void initLevelPointTeams()
	{
		levelPointTeams.clear();
		TerminalDB.getInstance().appendLevelPointTeams(levelPoint, levelPointTeams);
	}

	private void initTeamLevelPoints()
	{
		List<TeamLevelPoint> inputData = TerminalDB.getInstance().loadTeamLevelPoints(levelPoint);
		for (TeamLevelPoint teamLevelPoint : inputData)
		{
			teamLevelPoints.put(teamLevelPoint);
		}
	}

	private void initTeamDismissed()
	{
		List<TeamLevelDismiss> dismissed =
		    TerminalDB.getInstance().loadDismissedMembers(levelPoint);
		for (TeamLevelDismiss teamLevelDismiss : dismissed)
		{
			teamDismissed.put(teamLevelDismiss);
		}
	}

	public List<HistoryInfo> getHistory()
	{
		List<HistoryInfo> result = new ArrayList<HistoryInfo>();
		Set<Integer> teamIds = new HashSet<Integer>(levelPointTeams);
		List<TeamLevelPoint> levelPointsHistory = teamLevelPoints.getHistory();
		for (TeamLevelPoint teamLevelPoint : levelPointsHistory)
		{
			Integer teamId = teamLevelPoint.getTeam().getTeamId();
			TeamDismissedState teamDismissedState = teamDismissed.getTeamDismissedState(teamId);
			result.add(new HistoryInfo(teamId, teamLevelPoint, teamDismissedState));
			teamIds.remove(teamId);
		}
		appendDismissedWithoutData(teamIds, result);
		return result;
	}

	private void appendDismissedWithoutData(Set<Integer> teamIds, List<HistoryInfo> result)
	{
		if (teamIds.isEmpty()) return;
		int addedCount = 0;
		for (Integer teamId : teamIds)
		{
			if (teamDismissed.containsTeamId(teamId))
			{
				TeamDismissedState teamDismissedState = teamDismissed.getTeamDismissedState(teamId);
				result.add(new HistoryInfo(teamId, null, teamDismissedState));
				addedCount++;
			}
		}
		if (addedCount > 0)
		{
			Collections.sort(result, new Comparator<HistoryInfo>()
			{
				@Override
				public int compare(HistoryInfo object1, HistoryInfo object2)
				{
					return -1 * object1.compareTo(object2);
				}
			});
		}
	}

	private Set<Integer> getLevelPointTeams()
	{
		return levelPointTeams;
	}

	private TeamLevelPointsStorage getTeamLevelPoints()
	{
		return teamLevelPoints;
	}

	public LevelPoint getLevelPoint()
	{
		return levelPoint;
	}

	private TeamDismissedStorage getTeamDismissed()
	{
		return teamDismissed;
	}

	public static void putTeamLevelPoint(TeamLevelPoint teamLevelPoint)
	{
		if (instance.getLevelPoint().getLevelPointId() == teamLevelPoint.getLevelPointId())
		{
			instance.getTeamLevelPoints().put(teamLevelPoint);
			instance.getLevelPointTeams().add(teamLevelPoint.getTeamId());
		}
		else
		{
			String message =
			    "Fatal error." + "\n" + "Current data storage level point ["
			            + instance.getLevelPoint() + "]" + "\n"
			            + "Putting new team level point to [" + teamLevelPoint.getLevelPoint()
			            + "]";
			throw new DataStorageException(message);
		}
	}

	public static void putTeamLevelDismiss(TeamLevelDismiss teamLevelDismiss)
	{
		if (instance.getLevelPoint().getLevelPointId() == teamLevelDismiss.getLevelPointId())
		{
			instance.getTeamDismissed().put(teamLevelDismiss);
			instance.getLevelPointTeams().add(teamLevelDismiss.getTeamId());
		}
		else
		{
			String message =
			    "Fatal error." + "\n" + "Current data storage level point ["
			            + instance.getLevelPoint() + "]" + "\n"
			            + "Putting new team level dismiss to [" + teamLevelDismiss.getLevelPoint()
			            + "]";
			throw new DataStorageException(message);
		}
	}
}
