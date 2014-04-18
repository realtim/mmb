package ru.mmb.terminal.db;

import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamDismiss;
import ru.mmb.terminal.model.TeamResult;
import ru.mmb.terminal.model.User;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.model.MetaTable;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class TerminalDB
{
	private static TerminalDB instance = null;

	private SQLiteDatabase db;

	private Distances distances;
	private ScanPoints scanPoints;
	private LevelPoints levelPoints;
	private Teams teams;
	private MetaTables metaTables;
	private Users users;
	private TeamResults teamResults;
	private TeamDismissed teamDismissed;

	private IDGenerator idGenerator;

	public static TerminalDB getRawInstance()
	{
		if (instance == null)
		{
			instance = new TerminalDB();
			instance.tryConnectToDB();
		}
		return instance;
	}

	public static TerminalDB getConnectedInstance()
	{
		TerminalDB result = getRawInstance();
		if (!result.isConnected()) return null;
		return result;
	}

	private TerminalDB()
	{
	}

	public void tryConnectToDB()
	{
		try
		{
			db =
			    SQLiteDatabase.openDatabase(Settings.getInstance().getPathToTerminalDB(), null, SQLiteDatabase.OPEN_READWRITE);
			// Log.d("TerminalDB", "db open " + Settings.getInstance().getPathToTerminalDB());
			performTestQuery();
			// Log.d("TerminalDB", "db open SUCCESS");
			distances = new Distances(db);
			scanPoints = new ScanPoints(db);
			levelPoints = new LevelPoints(db);
			teams = new Teams(db);
			idGenerator = new IDGenerator(db);
			metaTables = new MetaTables(db);
			users = new Users(db);
			teamResults = new TeamResults(db);
			teamDismissed = new TeamDismissed(db);
		}
		catch (SQLiteException e)
		{
			if (db != null)
			{
				// Log.d("TerminalDB", "db open FAILURE");
				db.close();
				// Log.d("TerminalDB", "db closed");
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
			// Log.d("TerminalDB", "close connection OK");
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
}
