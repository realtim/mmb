package ru.mmb.terminal.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.TeamLevelPoint;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import ru.mmb.terminal.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class InputData
{
	private static final String TABLE_TEAM_LEVEL_POINTS = "TeamLevelPoints";

	private static final String TEAMLEVELPOINT_DATE = "teamlevelpoint_date";
	private static final String DEVICE_ID = "device_id";
	private static final String USER_ID = "user_id";
	private static final String LEVELPOINT_ID = "levelpoint_id";
	private static final String TEAM_ID = "team_id";
	private static final String TEAMLEVELPOINT_DATETIME = "teamlevelpoint_datetime";
	private static final String TEAMLEVELPOINT_POINTS = "teamlevelpoint_points";
	private static final String TEAMLEVELPOINT_COMMENT = "teamlevelpoint_comment";

	private final SQLiteDatabase db;

	public InputData(SQLiteDatabase db)
	{
		this.db = db;
	}

	public InputDataRecord getExistingTeamLevelPointRecord(LevelPoint levelPoint, Level level,
	        Team team)
	{
		InputDataRecord result = null;
		String sql =
		    "select " + TEAMLEVELPOINT_DATE + ", " + TEAMLEVELPOINT_DATETIME + ", "
		            + TEAMLEVELPOINT_POINTS + " from " + TABLE_TEAM_LEVEL_POINTS + " where "
		            + LEVELPOINT_ID + " = " + levelPoint.getLevelPointId() + " and " + TEAM_ID
		            + " = " + team.getTeamId();
		Cursor resultCursor = db.rawQuery(sql, null);

		if (resultCursor.moveToFirst() == false)
		{
			resultCursor.close();
			return null;
		}

		List<InputDataRecord> records = new ArrayList<InputDataRecord>();
		while (!resultCursor.isAfterLast())
		{
			String recordDateTime = resultCursor.getString(0);
			String checkDateTime = resultCursor.getString(1);
			String takenCheckpoints = resultCursor.getString(2);
			records.add(new InputDataRecord(recordDateTime, checkDateTime, takenCheckpoints, level));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		Collections.sort(records);
		if (records.size() > 0) result = records.get(records.size() - 1);

		return result;
	}

	public void saveInputData(LevelPoint levelPoint, Team team, Date checkDateTime,
	        String takenCheckpoints, Date recordDateTime)
	{
		db.beginTransaction();
		try
		{
			if (isThisUserRecordExists(levelPoint, team))
			{
				updateExistingRecord(levelPoint, team, checkDateTime, takenCheckpoints, recordDateTime);
			}
			else
			{
				insertNewRecord(levelPoint, team, checkDateTime, takenCheckpoints, recordDateTime);
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	private boolean isThisUserRecordExists(LevelPoint levelPoint, Team team)
	{
		String selectSql =
		    "select count(*) from " + TABLE_TEAM_LEVEL_POINTS + " where " + USER_ID + " = "
		            + Settings.getInstance().getUserId() + " and " + LEVELPOINT_ID + " = "
		            + levelPoint.getLevelPointId() + " and " + TEAM_ID + " = " + team.getTeamId();
		Cursor resultCursor = db.rawQuery(selectSql, null);
		resultCursor.moveToFirst();
		int recordCount = resultCursor.getInt(0);
		resultCursor.close();
		return recordCount > 0;
	}

	private void updateExistingRecord(LevelPoint levelPoint, Team team, Date checkDateTime,
	        String takenCheckpoints, Date recordDateTime)
	{
		String updateSql =
		    "update " + TABLE_TEAM_LEVEL_POINTS + " set " + DEVICE_ID + " = "
		            + Settings.getInstance().getDeviceId() + ", " + TEAMLEVELPOINT_DATE + " = '"
		            + DateFormat.format(recordDateTime) + "', " + TEAMLEVELPOINT_DATETIME + " = '"
		            + DateFormat.format(checkDateTime) + "', " + TEAMLEVELPOINT_POINTS + " = '"
		            + takenCheckpoints + "', " + TEAMLEVELPOINT_COMMENT + " = '' where " + USER_ID
		            + " = " + Settings.getInstance().getUserId() + " and " + LEVELPOINT_ID + " = "
		            + levelPoint.getLevelPointId() + " and " + TEAM_ID + " = " + team.getTeamId();
		db.execSQL(updateSql);
	}

	private void insertNewRecord(LevelPoint levelPoint, Team team, Date checkDateTime,
	        String takenCheckpoints, Date recordDateTime)
	{
		String insertSql =
		    "insert into " + TABLE_TEAM_LEVEL_POINTS + " (" + USER_ID + ", " + DEVICE_ID + ", "
		            + LEVELPOINT_ID + ", " + TEAM_ID + ", " + TEAMLEVELPOINT_DATE + ", "
		            + TEAMLEVELPOINT_DATETIME + ", " + TEAMLEVELPOINT_POINTS + ", "
		            + TEAMLEVELPOINT_COMMENT + ") values (" + Settings.getInstance().getUserId()
		            + ", " + Settings.getInstance().getDeviceId() + ", "
		            + levelPoint.getLevelPointId() + ", " + team.getTeamId() + ", '"
		            + DateFormat.format(recordDateTime) + "', '" + DateFormat.format(checkDateTime)
		            + "', '" + takenCheckpoints + "', '')";
		db.execSQL(insertSql);
	}

	public List<TeamLevelPoint> loadTeamLevelPoints(LevelPoint levelPoint)
	{
		List<TeamLevelPoint> result = new ArrayList<TeamLevelPoint>();
		String sql =
		    "select " + TEAMLEVELPOINT_DATE + ", " + USER_ID + ", " + DEVICE_ID + ", " + TEAM_ID
		            + ", " + TEAMLEVELPOINT_DATETIME + ", " + TEAMLEVELPOINT_POINTS + " from "
		            + TABLE_TEAM_LEVEL_POINTS + " where " + LEVELPOINT_ID + " = "
		            + levelPoint.getLevelPointId();
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			Date recordDateTime = DateFormat.parse(resultCursor.getString(0));
			Integer userId = resultCursor.getInt(1);
			Integer deviceId = resultCursor.getInt(2);
			Integer teamId = resultCursor.getInt(3);
			Date checkDateTime = DateFormat.parse(resultCursor.getString(4));
			String takenCheckpointNames = resultCursor.getString(5);

			TeamLevelPoint teamLevelPoint =
			    new TeamLevelPoint(teamId, userId, deviceId, levelPoint.getLevelPointId(), takenCheckpointNames, checkDateTime, recordDateTime);
			// init reference fields
			teamLevelPoint.setLevelPoint(levelPoint);
			teamLevelPoint.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
			teamLevelPoint.initTakenCheckpoints();

			result.add(teamLevelPoint);
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}

	public void appendLevelPointTeams(LevelPoint levelPoint, Set<Integer> teams)
	{
		String sql =
		    "select distinct " + TEAM_ID + " from " + TABLE_TEAM_LEVEL_POINTS + " where "
		            + LEVELPOINT_ID + " = " + levelPoint.getLevelPointId();
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			Integer teamId = resultCursor.getInt(0);
			teams.add(teamId);
			resultCursor.moveToNext();
		}
		resultCursor.close();
	}
}
