package ru.mmb.datacollector.model.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.ScanPoint;

public class DataStorage {
    private static DataStorage instance = null;

    /**
     * DataStorage is recreated for scan point.<br>
     * Initialization load must be performed only once on history activity
     * creation.<br>
     * Special static methods will make data storage usable from
     * InputDataActivity and WithdrawMemberActivity.
     *
     * @param scanPoint
     * @return
     */
    public static DataStorage getInstance(ScanPoint scanPoint) {
        if (instance == null
            || instance.getScanPoint().getScanPointId() != scanPoint.getScanPointId()) {
            instance = new DataStorage(scanPoint);
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }

    private final ScanPoint scanPoint;

    private final Set<Integer> scanPointTeams = new HashSet<Integer>();
    private final RawTeamLevelPointsStorage rawTeamLevelPointsStorage = new RawTeamLevelPointsStorage();
    private final TeamDismissedStorage teamDismissedStorage = new TeamDismissedStorage();

    private DataStorage(ScanPoint scanPoint) {
        this.scanPoint = scanPoint;
        initScanPointTeams();
        initRawTeamLevelPointsStorage();
        initTeamDismissedStorage();
    }

    private void initScanPointTeams() {
        scanPointTeams.clear();
        SQLiteDatabaseAdapter.getConnectedInstance().appendScanPointTeams(scanPoint, scanPointTeams);
    }

    private void initRawTeamLevelPointsStorage() {
        List<RawTeamLevelPoints> inputData =
                SQLiteDatabaseAdapter.getConnectedInstance().loadRawTeamLevelPoints(scanPoint);
        for (RawTeamLevelPoints rawTeamLevelPoints : inputData) {
            rawTeamLevelPointsStorage.put(rawTeamLevelPoints);
        }
    }

    private void initTeamDismissedStorage() {
        List<RawTeamLevelDismiss> dismissed =
                SQLiteDatabaseAdapter.getConnectedInstance().loadDismissedMembers(scanPoint);
        for (RawTeamLevelDismiss rawTeamLevelDismiss : dismissed) {
            teamDismissedStorage.put(rawTeamLevelDismiss);
        }
    }

    public List<HistoryInfo> getHistory() {
        List<HistoryInfo> result = new ArrayList<HistoryInfo>();
        Set<Integer> teamIds = new HashSet<Integer>(scanPointTeams);
        List<RawTeamLevelPoints> resultsHistory = rawTeamLevelPointsStorage.getHistory();
        for (RawTeamLevelPoints rawTeamLevelPoints : resultsHistory) {
            Integer teamId = rawTeamLevelPoints.getTeam().getTeamId();
            TeamDismissedState teamDismissedState =
                    teamDismissedStorage.getTeamDismissedState(teamId);
            result.add(new HistoryInfo(teamId, rawTeamLevelPoints, teamDismissedState));
            teamIds.remove(teamId);
        }
        appendDismissedWithoutData(teamIds, result);
        return result;
    }

    private void appendDismissedWithoutData(Set<Integer> teamIds, List<HistoryInfo> result) {
        if (teamIds.isEmpty()) return;
        int addedCount = 0;
        for (Integer teamId : teamIds) {
            if (teamDismissedStorage.containsTeamId(teamId)) {
                TeamDismissedState teamDismissedState =
                        teamDismissedStorage.getTeamDismissedState(teamId);
                result.add(new HistoryInfo(teamId, null, teamDismissedState));
                addedCount++;
            }
        }
        if (addedCount > 0) {
            Collections.sort(result, new Comparator<HistoryInfo>() {
                @Override
                public int compare(HistoryInfo object1, HistoryInfo object2) {
                    return -1 * object1.compareTo(object2);
                }
            });
        }
    }

    private Set<Integer> getScanPointTeams() {
        return scanPointTeams;
    }

    private RawTeamLevelPointsStorage getRawTeamLevelPointsStorage() {
        return rawTeamLevelPointsStorage;
    }

    public ScanPoint getScanPoint() {
        return scanPoint;
    }

    private TeamDismissedStorage getTeamDismissedStorage() {
        return teamDismissedStorage;
    }

    public static void putRawTeamLevelPoints(RawTeamLevelPoints rawTeamLevelPoints) {
        if (instance.getScanPoint().getScanPointId() == rawTeamLevelPoints.getScanPointId()) {
            instance.getRawTeamLevelPointsStorage().put(rawTeamLevelPoints);
            instance.getScanPointTeams().add(rawTeamLevelPoints.getTeamId());
        } else {
            String message =
                    "Fatal error." + "\n" + "Current HISTORY data storage scan point ["
                    + instance.getScanPoint() + "]" + "\n" + "Putting new team result to ["
                    + rawTeamLevelPoints.getScanPoint() + "]";
            throw new DataStorageException(message);
        }
    }

    public static void putRawTeamDismiss(RawTeamLevelDismiss rawTeamLevelDismiss) {
        if (instance.getScanPoint().getScanPointId() == rawTeamLevelDismiss.getScanPointId()) {
            instance.getTeamDismissedStorage().put(rawTeamLevelDismiss);
            instance.getScanPointTeams().add(rawTeamLevelDismiss.getTeamId());
        } else {
            String message =
                    "Fatal error." + "\n" + "Current data storage scan point ["
                    + instance.getScanPoint() + "]" + "\n" + "Putting new team dismiss to ["
                    + rawTeamLevelDismiss.getScanPoint() + "]";
            throw new DataStorageException(message);
        }
    }
}
