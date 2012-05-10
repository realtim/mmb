package ru.mmb.terminal.db;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import ru.mmb.terminal.model.Distance;
import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Participant;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.transport.model.MetaTable;
import ru.mmb.terminal.util.ExternalStorage;
import android.database.sqlite.SQLiteDatabase;

public class TerminalDB
{
	private static TerminalDB instance = null;

	private final SQLiteDatabase db;
	private final Withdraw withdraw;
	private final InputData inputData;
	private final Distances distances;
	private final Levels levels;
	private final Teams teams;
	private final MetaTables metaTables;
	private final Settings settings;

	private final IDGenerator idGenerator;

	public static TerminalDB getInstance()
	{
		if (instance == null)
		{
			instance = new TerminalDB();
		}
		return instance;
	}

	private TerminalDB()
	{
		db =
		    SQLiteDatabase.openDatabase(ExternalStorage.getDir() + "/mmb/db/terminal.db", null, SQLiteDatabase.OPEN_READWRITE);
		withdraw = new Withdraw(db);
		inputData = new InputData(db);
		distances = new Distances(db);
		levels = new Levels(db);
		teams = new Teams(db);
		idGenerator = new IDGenerator(db);
		metaTables = new MetaTables(db);
		settings = new Settings(db);
	}

	public List<Participant> getWithdrawnMembers(LevelPoint levelPoint, Level level, Team team)
	{
		return withdraw.getWithdrawnMembers(levelPoint, level, team);
	}

	public void saveWithdrawnMembers(LevelPoint levelPoint, Level level, Team team,
	        List<Participant> withdrawnMembers)
	{
		withdraw.saveWithdrawnMembers(levelPoint, level, team, withdrawnMembers);
	}

	public void saveInputData(LevelPoint levelPoint, Team team, Date checkDateTime,
	        String takenCheckpoints)
	{
		inputData.saveInputData(levelPoint, team, checkDateTime, takenCheckpoints);
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

	public Properties loadSettings()
	{
		return settings.loadSettings();
	}

	public void setSettingValue(String settingName, String settingValue)
	{
		settings.setSettingValue(settingName, settingValue);
	}
}
