package ru.mmb.datacollector.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.util.DateFormat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TeamResults
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

	public TeamResults(SQLiteDatabase db)
	{
		this.db = db;
	}

	public TeamResultRecord getExistingTeamResultRecord(LevelPoint levelPoint, Team team)
	{
		TeamResultRecord result = null;
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

		List<TeamResultRecord> records = new ArrayList<TeamResultRecord>();
		while (!resultCursor.isAfterLast())
		{
			String recordDateTime = resultCursor.getString(0);
			String checkDateTime = resultCursor.getString(1);
			String takenCheckpoints = replaceNullWithEmptyString(resultCursor.getString(2));
			records.add(new TeamResultRecord(recordDateTime, checkDateTime, takenCheckpoints, levelPoint));
			resultCursor.moveToNext();
		}
		resultCursor.close();

		Collections.sort(records);
		if (records.size() > 0) result = records.get(records.size() - 1);

		return result;
	}

	private String replaceNullWithEmptyString(String takenCheckpoints)
	{
		if ("NULL".equals(takenCheckpoints))
		{
			return "";
		}
		else
		{
			return takenCheckpoints;
		}
	}

	public void saveTeamResult(LevelPoint levelPoint, Team team, Date checkDateTime,
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

	public List<TeamResult> loadTeamResults(LevelPoint levelPoint)
	{
		List<TeamResult> result = new ArrayList<TeamResult>();
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
			String takenCheckpointNames = replaceNullWithEmptyString(resultCursor.getString(5));

			TeamResult teamResult =
			    new TeamResult(teamId, userId, deviceId, levelPoint.getScanPoint().getScanPointId(), takenCheckpointNames, checkDateTime, recordDateTime);
			// init reference fields
			teamResult.setScanPoint(levelPoint.getScanPoint());
			teamResult.setTeam(TeamsRegistry.getInstance().getTeamById(teamId));
			teamResult.initTakenCheckpoints();

			result.add(teamResult);
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

	public List<TeamResult> loadTeamResults(Team team)
	{
		List<TeamResult> result = new ArrayList<TeamResult>();
		String sql =
		    "select " + TEAMLEVELPOINT_DATE + ", " + USER_ID + ", " + DEVICE_ID + ", " + TEAM_ID
		            + ", " + LEVELPOINT_ID + ", " + TEAMLEVELPOINT_DATETIME + ", "
		            + TEAMLEVELPOINT_POINTS + " from " + TABLE_TEAM_LEVEL_POINTS + " where "
		            + TEAM_ID + " = " + team.getTeamId() + " order by " + LEVELPOINT_ID + ", "
		            + TEAMLEVELPOINT_DATE;
		Cursor resultCursor = db.rawQuery(sql, null);

		resultCursor.moveToFirst();
		while (!resultCursor.isAfterLast())
		{
			Date recordDateTime = DateFormat.parse(resultCursor.getString(0));
			Integer userId = resultCursor.getInt(1);
			Integer deviceId = resultCursor.getInt(2);
			Integer teamId = resultCursor.getInt(3);
			int levelPointId = resultCursor.getInt(4);
			Date checkDateTime = DateFormat.parse(resultCursor.getString(5));
			String takenCheckpointNames = replaceNullWithEmptyString(resultCursor.getString(6));

			ScanPoint scanPoint =
			    ScanPointsRegistry.getInstance().getScanPointByLevelPointId(levelPointId);

			TeamResult teamResult =
			    new TeamResult(teamId, userId, deviceId, scanPoint.getScanPointId(), takenCheckpointNames, checkDateTime, recordDateTime);
			// init reference fields
			teamResult.setScanPoint(scanPoint);
			teamResult.setTeam(team);
			teamResult.initTakenCheckpoints();

			result.add(teamResult);
			resultCursor.moveToNext();
		}
		resultCursor.close();

		return result;
	}
}
