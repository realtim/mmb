package ru.mmb.datacollector.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.LevelPointDiscount;
import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.User;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.model.MetaTable;

public class DatacollectorDB
{
	private static DatacollectorDB instance = null;

	private SQLiteDatabase db;

	private DistancesDB distancesDB;
	private ScanPointsDB scanPointsDB;
	private LevelPointsDB levelPointsDB;
	private LevelPointDiscountsDB levelPointDiscountsDB;
	private TeamsDB teamsDB;
	private MetaTablesDB metaTablesDB;
	private UsersDB usersDB;
	private TeamResultsDB teamResultsDB;
    private RawLoggerDataDB rawLoggerDataDB;
    private RawTeamLevelPointsDB rawTeamLevelPointsDB;
    private RawTeamLevelDismissDB rawTeamLevelDismissDB;

	private IDGenerator idGenerator;

	public static DatacollectorDB getRawInstance()
	{
		if (instance == null)
		{
			instance = new DatacollectorDB();
			instance.tryConnectToDB();
		}
		return instance;
	}

	public static DatacollectorDB getConnectedInstance()
	{
		DatacollectorDB result = getRawInstance();
		if (!result.isConnected()) return null;
		return result;
	}

	private DatacollectorDB()
	{
	}

	public void tryConnectToDB()
	{
		try
		{
			db =
			    SQLiteDatabase.openDatabase(Settings.getInstance().getPathToDB(), null, SQLiteDatabase.OPEN_READWRITE);
			// Log.d("DatacollectorDB", "db open " + Settings.getInstance().getPathToDB());
			performTestQuery();
			// Log.d("DatacollectorDB", "db open SUCCESS");
			distancesDB = new DistancesDB(db);
			scanPointsDB = new ScanPointsDB(db);
			levelPointsDB = new LevelPointsDB(db);
			levelPointDiscountsDB = new LevelPointDiscountsDB(db);
			teamsDB = new TeamsDB(db);
			idGenerator = new IDGenerator(db);
			metaTablesDB = new MetaTablesDB(db);
			usersDB = new UsersDB(db);
            rawLoggerDataDB = new RawLoggerDataDB(db);
            rawTeamLevelPointsDB = new RawTeamLevelPointsDB(db);
            rawTeamLevelDismissDB = new RawTeamLevelDismissDB(db);
            teamResultsDB = new TeamResultsDB(db);
		}
		catch (SQLiteException e)
		{
			if (db != null)
			{
				db.close();
				db = null;
			}
		}
	}

	private void performTestQuery()
	{
		if (db == null) return;

		DistancesDB.performTestQuery(db);
	}

	public boolean isConnected()
	{
		return db != null;
	}

	public void closeConnection()
	{
		if (isConnected())
		{
			db.close();
			db = null;
		}
	}

    public SQLiteDatabase getDb()
    {
        return db;
    }

	public List<Distance> loadDistances(int raidId)
	{
		return distancesDB.loadDistances(raidId);
	}

	public List<Team> loadTeams()
	{
		return teamsDB.loadTeams();
	}

	public int getNextId()
	{
		return idGenerator.getNextId();
	}

    public List<MetaTable> loadMetaTables()
    {
        return metaTablesDB.loadMetaTables();
    }

    public List<User> loadUsers()
    {
        return usersDB.loadUsers();
    }

    public List<ScanPoint> loadScanPoints(int raidId)
    {
        return scanPointsDB.loadScanPoints(raidId);
    }

    public List<LevelPoint> loadLevelPoints(int raidId)
    {
        return levelPointsDB.loadLevelPoints(raidId);
    }

    public List<LevelPointDiscount> loadLevelPointDiscounts(int raidId)
    {
        return levelPointDiscountsDB.loadLevelPointDiscounts(raidId);
    }

	public RawTeamLevelPointsRecord getExistingTeamResultRecord(ScanPoint scanPoint, Team team)
	{
		return rawTeamLevelPointsDB.getExistingTeamResultRecord(scanPoint, team);
	}

    public List<RawTeamLevelPoints> loadRawTeamLevelPoints(ScanPoint scanPoint) {
        return rawTeamLevelPointsDB.loadRawTeamLevelPoints(scanPoint);
    }

    public void saveRawTeamLevelPoints(ScanPoint scanPoint, Team team, String takenCheckpoints, Date recordDateTime)
    {
        rawTeamLevelPointsDB.saveRawTeamLevelPoints(scanPoint, team, takenCheckpoints, recordDateTime);
    }

	public List<RawTeamLevelDismiss> loadDismissedMembers(ScanPoint scanPoint)
	{
		return rawTeamLevelDismissDB.loadDismissedMembers(scanPoint);
	}

    public List<Participant> getDismissedMembers(ScanPoint scanPoint, Team team)
    {
        return rawTeamLevelDismissDB.getDismissedMembers(scanPoint, team);
    }

    public void saveDismissedMembers(ScanPoint scanPoint, Team team,
                                     List<Participant> dismissedMembers, Date recordDateTime)
    {
        rawTeamLevelDismissDB.saveDismissedMembers(scanPoint, team, dismissedMembers, recordDateTime);
    }

	public void appendScanPointTeams(ScanPoint scanPoint, Set<Integer> teams)
	{
		rawTeamLevelPointsDB.appendScanPointTeams(scanPoint, teams);
        rawTeamLevelDismissDB.appendScanPointTeams(scanPoint, teams);
	}

	public List<TeamResult> loadTeamResults(Team team)
	{
		return teamResultsDB.loadTeamResults(team);
	}

    public RawLoggerData getExistingLoggerRecord(int loggerId, int scanpointId, int teamId) {
        return rawLoggerDataDB.getExistingRecord(loggerId, scanpointId, teamId);
    }

    public void updateExistingLoggerRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        rawLoggerDataDB.updateExistingRecord(loggerId, scanpointId, teamId, recordDateTime);
    }

    public void insertNewLoggerRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        rawLoggerDataDB.insertNewRecord(loggerId, scanpointId, teamId, recordDateTime);
    }
}
