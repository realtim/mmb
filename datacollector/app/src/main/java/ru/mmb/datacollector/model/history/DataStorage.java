package ru.mmb.datacollector.model.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.db.DatacollectorDB;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.TeamDismiss;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.exception.DataStorageException;

public class DataStorage
{
	private static DataStorage instance = null;

	/**
	 * DataStorage is recreated for scan point.<br>
	 * Initialization load must be performed only once on history activity
	 * creation.<br>
	 * Special static methods will make data storage usable from
	 * InputDataActivity and WithdrawMemberActivity.
	 * 
	 * @param levelPoint
	 * @return
	 */
	public static DataStorage getInstance(ScanPoint scanPoint)
	{
		if (instance == null
		        || instance.getScanPoint().getScanPointId() != scanPoint.getScanPointId())
		{
			instance = new DataStorage(scanPoint);
		}
		return instance;
	}

	public static void reset()
	{
		instance = null;
	}

	private final ScanPoint scanPoint;

	private final Set<Integer> scanPointTeams = new HashSet<Integer>();
	private final TeamResultsStorage teamResultsStorage = new TeamResultsStorage();
	private final TeamDismissedStorage teamDismissedStorage = new TeamDismissedStorage();

	private DataStorage(ScanPoint scanPoint)
	{
		this.scanPoint = scanPoint;
		initScanPointTeams();
		initTeamResultsStorage();
		initTeamDismissedStorage();
	}

	private void initScanPointTeams()
	{
		scanPointTeams.clear();
		for (LevelPoint levelPoint : scanPoint.getLevelPoints().values())
		{
			DatacollectorDB.getConnectedInstance().appendLevelPointTeams(levelPoint, scanPointTeams);
		}
	}

	private void initTeamResultsStorage()
	{
		for (LevelPoint levelPoint : scanPoint.getLevelPoints().values())
		{
			List<TeamResult> inputData =
			    DatacollectorDB.getConnectedInstance().loadTeamResults(levelPoint);
			for (TeamResult teamLevelPoint : inputData)
			{
				teamResultsStorage.put(teamLevelPoint);
			}
		}
	}

	private void initTeamDismissedStorage()
	{
		for (LevelPoint levelPoint : scanPoint.getLevelPoints().values())
		{
			List<TeamDismiss> dismissed =
			    DatacollectorDB.getConnectedInstance().loadDismissedMembers(levelPoint);
			for (TeamDismiss teamDismiss : dismissed)
			{
				teamDismissedStorage.put(teamDismiss);
			}
		}
	}

	public List<HistoryInfo> getHistory()
	{
		List<HistoryInfo> result = new ArrayList<HistoryInfo>();
		Set<Integer> teamIds = new HashSet<Integer>(scanPointTeams);
		List<TeamResult> resultsHistory = teamResultsStorage.getHistory();
		for (TeamResult teamResult : resultsHistory)
		{
			Integer teamId = teamResult.getTeam().getTeamId();
			TeamDismissedState teamDismissedState =
			    teamDismissedStorage.getTeamDismissedState(teamId);
			result.add(new HistoryInfo(teamId, teamResult, teamDismissedState));
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
			if (teamDismissedStorage.containsTeamId(teamId))
			{
				TeamDismissedState teamDismissedState =
				    teamDismissedStorage.getTeamDismissedState(teamId);
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

	private Set<Integer> getScanPointTeams()
	{
		return scanPointTeams;
	}

	private TeamResultsStorage getTeamResultsStorage()
	{
		return teamResultsStorage;
	}

	public ScanPoint getScanPoint()
	{
		return scanPoint;
	}

	private TeamDismissedStorage getTeamDismissedStorage()
	{
		return teamDismissedStorage;
	}

	public static void putTeamResult(TeamResult teamResult)
	{
		if (instance.getScanPoint().getScanPointId() == teamResult.getScanPointId())
		{
			instance.getTeamResultsStorage().put(teamResult);
			instance.getScanPointTeams().add(teamResult.getTeamId());
		}
		else
		{
			String message =
			    "Fatal error." + "\n" + "Current HISTORY data storage scan point ["
			            + instance.getScanPoint() + "]" + "\n" + "Putting new team result to ["
			            + teamResult.getScanPoint() + "]";
			throw new DataStorageException(message);
		}
	}

	public static void putTeamDismiss(TeamDismiss teamDismiss)
	{
		if (instance.getScanPoint().getScanPointId() == teamDismiss.getScanPointId())
		{
			instance.getTeamDismissedStorage().put(teamDismiss);
			instance.getScanPointTeams().add(teamDismiss.getTeamId());
		}
		else
		{
			String message =
			    "Fatal error." + "\n" + "Current data storage scan point ["
			            + instance.getScanPoint() + "]" + "\n" + "Putting new team dismiss to ["
			            + teamDismiss.getScanPoint() + "]";
			throw new DataStorageException(message);
		}
	}
}
