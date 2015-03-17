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
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamDismiss;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.User;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.model.MetaTable;

public class DatacollectorDB
{
	private static DatacollectorDB instance = null;

	private SQLiteDatabase db;

	private Distances distances;
	private ScanPoints scanPoints;
	private LevelPoints levelPoints;
	private LevelPointDiscounts levelPointDiscounts;
	private Teams teams;
	private MetaTables metaTables;
	private Users users;
	private TeamResults teamResults;
	private TeamDismissed teamDismissed;
    private RawLoggerData rawLoggerData;

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
			distances = new Distances(db);
			scanPoints = new ScanPoints(db);
			levelPoints = new LevelPoints(db);
			levelPointDiscounts = new LevelPointDiscounts(db);
			teams = new Teams(db);
			idGenerator = new IDGenerator(db);
			metaTables = new MetaTables(db);
			users = new Users(db);
			teamResults = new TeamResults(db);
			teamDismissed = new TeamDismissed(db);
            rawLoggerData = new RawLoggerData(db);
		}
		catch (SQLiteException e)
		{
			if (db != null)
			{
				// Log.d("DatacollectorDB", "db open FAILURE");
				db.close();
				// Log.d("DatacollectorDB", "db closed");
				db = null;
			}
		}
	}

	private void performTestQuery()
	{
		if (db == null) return;

		Distances.performTestQuery(db);
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
			// Log.d("DatacollectorDB", "close connection OK");
			db = null;
		}
	}

	public List<Participant> getDismissedMembers(LevelPoint levelPoint, Team team)
	{
		return teamDismissed.getDismissedMembers(levelPoint, team);
	}

	public void saveDismissedMembers(LevelPoint levelPoint, Team team,
	        List<Participant> dismissedMembers, Date recordDateTime)
	{
		teamDismissed.saveDismissedMembers(levelPoint, team, dismissedMembers, recordDateTime);
	}

	public void saveTeamResult(LevelPoint levelPoint, Team team, Date checkDateTime,
	        String takenCheckpoints, Date recordDateTime)
	{
		teamResults.saveTeamResult(levelPoint, team, checkDateTime, takenCheckpoints, recordDateTime);
	}

	public SQLiteDatabase getDb()
	{
		return db;
	}

	public List<Distance> loadDistances(int raidId)
	{
		return distances.loadDistances(raidId);
	}

	public List<Team> loadTeams()
	{
		return teams.loadTeams();
	}

	public int getNextId()
	{
		return idGenerator.getNextId();
	}

	public TeamResultRecord getExistingTeamResultRecord(LevelPoint levelPoint, Team team)
	{
		return teamResults.getExistingTeamResultRecord(levelPoint, team);
	}

	public List<MetaTable> loadMetaTables()
	{
		return metaTables.loadMetaTables();
	}

	public List<TeamResult> loadTeamResults(LevelPoint levelPoint)
	{
		return teamResults.loadTeamResults(levelPoint);
	}

	public List<User> loadUsers()
	{
		return users.loadUsers();
	}

	public List<TeamDismiss> loadDismissedMembers(LevelPoint levelPoint)
	{
		return teamDismissed.loadDismissedMembers(levelPoint);
	}

	public void appendLevelPointTeams(LevelPoint levelPoint, Set<Integer> teams)
	{
		teamResults.appendLevelPointTeams(levelPoint, teams);
		teamDismissed.appendLevelPointTeams(levelPoint, teams);
	}

	public List<ScanPoint> loadScanPoints(int raidId)
	{
		return scanPoints.loadScanPoints(raidId);
	}

	public List<LevelPoint> loadLevelPoints(int raidId)
	{
		return levelPoints.loadLevelPoints(raidId);
	}

	public List<LevelPointDiscount> loadLevelPointDiscounts(int raidId)
	{
		return levelPointDiscounts.loadLevelPointDiscounts(raidId);
	}

	public List<TeamResult> loadTeamResults(Team team)
	{
		return teamResults.loadTeamResults(team);
	}

    public ru.mmb.datacollector.model.RawLoggerData getExistingRecord(int loggerId, int scanpointId, int teamId) {
        return rawLoggerData.getExistingRecord(loggerId, scanpointId, teamId);
    }

    public void updateExistingRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        rawLoggerData.updateExistingRecord(loggerId, scanpointId, teamId, recordDateTime);
    }

    public void insertNewRecord(int loggerId, int scanpointId, int teamId, Date recordDateTime) {
        rawLoggerData.insertNewRecord(loggerId, scanpointId, teamId, recordDateTime);
    }
}
