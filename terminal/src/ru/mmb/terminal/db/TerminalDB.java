package ru.mmb.terminal.db;

import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamLevelDismiss;
import ru.mmb.terminal.model.TeamLevelPoint;
import ru.mmb.terminal.model.User;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.model.MetaTable;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class TerminalDB
{
	private static TerminalDB instance = null;

	private SQLiteDatabase db;
	private Withdraw withdraw;
	private InputData inputData;
	private Distances distances;
	private Levels levels;
	private Teams teams;
	private MetaTables metaTables;
	private Users users;

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
			performTestQuery();
			// Log.d("TerminalDB", "db open SUCCESS");
			withdraw = new Withdraw(db);
			inputData = new InputData(db);
			distances = new Distances(db);
			levels = new Levels(db);
			teams = new Teams(db);
			idGenerator = new IDGenerator(db);
			metaTables = new MetaTables(db);
			users = new Users(db);
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

	public List<Participant> getWithdrawnMembers(LevelPoint levelPoint, Level level, Team team)
	{
		return withdraw.getWithdrawnMembers(levelPoint, level, team);
	}

	public void saveWithdrawnMembers(LevelPoint levelPoint, Level level, Team team,
	        List<Participant> withdrawnMembers, Date recordDateTime)
	{
		withdraw.saveWithdrawnMembers(levelPoint, level, team, withdrawnMembers, recordDateTime);
	}

	public void saveInputData(LevelPoint levelPoint, Team team, Date checkDateTime,
	        String takenCheckpoints, Date recordDateTime)
	{
		inputData.saveInputData(levelPoint, team, checkDateTime, takenCheckpoints, recordDateTime);
	}

	public SQLiteDatabase getDb()
	{
		return db;
	}

	public List<Distance> loadDistances(int raidId)
	{
		return distances.loadDistances(raidId);
	}

	public List<Level> loadLevels(int distanceId)
	{
		return levels.loadLevels(distanceId);
	}

	public List<Team> loadTeams()
	{
		return teams.loadTeams();
	}

	public int getNextId()
	{
		return idGenerator.getNextId();
	}

	public InputDataRecord getExistingTeamLevelPointRecord(LevelPoint levelPoint, Level level,
	        Team team)
	{
		return inputData.getExistingTeamLevelPointRecord(levelPoint, level, team);
	}

	public List<MetaTable> loadMetaTables()
	{
		return metaTables.loadMetaTables();
	}

	public List<TeamLevelPoint> loadTeamLevelPoints(LevelPoint levelPoint)
	{
		return inputData.loadTeamLevelPoints(levelPoint);
	}

	public List<User> loadUsers()
	{
		return users.loadUsers();
	}

	public List<TeamLevelDismiss> loadDismissedMembers(LevelPoint levelPoint)
	{
		return withdraw.loadDismissedMembers(levelPoint);
	}

	public void appendLevelPointTeams(LevelPoint levelPoint, Set<Integer> teams)
	{
		inputData.appendLevelPointTeams(levelPoint, teams);
		withdraw.appendLevelPointTeams(levelPoint, teams);
	}
}
